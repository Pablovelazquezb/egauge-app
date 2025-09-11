package com.egauge.controller;

import com.egauge.service.DescargaService;
import com.egauge.service.DescargaService.DescargaResultado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/descarga")
@CrossOrigin(origins = "http://localhost:3000")
public class DescargaController {
    
    @Autowired
    private DescargaService descargaService;
    
    /**
     * POST /api/descarga/cliente/{clienteId} - Descargar datos para un cliente
     */
    @PostMapping("/cliente/{clienteId}")
    public ResponseEntity<?> descargarDatosCliente(
            @PathVariable Long clienteId,
            @RequestBody DescargaRequest request) {
        
        try {
            // Validar request
            if (request.getFechaInicio() == null || request.getFechaFin() == null) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("fechaInicio y fechaFin son requeridos"));
            }
            
            // Parsear fechas
            LocalDateTime inicio = parseDateTime(request.getFechaInicio());
            LocalDateTime fin = parseDateTime(request.getFechaFin());
            
            if (inicio.isAfter(fin)) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("fechaInicio debe ser anterior a fechaFin"));
            }
            
            // Ejecutar descarga
            DescargaResultado resultado = descargaService.descargarDatos(clienteId, inicio, fin);
            
            return ResponseEntity.ok(resultado);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Formato de fecha inválido: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error interno: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/descarga/cliente/{clienteId}/ultimo-timestamp - Obtener último timestamp descargado
     */
    @GetMapping("/cliente/{clienteId}/ultimo-timestamp")
    public ResponseEntity<?> obtenerUltimoTimestamp(@PathVariable Long clienteId) {
        try {
            // Implementar lógica para obtener último timestamp
            // Por ahora retornamos respuesta básica
            return ResponseEntity.ok(new UltimoTimestampResponse(null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error obteniendo último timestamp"));
        }
    }
    
    /**
     * GET /api/descarga/cliente/{clienteId}/stats - Estadísticas de datos descargados
     */
    @GetMapping("/cliente/{clienteId}/stats")
    public ResponseEntity<?> obtenerEstadisticas(@PathVariable Long clienteId) {
        try {
            // Implementar lógica de estadísticas
            return ResponseEntity.ok(new EstadisticasResponse(0L, null, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error obteniendo estadísticas"));
        }
    }
    
    /**
     * Parsea string de fecha a LocalDateTime
     */
    private LocalDateTime parseDateTime(String fechaStr) {
        if (fechaStr == null || fechaStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Fecha no puede estar vacía");
        }
        
        // Formatos soportados
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(fechaStr, formatter);
            } catch (DateTimeParseException e) {
                // Intentar siguiente formato
            }
        }
        
        throw new IllegalArgumentException("Formato de fecha no soportado: " + fechaStr);
    }
    
    // ========== Clases de Request/Response ==========
    
    public static class DescargaRequest {
        private String fechaInicio;
        private String fechaFin;
        private Integer pasoSegundos = 3600; // Default: cada hora
        
        public String getFechaInicio() { return fechaInicio; }
        public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }
        
        public String getFechaFin() { return fechaFin; }
        public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }
        
        public Integer getPasoSegundos() { return pasoSegundos; }
        public void setPasoSegundos(Integer pasoSegundos) { this.pasoSegundos = pasoSegundos; }
    }
    
    public static class ErrorResponse {
        private String error;
        private long timestamp;
        
        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
    
    public static class UltimoTimestampResponse {
        private String ultimoTimestamp;
        
        public UltimoTimestampResponse(String ultimoTimestamp) {
            this.ultimoTimestamp = ultimoTimestamp;
        }
        
        public String getUltimoTimestamp() { return ultimoTimestamp; }
        public void setUltimoTimestamp(String ultimoTimestamp) { this.ultimoTimestamp = ultimoTimestamp; }
    }
    
    public static class EstadisticasResponse {
        private Long totalRegistros;
        private String primerRegistro;
        private String ultimoRegistro;
        
        public EstadisticasResponse(Long totalRegistros, String primerRegistro, String ultimoRegistro) {
            this.totalRegistros = totalRegistros;
            this.primerRegistro = primerRegistro;
            this.ultimoRegistro = ultimoRegistro;
        }
        
        public Long getTotalRegistros() { return totalRegistros; }
        public void setTotalRegistros(Long totalRegistros) { this.totalRegistros = totalRegistros; }
        
        public String getPrimerRegistro() { return primerRegistro; }
        public void setPrimerRegistro(String primerRegistro) { this.primerRegistro = primerRegistro; }
        
        public String getUltimoRegistro() { return ultimoRegistro; }
        public void setUltimoRegistro(String ultimoRegistro) { this.ultimoRegistro = ultimoRegistro; }
    }
}