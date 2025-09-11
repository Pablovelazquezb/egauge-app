package com.egauge.service;

import com.egauge.entity.Cliente;
import com.egauge.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {
    
    @Autowired
    private ClienteRepository clienteRepository;
    
    // Obtener todos los clientes
    public List<Cliente> obtenerTodos() {
        return clienteRepository.findAll();
    }
    
    // Obtener solo clientes activos
    public List<Cliente> obtenerActivos() {
        return clienteRepository.findByActivoTrue();
    }
    
    // Obtener cliente por ID
    public Optional<Cliente> obtenerPorId(Long id) {
        return clienteRepository.findById(id);
    }
    
    // Crear nuevo cliente
    public Cliente crearCliente(Cliente cliente) {
        // Validaciones
        if (clienteRepository.existsByHostname(cliente.getHostname())) {
            throw new RuntimeException("Ya existe un cliente con este hostname: " + cliente.getHostname());
        }
        
        if (clienteRepository.existsByTablaNombre(cliente.getTablaNombre())) {
            throw new RuntimeException("Ya existe un cliente con este nombre de tabla: " + cliente.getTablaNombre());
        }
        
        // Limpiar nombre de tabla
        cliente.setTablaNombre(limpiarNombreTabla(cliente.getTablaNombre()));
        cliente.setActivo(true);
        
        return clienteRepository.save(cliente);
    }
    
    // Actualizar cliente
    public Cliente actualizarCliente(Long id, Cliente clienteActualizado) {
        return clienteRepository.findById(id)
            .map(cliente -> {
                cliente.setNombreCliente(clienteActualizado.getNombreCliente());
                cliente.setHostname(clienteActualizado.getHostname());
                cliente.setUrlCompleta(clienteActualizado.getUrlCompleta());
                cliente.setTablaNombre(limpiarNombreTabla(clienteActualizado.getTablaNombre()));
                cliente.setUpdatedAt(LocalDateTime.now());
                return clienteRepository.save(cliente);
            })
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
    }
    
    // Activar/Desactivar cliente
    public Cliente toggleActivo(Long id) {
        return clienteRepository.findById(id)
            .map(cliente -> {
                cliente.setActivo(!cliente.getActivo());
                cliente.setUpdatedAt(LocalDateTime.now());
                return clienteRepository.save(cliente);
            })
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
    }
    
    // Eliminar cliente
    public void eliminarCliente(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new RuntimeException("Cliente no encontrado con ID: " + id);
        }
        clienteRepository.deleteById(id);
    }
    
    // Buscar clientes por nombre
    public List<Cliente> buscarPorNombre(String nombre) {
        return clienteRepository.findByNombreClienteContainingIgnoreCase(nombre);
    }
    
    // Contar clientes activos
    public Long contarClientesActivos() {
        return clienteRepository.countClientesActivos();
    }
    
    // Utilidad: Limpiar nombre de tabla (igual que en Python)
    private String limpiarNombreTabla(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "cliente_sin_nombre";
        }
        
        String tablaNombre = nombre.toLowerCase().replace(' ', '_');
        
        // Limpiar caracteres especiales
        tablaNombre = tablaNombre.replaceAll("[\\-\\.\\(\\)\\[\\]&@#$%^*+=|]", "_");
        
        // Eliminar múltiples guiones bajos
        while (tablaNombre.contains("__")) {
            tablaNombre = tablaNombre.replace("__", "_");
        }
        
        // Remover guiones bajos al inicio y final
        tablaNombre = tablaNombre.replaceAll("^_+|_+$", "");
        
        if (tablaNombre.isEmpty()) {
            tablaNombre = "cliente_sin_nombre";
        }
        
        return tablaNombre;
    }
}