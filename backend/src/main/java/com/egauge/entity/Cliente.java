package com.egauge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "egauge_clientes")
public class Cliente {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "nombre_cliente", nullable = false)
    private String nombreCliente;
    
    @Column(name = "hostname", nullable = false)
    private String hostname;
    
    @Column(name = "url_completa")
    private String urlCompleta;
    
    @Column(name = "tabla_nombre", nullable = false, unique = true)
    private String tablaNombre;
    
    @Column(name = "activo")
    private Boolean activo = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructor vacío (requerido por JPA)
    public Cliente() {}
    
    // Constructor con parámetros
    public Cliente(String nombreCliente, String hostname, String urlCompleta, String tablaNombre) {
        this.nombreCliente = nombreCliente;
        this.hostname = hostname;
        this.urlCompleta = urlCompleta;
        this.tablaNombre = tablaNombre;
        this.activo = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    
    public String getUrlCompleta() { return urlCompleta; }
    public void setUrlCompleta(String urlCompleta) { this.urlCompleta = urlCompleta; }
    
    public String getTablaNombre() { return tablaNombre; }
    public void setTablaNombre(String tablaNombre) { this.tablaNombre = tablaNombre; }
    
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}