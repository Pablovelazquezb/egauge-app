package com.egauge.controller;

import com.egauge.service.CalculadoraCFEService;
import com.egauge.service.CalculadoraCFEService.CalculoRequest;
import com.egauge.service.CalculadoraCFEService.CalculoResultado;
import com.egauge.service.CalculadoraCFEService.PreciosCFE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/calculadora")
@CrossOrigin(origins = "http://localhost:3000")
public class CalculadoraCFEController {
    
    @Autowired
    private CalculadoraCFEService calculadoraService;
    
    /**
     * POST /api/calculadora/calcular - Calcular costos CFE
     */
    @PostMapping("/calcular")
    public ResponseEntity<?> calcularCostosCFE(@RequestBody CalculadoraRequest request) {
        try {
            // Validar request
            if (request.getClienteId() == null) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("clienteId es requerido"));
            }
            
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
            
            // Crear request para el service
            CalculoRequest calculoRequest = new CalculoRequest();
            calculoRequest.setClienteId(request.getClienteId());
            calculoRequest.setFechaInicio(inicio);
            calculoRequest.setFechaFin(fin);
            calculoRequest.setColumnaSensor(request.getColumnaSensor());
            
            // Configurar precios (usar defaults si no se especifican)
            PreciosCFE precios = new PreciosCFE();
            if (request.getPrecios() != null) {
                Precios preciosRequest = request.getPrecios();
                
                if (preciosRequest.getPrecioBase() != null) {
                    precios.setPrecioBase(preciosRequest.getPrecioBase());
                }
                if (preciosRequest.getPrecioIntermedio() != null) {
                    precios.setPrecioIntermedio(preciosRequest.getPrecioIntermedio());
                }
                if (preciosRequest.getPrecioPunta() != null) {
                    precios.setPrecioPunta(preciosRequest.getPrecioPunta());
                }
                if (preciosRequest.getPrecioCapacidad() != null) {
                    precios.setPrecioCapacidad(preciosRequest.getPrecioCapacidad());
                }
                if (preciosRequest.getPrecioDistribucion() != null) {
                    precios.setPrecioDistribucion(preciosRequest.getPrecioDistribucion());
                }
                if (preciosRequest.getCargoFijo() != null) {
                    precios.setCargoFijo(preciosRequest.getCargoFijo());
                }
                if (preciosRequest.getIncluirDap() != null) {
                    precios.setIncluirDap(preciosRequest.getIncluirDap());
                }
                if (preciosRequest.getPorcentajeDap() != null) {
                    precios.setPorcentajeDap(preciosRequest.getPorcentajeDap());
                }
            }
            
            calculoRequest.setPrecios(precios);
            
            // Ejecutar cálculo
            CalculoResultado resultado = calculadoraService.calcularCostosCFE(calculoRequest);
            
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
     * GET /api/calculadora/cliente/{clienteId}/columnas - Obtener columnas disponibles para un cliente
     */
    @GetMapping("/cliente/{clienteId}/columnas")
    public ResponseEntity<?> obtenerColumnasDisponibles(@PathVariable Long clienteId) {
        try {
            List<String> columnas = calculadoraService.obtenerColumnasDisponibles(clienteId);
            return ResponseEntity.ok(new ColumnasResponse(columnas));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error obteniendo columnas disponibles"));
        }
    }
    
    /**
     * GET /api/calculadora/precios-default - Obtener precios CFE por defecto
     */
    @GetMapping("/precios-default")
    public ResponseEntity<PreciosResponse> obtenerPreciosDefault() {
        PreciosCFE preciosDefault = new PreciosCFE();
        
        PreciosResponse response = new PreciosResponse();
        response.setPrecioBase(preciosDefault.getPrecioBase());
        response.setPrecioIntermedio(preciosDefault.getPrecioIntermedio());
        response.setPrecioPunta(preciosDefault.getPrecioPunta());
        response.setPrecioCapacidad(preciosDefault.getPrecioCapacidad());
        response.setPrecioDistribucion(preciosDefault.getPrecioDistribucion());
        response.setCargoFijo(preciosDefault.getCargoFijo());
        response.setIncluirDap(preciosDefault.isIncluirDap());
        response.setPorcentajeDap(preciosDefault.getPorcentajeDap());
        
        return ResponseEntity.ok(response);
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
    
    // ========== CLASES DE REQUEST/RESPONSE ==========
    
    public static class CalculadoraRequest {
        private Long clienteId;
        private String fechaInicio;
        private String fechaFin;
        private String columnaSensor;
        private Precios precios;
        
        // Getters y Setters
        public Long getClienteId() { return clienteId; }
        public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
        
        public String getFechaInicio() { return fechaInicio; }
        public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }
        
        public String getFechaFin() { return fechaFin; }
        public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }
        
        public String getColumnaSensor() { return columnaSensor; }
        public void setColumnaSensor(String columnaSensor) { this.columnaSensor = columnaSensor; }
        
        public Precios getPrecios() { return precios; }
        public void setPrecios(Precios precios) { this.precios = precios; }
    }
    
    public static class Precios {
        private Double precioBase;
        private Double precioIntermedio;
        private Double precioPunta;
        private Double precioCapacidad;
        private Double precioDistribucion;
        private Double cargoFijo;
        private Boolean incluirDap;
        private Double porcentajeDap;
        
        // Getters y Setters
        public Double getPrecioBase() { return precioBase; }
        public void setPrecioBase(Double precioBase) { this.precioBase = precioBase; }
        
        public Double getPrecioIntermedio() { return precioIntermedio; }
        public void setPrecioIntermedio(Double precioIntermedio) { this.precioIntermedio = precioIntermedio; }
        
        public Double getPrecioPunta() { return precioPunta; }
        public void setPrecioPunta(Double precioPunta) { this.precioPunta = precioPunta; }
        
        public Double getPrecioCapacidad() { return precioCapacidad; }
        public void setPrecioCapacidad(Double precioCapacidad) { this.precioCapacidad = precioCapacidad; }
        
        public Double getPrecioDistribucion() { return precioDistribucion; }
        public void setPrecioDistribucion(Double precioDistribucion) { this.precioDistribucion = precioDistribucion; }
        
        public Double getCargoFijo() { return cargoFijo; }
        public void setCargoFijo(Double cargoFijo) { this.cargoFijo = cargoFijo; }
        
        public Boolean getIncluirDap() { return incluirDap; }
        public void setIncluirDap(Boolean incluirDap) { this.incluirDap = incluirDap; }
        
        public Double getPorcentajeDap() { return porcentajeDap; }
        public void setPorcentajeDap(Double porcentajeDap) { this.porcentajeDap = porcentajeDap; }
    }
    
    public static class PreciosResponse {
        private double precioBase;
        private double precioIntermedio;
        private double precioPunta;
        private double precioCapacidad;
        private double precioDistribucion;
        private double cargoFijo;
        private boolean incluirDap;
        private double porcentajeDap;
        
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
    
    public static class ColumnasResponse {
        private List<String> columnas;
        
        public ColumnasResponse(List<String> columnas) {
            this.columnas = columnas;
        }
        
        public List<String> getColumnas() { return columnas; }
        public void setColumnas(List<String> columnas) { this.columnas = columnas; }
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
}