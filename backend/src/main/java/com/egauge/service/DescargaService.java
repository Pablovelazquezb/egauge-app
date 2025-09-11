package com.egauge.service;

import com.egauge.entity.Cliente;
import com.egauge.repository.ClienteRepository;
import com.egauge.util.TarifaClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.StringReader;
import java.io.BufferedReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DescargaService {
    
    @Autowired
    private ClienteRepository clienteRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    /**
     * Descarga datos para un cliente en un rango de fechas
     */
    public DescargaResultado descargarDatos(Long clienteId, LocalDateTime inicio, LocalDateTime fin) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(clienteId);
        if (!clienteOpt.isPresent()) {
            throw new RuntimeException("Cliente no encontrado con ID: " + clienteId);
        }
        
        Cliente cliente = clienteOpt.get();
        String nombreTabla = cliente.getTablaNombre();
        
        List<Long> timestamps = generarTimestamps(inicio, fin, 3600); // Cada hora
        
        DescargaResultado resultado = new DescargaResultado();
        resultado.setCliente(cliente.getNombreCliente());
        resultado.setTotalPuntos(timestamps.size());
        resultado.setInicio(inicio);
        resultado.setFin(fin);
        
        // Descargar primer timestamp para detectar estructura de columnas
        Map<String, Object> ejemploData = null;
        if (!timestamps.isEmpty()) {
            ejemploData = descargarTimestamp(cliente, timestamps.get(0));
        }
        
        if (ejemploData != null) {
            @SuppressWarnings("unchecked")
            Map<String, Double> sensoresEjemplo = (Map<String, Double>) ejemploData.get("sensores_map");
            if (sensoresEjemplo != null) {
                crearTablaCliente(nombreTabla, sensoresEjemplo);
            }
        }
        
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
        
        // Descargar en paralelo
        for (Long timestamp : timestamps) {
            CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(() -> 
                descargarTimestamp(cliente, timestamp), executor);
            futures.add(future);
        }
        
        // Esperar todos los resultados e insertar
        List<Map<String, Object>> datosDescargados = new ArrayList<>();
        int errores = 0;
        
        for (CompletableFuture<Map<String, Object>> future : futures) {
            try {
                Map<String, Object> datos = future.get();
                if (datos != null) {
                    datosDescargados.add(datos);
                } else {
                    errores++;
                }
            } catch (Exception e) {
                errores++;
            }
        }
        
        // Insertar datos en la tabla del cliente
        int filasInsertadas = insertarDatosEnTabla(nombreTabla, datosDescargados);
        
        resultado.setFilasInsertadas(filasInsertadas);
        resultado.setErrores(errores);
        resultado.setExito(filasInsertadas > 0);
        
        return resultado;
    }
    
    /**
     * Crea tabla individual para el cliente con columnas dinámicas
     */
    private void crearTablaCliente(String nombreTabla, Map<String, Double> sensoresEjemplo) {
        try {
            // Verificar si la tabla ya existe
            String checkTableSql = """
                SELECT EXISTS (
                    SELECT FROM information_schema.tables 
                    WHERE table_schema = 'public' AND table_name = ?
                )
                """;
            
            Boolean tablaExiste = jdbcTemplate.queryForObject(checkTableSql, Boolean.class, nombreTabla);
            
            if (!tablaExiste) {
                // Crear columnas básicas
                StringBuilder createTableSql = new StringBuilder();
                createTableSql.append(String.format("""
                    CREATE TABLE "%s" (
                        id SERIAL PRIMARY KEY,
                        timestamp TIMESTAMP,
                        tarifa VARCHAR(20),
                    """, nombreTabla));
                
                // Agregar columnas para cada sensor
                for (String sensorName : sensoresEjemplo.keySet()) {
                    String columnName = limpiarNombreColumna(sensorName);
                    createTableSql.append(String.format("\"%s\" FLOAT,\n", columnName));
                }
                
                createTableSql.append("created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n");
                createTableSql.append(")");
                
                jdbcTemplate.execute(createTableSql.toString());
                
                // Crear índices
                String createIndexSql = String.format("""
                    CREATE UNIQUE INDEX IF NOT EXISTS "idx_%s_timestamp" ON "%s"(timestamp)
                    """, nombreTabla, nombreTabla);
                jdbcTemplate.execute(createIndexSql);
                
                String createTarifaIndexSql = String.format("""
                    CREATE INDEX IF NOT EXISTS "idx_%s_tarifa" ON "%s"(tarifa)
                    """, nombreTabla, nombreTabla);
                jdbcTemplate.execute(createTarifaIndexSql);
                
                System.out.println("✅ Tabla creada: " + nombreTabla + " con " + sensoresEjemplo.size() + " sensores");
            } else {
                // Si la tabla existe, verificar si necesitamos agregar nuevas columnas
                actualizarColumnasTabla(nombreTabla, sensoresEjemplo);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error creando tabla " + nombreTabla + ": " + e.getMessage());
            throw new RuntimeException("Error creando tabla para cliente");
        }
    }
    
    /**
     * Actualiza tabla existente agregando nuevas columnas si es necesario
     */
    private void actualizarColumnasTabla(String nombreTabla, Map<String, Double> sensores) {
        try {
            // Obtener columnas existentes
            String getColumnsSql = """
                SELECT column_name FROM information_schema.columns 
                WHERE table_name = ? AND table_schema = 'public'
                """;
            
            List<String> columnasExistentes = jdbcTemplate.queryForList(getColumnsSql, String.class, nombreTabla);
            
            // Agregar columnas faltantes
            for (String sensorName : sensores.keySet()) {
                String columnName = limpiarNombreColumna(sensorName);
                
                if (!columnasExistentes.contains(columnName)) {
                    String addColumnSql = String.format("""
                        ALTER TABLE "%s" ADD COLUMN "%s" FLOAT
                        """, nombreTabla, columnName);
                    
                    jdbcTemplate.execute(addColumnSql);
                    System.out.println("✅ Columna agregada: " + columnName + " a tabla " + nombreTabla);
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error actualizando columnas de tabla " + nombreTabla + ": " + e.getMessage());
        }
    }
    
    /**
 * Limpia nombre de sensor para usar como nombre de columna (versión simplificada)
 */
private String limpiarNombreColumna(String sensorName) {
    // Solo reemplazar espacios y caracteres problemáticos básicos
    return sensorName
        .trim()
        .replaceAll("\\s+", "_")           // Espacios a _
        .replaceAll("[\"'`]", "")          // Remover comillas
        .replaceAll("[\\(\\)]", "_")       // Paréntesis a _
        .replaceAll("_+", "_")             // Múltiples _ a uno solo
        .replaceAll("^_|_$", "");          // Quitar _ del inicio/final
}
    
    /**
     * Inserta datos en la tabla específica del cliente con columnas separadas
     */
    private int insertarDatosEnTabla(String nombreTabla, List<Map<String, Object>> datos) {
        if (datos.isEmpty()) return 0;
        
        int insertados = 0;
        
        for (Map<String, Object> registro : datos) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Double> sensores = (Map<String, Double>) registro.get("sensores_map");
                
                if (sensores == null || sensores.isEmpty()) {
                    continue;
                }
                
                // Construir consulta dinámica
                StringBuilder columnNames = new StringBuilder("timestamp, tarifa");
                StringBuilder placeholders = new StringBuilder("?, ?");
                List<Object> values = new ArrayList<>();
                
                // Agregar timestamp y tarifa
                values.add(registro.get("timestamp"));
                values.add(registro.get("tarifa"));
                
                // Agregar cada sensor como columna
                for (Map.Entry<String, Double> sensor : sensores.entrySet()) {
                    String columnName = limpiarNombreColumna(sensor.getKey());
                    columnNames.append(", \"").append(columnName).append("\"");
                    placeholders.append(", ?");
                    values.add(sensor.getValue());
                }
                
                // UPSERT query
                String upsertSql = String.format("""
                    INSERT INTO "%s" (%s)
                    VALUES (%s)
                    ON CONFLICT (timestamp) 
                    DO UPDATE SET %s
                    """, 
                    nombreTabla, 
                    columnNames.toString(),
                    placeholders.toString(),
                    construirUpdateClause(sensores)
                );
                
                int rowsAffected = jdbcTemplate.update(upsertSql, values.toArray());
                
                if (rowsAffected > 0) {
                    insertados++;
                }
                
            } catch (Exception e) {
                System.err.println("Error insertando registro: " + e.getMessage());
                // Continuar con el siguiente registro
            }
        }
        
        return insertados;
    }
    
    /**
     * Construye la cláusula UPDATE para el UPSERT
     */
    private String construirUpdateClause(Map<String, Double> sensores) {
        StringBuilder updateClause = new StringBuilder("tarifa = EXCLUDED.tarifa");
        
        for (String sensorName : sensores.keySet()) {
            String columnName = limpiarNombreColumna(sensorName);
            updateClause.append(", \"").append(columnName).append("\" = EXCLUDED.\"").append(columnName).append("\"");
        }
        
        return updateClause.toString();
    }
    
    /**
     * Descarga un timestamp específico de eGauge
     */
    private Map<String, Object> descargarTimestamp(Cliente cliente, Long timestamp) {
        try {
            String url = construirUrlEgauge(cliente.getHostname(), timestamp, 3600);
            String csvContent = restTemplate.getForObject(url, String.class);
            
            if (csvContent == null || csvContent.trim().isEmpty()) {
                return null;
            }
            
            return procesarCsvContent(csvContent);
            
        } catch (Exception e) {
            System.err.println("Error descargando timestamp " + timestamp + " para " + cliente.getHostname() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Construye URL de eGauge
     */
    private String construirUrlEgauge(String hostname, Long timestamp, int pasoSegundos) {
        return String.format("https://%s/cgi-bin/egauge-show?E&c&S&s=%d&n=1&f=%d&F=data.csv&C&Z=LST6",
                hostname, pasoSegundos, timestamp);
    }
    
    /**
     * Procesa contenido CSV y retorna Map con datos
     */
    private Map<String, Object> procesarCsvContent(String csvContent) {
        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            
            String headerLine = reader.readLine();
            if (headerLine == null) return null;
            
            String dataLine = reader.readLine();
            if (dataLine == null) return null;
            
            // Detectar separador
            char separador = headerLine.contains(",") ? ',' : ';';
            
            // Parsear headers
            String[] headers = headerLine.split(String.valueOf(separador));
            String[] values = dataLine.split(String.valueOf(separador));
            
            if (headers.length != values.length) return null;
            
            // Limpiar headers
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim().replaceAll("\"", "");
            }
            
            // Encontrar timestamp
            LocalDateTime timestamp = null;
            Map<String, Double> sensores = new HashMap<>();
            
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i];
                String value = values[i].trim().replaceAll("\"", "");
                
                // Primera columna o columna con nombre de fecha = timestamp
                if (i == 0 || header.toLowerCase().contains("fecha") || 
                    header.toLowerCase().contains("time") || header.toLowerCase().contains("timestamp")) {
                    timestamp = parseTimestamp(value);
                } else {
                    // Resto son sensores
                    try {
                        Double valorNumerico = Double.parseDouble(value);
                        sensores.put(header, valorNumerico);
                    } catch (NumberFormatException e) {
                        // Ignorar valores no numéricos
                    }
                }
            }
            
            if (timestamp == null) {
                return null;
            }
            
            // Clasificar tarifa
            String tarifa = TarifaClassifier.clasificarTarifa(timestamp);
            
            // Crear resultado
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("timestamp", timestamp);
            resultado.put("tarifa", tarifa);
            resultado.put("sensores_map", sensores); // Pasamos el Map directamente
            
            return resultado;
            
        } catch (Exception e) {
            System.err.println("Error procesando CSV: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parsea timestamp de diferentes formatos
     */
    private LocalDateTime parseTimestamp(String timestampStr) {
        List<DateTimeFormatter> formatters = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
        );
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(timestampStr, formatter);
            } catch (DateTimeParseException e) {
                // Intentar siguiente formato
            }
        }
        
        throw new IllegalArgumentException("No se pudo parsear timestamp: " + timestampStr);
    }
    
    /**
     * Genera lista de timestamps para un rango
     */
    private List<Long> generarTimestamps(LocalDateTime inicio, LocalDateTime fin, int pasoSegundos) {
        List<Long> timestamps = new ArrayList<>();
        LocalDateTime actual = inicio;
        
        while (!actual.isAfter(fin)) {
            timestamps.add(actual.atZone(java.time.ZoneId.systemDefault()).toEpochSecond());
            actual = actual.plusSeconds(pasoSegundos);
        }
        
        return timestamps;
    }
    
    /**
     * Clase para resultado de descarga
     */
    public static class DescargaResultado {
        private String cliente;
        private int totalPuntos;
        private int filasInsertadas;
        private int errores;
        private boolean exito;
        private LocalDateTime inicio;
        private LocalDateTime fin;
        
        // Getters y Setters
        public String getCliente() { return cliente; }
        public void setCliente(String cliente) { this.cliente = cliente; }
        
        public int getTotalPuntos() { return totalPuntos; }
        public void setTotalPuntos(int totalPuntos) { this.totalPuntos = totalPuntos; }
        
        public int getFilasInsertadas() { return filasInsertadas; }
        public void setFilasInsertadas(int filasInsertadas) { this.filasInsertadas = filasInsertadas; }
        
        public int getErrores() { return errores; }
        public void setErrores(int errores) { this.errores = errores; }
        
        public boolean isExito() { return exito; }
        public void setExito(boolean exito) { this.exito = exito; }
        
        public LocalDateTime getInicio() { return inicio; }
        public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }
        
        public LocalDateTime getFin() { return fin; }
        public void setFin(LocalDateTime fin) { this.fin = fin; }
    }
}

