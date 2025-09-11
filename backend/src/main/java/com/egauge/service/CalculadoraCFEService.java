package com.egauge.service;

import com.egauge.entity.Cliente;
import com.egauge.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CalculadoraCFEService {
    
    @Autowired
    private ClienteRepository clienteRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * Calcula costos CFE para un cliente en un período
     */
    public CalculoResultado calcularCostosCFE(CalculoRequest request) {
        // Validar cliente
        Optional<Cliente> clienteOpt = clienteRepository.findById(request.getClienteId());
        if (!clienteOpt.isPresent()) {
            throw new RuntimeException("Cliente no encontrado con ID: " + request.getClienteId());
        }
        
        Cliente cliente = clienteOpt.get();
        String nombreTabla = cliente.getTablaNombre();
        
        // Verificar que la tabla existe
        if (!tablaExiste(nombreTabla)) {
            throw new RuntimeException("No se encontraron datos para el cliente: " + cliente.getNombreCliente());
        }
        
        // Obtener columnas disponibles
        List<String> columnasDisponibles = obtenerColumnasNumericas(nombreTabla);
        
        if (columnasDisponibles.isEmpty()) {
            throw new RuntimeException("No se encontraron columnas de sensores en la tabla del cliente");
        }
        
        // Usar la primera columna disponible si no se especifica
        String columnaUsar = request.getColumnaSensor();
        if (columnaUsar == null || columnaUsar.trim().isEmpty()) {
            columnaUsar = columnasDisponibles.get(0);
        }
        
        // Validar que la columna existe
        if (!columnasDisponibles.contains(columnaUsar)) {
            throw new RuntimeException("La columna especificada no existe: " + columnaUsar);
        }
        
        // Obtener datos agrupados por tarifa
        Map<String, DatosTarifa> datosPorTarifa = obtenerDatosPorTarifa(
            nombreTabla, 
            columnaUsar, 
            request.getFechaInicio(), 
            request.getFechaFin()
        );
        
        // Calcular costos
        return calcularCostos(cliente, datosPorTarifa, request.getPrecios(), columnaUsar);
    }
    
    /**
     * Obtiene columnas numéricas disponibles en la tabla
     */
    public List<String> obtenerColumnasDisponibles(Long clienteId) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(clienteId);
        if (!clienteOpt.isPresent()) {
            throw new RuntimeException("Cliente no encontrado");
        }
        
        String nombreTabla = clienteOpt.get().getTablaNombre();
        return obtenerColumnasNumericas(nombreTabla);
    }
    
    /**
     * Verifica si una tabla existe
     */
    private boolean tablaExiste(String nombreTabla) {
        try {
            String sql = """
                SELECT EXISTS (
                    SELECT FROM information_schema.tables 
                    WHERE table_schema = 'public' AND table_name = ?
                )
                """;
            return jdbcTemplate.queryForObject(sql, Boolean.class, nombreTabla);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Obtiene columnas numéricas de una tabla
     */
    private List<String> obtenerColumnasNumericas(String nombreTabla) {
        try {
            String sql = """
                SELECT column_name FROM information_schema.columns 
                WHERE table_name = ? AND table_schema = 'public'
                AND data_type IN ('double precision', 'real', 'numeric', 'integer', 'bigint', 'smallint', 'float')
                AND column_name NOT IN ('id', 'created_at')
                ORDER BY ordinal_position
                """;
            return jdbcTemplate.queryForList(sql, String.class, nombreTabla);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Obtiene datos agrupados por tarifa desde la tabla del cliente
     */
    private Map<String, DatosTarifa> obtenerDatosPorTarifa(
            String nombreTabla, 
            String columnaSensor, 
            LocalDateTime fechaInicio, 
            LocalDateTime fechaFin) {
        
        String sql = String.format("""
            SELECT 
                tarifa,
                COUNT(*) as cantidad_registros,
                SUM("%s") as suma_kwh,
                AVG("%s") as promedio_kw,
                MAX("%s") as max_demanda
            FROM "%s"
            WHERE timestamp >= ? AND timestamp <= ?
            AND "%s" IS NOT NULL
            AND tarifa IS NOT NULL
            GROUP BY tarifa
            """, columnaSensor, columnaSensor, columnaSensor, nombreTabla, columnaSensor);
        
        Map<String, DatosTarifa> resultado = new HashMap<>();
        
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, fechaInicio, fechaFin);
            
            for (Map<String, Object> row : rows) {
                String tarifa = (String) row.get("tarifa");
                if (tarifa != null) {
                    DatosTarifa datos = new DatosTarifa();
                    datos.setTarifa(tarifa);
                    datos.setCantidadRegistros(((Number) row.get("cantidad_registros")).intValue());
                    datos.setSumaKwh(((Number) row.get("suma_kwh")).doubleValue());
                    datos.setPromedioKw(((Number) row.get("promedio_kw")).doubleValue());
                    datos.setMaxDemanda(((Number) row.get("max_demanda")).doubleValue());
                    
                    resultado.put(tarifa, datos);
                }
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo datos por tarifa: " + e.getMessage());
            throw new RuntimeException("Error procesando datos de consumo");
        }
        
        return resultado;
    }
    
    /**
     * Calcula costos CFE basado en los datos y precios
     */
    private CalculoResultado calcularCostos(
            Cliente cliente, 
            Map<String, DatosTarifa> datosPorTarifa, 
            PreciosCFE precios,
            String columnaSensor) {
        
        CalculoResultado resultado = new CalculoResultado();
        resultado.setClienteNombre(cliente.getNombreCliente());
        resultado.setColumnaSensor(columnaSensor);
        
        // Obtener datos por tarifa
        DatosTarifa datosBase = datosPorTarifa.getOrDefault("Base", new DatosTarifa());
        DatosTarifa datosIntermedio = datosPorTarifa.getOrDefault("Intermedio", new DatosTarifa());
        DatosTarifa datosPunta = datosPorTarifa.getOrDefault("Punta", new DatosTarifa());
        
        // Consumos kWh
        resultado.setKwhBase(redondear(datosBase.getSumaKwh()));
        resultado.setKwhIntermedio(redondear(datosIntermedio.getSumaKwh()));
        resultado.setKwhPunta(redondear(datosPunta.getSumaKwh()));
        
        // Demandas máximas
        resultado.setMaxBase(redondear(datosBase.getMaxDemanda()));
        resultado.setMaxIntermedio(redondear(datosIntermedio.getMaxDemanda()));
        resultado.setMaxPunta(redondear(datosPunta.getMaxDemanda()));
        
        // Calcular demanda facturable (mayor de las tres)
        double demandaFacturable = Math.max(
            Math.max(resultado.getMaxBase(), resultado.getMaxIntermedio()),
            resultado.getMaxPunta()
        );
        resultado.setDemandaFacturable(redondear(demandaFacturable));
        
        // Cálculo de distribución según fórmula CFE
        double consumoTotal = resultado.getKwhBase() + resultado.getKwhIntermedio() + resultado.getKwhPunta();
        double diasPeriodo = 30.0; // Asumir 30 días por defecto
        double fc = 0.57; // Factor de carga CFE
        double formulaDistribucion = consumoTotal / (24 * diasPeriodo * fc);
        double demandaDistribucion = Math.min(resultado.getMaxPunta(), formulaDistribucion);
        resultado.setDemandaDistribucion(redondear(demandaDistribucion));
        
        // Costos por energía
        resultado.setCostoBase(redondear(resultado.getKwhBase() * precios.getPrecioBase()));
        resultado.setCostoIntermedio(redondear(resultado.getKwhIntermedio() * precios.getPrecioIntermedio()));
        resultado.setCostoPunta(redondear(resultado.getKwhPunta() * precios.getPrecioPunta()));
        
        // Costo de capacidad (usando demanda de capacidad calculada)
        resultado.setCostoCapacidad(redondear(resultado.getDemandaFacturable() * precios.getPrecioCapacidad()));
        
        // Costo de distribución (usando demanda de distribución calculada)
        resultado.setCostoDistribucion(redondear(resultado.getDemandaDistribucion() * precios.getPrecioDistribucion()));
        
        // Total energía
        double totalEnergia = resultado.getCostoBase() + resultado.getCostoIntermedio() + 
                             resultado.getCostoPunta() + resultado.getCostoCapacidad() + 
                             resultado.getCostoDistribucion();
        resultado.setEnergia(redondear(totalEnergia));
        
        // Cargo fijo
        resultado.setCargoFijo(redondear(precios.getCargoFijo()));
        
        // Subtotal
        double subtotal = totalEnergia + resultado.getCargoFijo();
        resultado.setSubtotal(redondear(subtotal));
        
        // DAP (Servicio de Alumbrado Público)
        double dap = 0.0;
        if (precios.isIncluirDap()) {
            dap = subtotal * (precios.getPorcentajeDap() / 100.0);
        }
        resultado.setDap(redondear(dap));
        
        // Subtotal con DAP
        double subtotalConDap = subtotal + dap;
        resultado.setSubtotalConDap(redondear(subtotalConDap));
        
        // IVA
        double iva = subtotalConDap * 0.16;
        resultado.setIva(redondear(iva));
        
        // Total final
        double totalFinal = subtotalConDap + iva;
        resultado.setTotal(redondear(totalFinal));
        
        return resultado;
    }
    
    /**
     * Redondea a 2 decimales
     */
    private double redondear(double valor) {
        return BigDecimal.valueOf(valor)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
    
    // ========== CLASES INTERNAS ==========
    
    public static class CalculoRequest {
        private Long clienteId;
        private LocalDateTime fechaInicio;
        private LocalDateTime fechaFin;
        private String columnaSensor;
        private PreciosCFE precios;
        
        // Getters y Setters
        public Long getClienteId() { return clienteId; }
        public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
        
        public LocalDateTime getFechaInicio() { return fechaInicio; }
        public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
        
        public LocalDateTime getFechaFin() { return fechaFin; }
        public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }
        
        public String getColumnaSensor() { return columnaSensor; }
        public void setColumnaSensor(String columnaSensor) { this.columnaSensor = columnaSensor; }
        
        public PreciosCFE getPrecios() { return precios; }
        public void setPrecios(PreciosCFE precios) { this.precios = precios; }
    }
    
    public static class PreciosCFE {
        private double precioBase = 1.20;
        private double precioIntermedio = 1.98;
        private double precioPunta = 2.32;
        private double precioCapacidad = 367.15;
        private double precioDistribucion = 100.00;
        private double cargoFijo = 563.57;
        private boolean incluirDap = false;
        private double porcentajeDap = 2.0;
        
        // Getters y Setters
        public double getPrecioBase() { return precioBase; }
        public void setPrecioBase(double precioBase) { this.precioBase = precioBase; }
        
        public double getPrecioIntermedio() { return precioIntermedio; }
        public void setPrecioIntermedio(double precioIntermedio) { this.precioIntermedio = precioIntermedio; }
        
        public double getPrecioPunta() { return precioPunta; }
        public void setPrecioPunta(double precioPunta) { this.precioPunta = precioPunta; }
        
        public double getPrecioCapacidad() { return precioCapacidad; }
        public void setPrecioCapacidad(double precioCapacidad) { this.precioCapacidad = precioCapacidad; }
        
        public double getPrecioDistribucion() { return precioDistribucion; }
        public void setPrecioDistribucion(double precioDistribucion) { this.precioDistribucion = precioDistribucion; }
        
        public double getCargoFijo() { return cargoFijo; }
        public void setCargoFijo(double cargoFijo) { this.cargoFijo = cargoFijo; }
        
        public boolean isIncluirDap() { return incluirDap; }
        public void setIncluirDap(boolean incluirDap) { this.incluirDap = incluirDap; }
        
        public double getPorcentajeDap() { return porcentajeDap; }
        public void setPorcentajeDap(double porcentajeDap) { this.porcentajeDap = porcentajeDap; }
    }
    
    public static class CalculoResultado {
        private String clienteNombre;
        private String columnaSensor;
        private int diasPeriodo;
        
        // Consumos kWh
        private double kwhBase;
        private double kwhIntermedio;
        private double kwhPunta;
        
        // Demandas kW
        private double maxBase;
        private double maxIntermedio;
        private double maxPunta;
        private double demandaFacturable;
        private double demandaDistribucion;
        
        // Costos
        private double costoBase;
        private double costoIntermedio;
        private double costoPunta;
        private double costoCapacidad;
        private double costoDistribucion;
        private double energia;
        private double cargoFijo;
        private double subtotal;
        private double dap;
        private double subtotalConDap;
        private double iva;
        private double total;
        
        // Getters y Setters (todos los campos)
        public String getClienteNombre() { return clienteNombre; }
        public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
        
        public String getColumnaSensor() { return columnaSensor; }
        public void setColumnaSensor(String columnaSensor) { this.columnaSensor = columnaSensor; }
        
        public int getDiasPeriodo() { return diasPeriodo; }
        public void setDiasPeriodo(int diasPeriodo) { this.diasPeriodo = diasPeriodo; }
        
        public double getKwhBase() { return kwhBase; }
        public void setKwhBase(double kwhBase) { this.kwhBase = kwhBase; }
        
        public double getKwhIntermedio() { return kwhIntermedio; }
        public void setKwhIntermedio(double kwhIntermedio) { this.kwhIntermedio = kwhIntermedio; }
        
        public double getKwhPunta() { return kwhPunta; }
        public void setKwhPunta(double kwhPunta) { this.kwhPunta = kwhPunta; }
        
        public double getMaxBase() { return maxBase; }
        public void setMaxBase(double maxBase) { this.maxBase = maxBase; }
        
        public double getMaxIntermedio() { return maxIntermedio; }
        public void setMaxIntermedio(double maxIntermedio) { this.maxIntermedio = maxIntermedio; }
        
        public double getMaxPunta() { return maxPunta; }
        public void setMaxPunta(double maxPunta) { this.maxPunta = maxPunta; }
        
        public double getDemandaFacturable() { return demandaFacturable; }
        public void setDemandaFacturable(double demandaFacturable) { this.demandaFacturable = demandaFacturable; }
        
        public double getDemandaDistribucion() { return demandaDistribucion; }
        public void setDemandaDistribucion(double demandaDistribucion) { this.demandaDistribucion = demandaDistribucion; }
        
        public double getCostoBase() { return costoBase; }
        public void setCostoBase(double costoBase) { this.costoBase = costoBase; }
        
        public double getCostoIntermedio() { return costoIntermedio; }
        public void setCostoIntermedio(double costoIntermedio) { this.costoIntermedio = costoIntermedio; }
        
        public double getCostoPunta() { return costoPunta; }
        public void setCostoPunta(double costoPunta) { this.costoPunta = costoPunta; }
        
        public double getCostoCapacidad() { return costoCapacidad; }
        public void setCostoCapacidad(double costoCapacidad) { this.costoCapacidad = costoCapacidad; }
        
        public double getCostoDistribucion() { return costoDistribucion; }
        public void setCostoDistribucion(double costoDistribucion) { this.costoDistribucion = costoDistribucion; }
        
        public double getEnergia() { return energia; }
        public void setEnergia(double energia) { this.energia = energia; }
        
        public double getCargoFijo() { return cargoFijo; }
        public void setCargoFijo(double cargoFijo) { this.cargoFijo = cargoFijo; }
        
        public double getSubtotal() { return subtotal; }
        public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
        
        public double getDap() { return dap; }
        public void setDap(double dap) { this.dap = dap; }
        
        public double getSubtotalConDap() { return subtotalConDap; }
        public void setSubtotalConDap(double subtotalConDap) { this.subtotalConDap = subtotalConDap; }
        
        public double getIva() { return iva; }
        public void setIva(double iva) { this.iva = iva; }
        
        public double getTotal() { return total; }
        public void setTotal(double total) { this.total = total; }
    }
    
    public static class DatosTarifa {
        private String tarifa;
        private int cantidadRegistros;
        private double sumaKwh;
        private double promedioKw;
        private double maxDemanda;
        
        // Getters y Setters
        public String getTarifa() { return tarifa; }
        public void setTarifa(String tarifa) { this.tarifa = tarifa; }
        
        public int getCantidadRegistros() { return cantidadRegistros; }
        public void setCantidadRegistros(int cantidadRegistros) { this.cantidadRegistros = cantidadRegistros; }
        
        public double getSumaKwh() { return sumaKwh; }
        public void setSumaKwh(double sumaKwh) { this.sumaKwh = sumaKwh; }
        
        public double getPromedioKw() { return promedioKw; }
        public void setPromedioKw(double promedioKw) { this.promedioKw = promedioKw; }
        
        public double getMaxDemanda() { return maxDemanda; }
        public void setMaxDemanda(double maxDemanda) { this.maxDemanda = maxDemanda; }
    }
}