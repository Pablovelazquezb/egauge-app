package com.egauge.repository;

import com.egauge.entity.DatosEgauge;
import com.egauge.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DatosEgaugeRepository extends JpaRepository<DatosEgauge, Long> {
    
    // Buscar datos por cliente
    List<DatosEgauge> findByCliente(Cliente cliente);
    
    // Buscar datos por cliente y rango de fechas
    List<DatosEgauge> findByClienteAndTimestampBetween(
        Cliente cliente, 
        LocalDateTime inicio, 
        LocalDateTime fin
    );
    
    // Verificar si existe un timestamp específico para un cliente
    boolean existsByClienteAndTimestamp(Cliente cliente, LocalDateTime timestamp);
    
    // Contar registros por cliente
    Long countByCliente(Cliente cliente);
}