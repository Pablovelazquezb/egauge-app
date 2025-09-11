package com.egauge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "datos_egauge", 
       indexes = {
           @Index(name = "idx_datos_timestamp", columnList = "timestamp"),
           @Index(name = "idx_datos_cliente_id", columnList = "cliente_id"),
           @Index(name = "idx_datos_tarifa", columnList = "tarifa")
       })
public class DatosEgauge {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "tarifa")
    private String tarifa; // "Base", "Intermedio", "Punta"
    
    // Guardar todos los datos como JSON para máxima flexibilidad
    @Column(name = "datos_sensores", columnDefinition = "TEXT")
    private String datosSensores; // JSON: {"Uso [kW]": 25.5, "Generación [kW]": 12.3, ...}
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructores
    public DatosEgauge() {}
    
    public DatosEgauge(Cliente cliente, LocalDateTime timestamp, String tarifa, String datosSensores) {
        this.cliente = cliente;
        this.timestamp = timestamp;
        this.tarifa = tarifa;
        this.datosSensores = datosSensores;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters y Setters básicos
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getTarifa() { return tarifa; }
    public void setTarifa(String tarifa) { this.tarifa = tarifa; }
    
    public String getDatosSensores() { return datosSensores; }
    public void setDatosSensores(String datosSensores) { this.datosSensores = datosSensores; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    // Métodos de utilidad para trabajar con JSON
    @Transient
    public Map<String, Double> getSensoresAsMap() {
        if (datosSensores == null || datosSensores.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.core.type.TypeReference<Map<String, Double>> typeRef = 
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Double>>() {};
            return mapper.readValue(datosSensores, typeRef);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
    
    @Transient
    public void setSensoresFromMap(Map<String, Double> sensores) {
        if (sensores == null || sensores.isEmpty()) {
            this.datosSensores = "{}";
            return;
        }
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            this.datosSensores = mapper.writeValueAsString(sensores);
        } catch (Exception e) {
            this.datosSensores = "{}";
        }
    }
    
    @Transient
    public Double getValorSensor(String nombreSensor) {
        Map<String, Double> sensores = getSensoresAsMap();
        return sensores.get(nombreSensor);
    }
    
    @Transient
    public void agregarSensor(String nombre, Double valor) {
        Map<String, Double> sensores = getSensoresAsMap();
        sensores.put(nombre, valor);
        setSensoresFromMap(sensores);
    }
}