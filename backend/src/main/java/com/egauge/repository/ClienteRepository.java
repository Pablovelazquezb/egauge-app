package com.egauge.repository;

import com.egauge.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    // Buscar clientes activos
    List<Cliente> findByActivoTrue();
    
    // Buscar por hostname
    Optional<Cliente> findByHostname(String hostname);
    
    // Buscar por tabla nombre
    Optional<Cliente> findByTablaNombre(String tablaNombre);
    
    // Buscar por nombre (ignorando mayúsculas/minúsculas)
    List<Cliente> findByNombreClienteContainingIgnoreCase(String nombre);
    
    // Contar clientes activos
    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.activo = true")
    Long countClientesActivos();
    
    // Verificar si existe hostname
    boolean existsByHostname(String hostname);
    
    // Verificar si existe tabla nombre
    boolean existsByTablaNombre(String tablaNombre);
}