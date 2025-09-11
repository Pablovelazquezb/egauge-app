package com.egauge.controller;

import com.egauge.entity.Cliente;
import com.egauge.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "http://localhost:3000") // Para conectar con React
public class ClienteController {
    
    @Autowired
    private ClienteService clienteService;
    
    // GET /api/clientes - Obtener todos los clientes
    @GetMapping
    public ResponseEntity<List<Cliente>> obtenerTodos() {
        try {
            List<Cliente> clientes = clienteService.obtenerTodos();
            return ResponseEntity.ok(clientes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // GET /api/clientes/activos - Obtener solo clientes activos
    @GetMapping("/activos")
    public ResponseEntity<List<Cliente>> obtenerActivos() {
        try {
            List<Cliente> clientes = clienteService.obtenerActivos();
            return ResponseEntity.ok(clientes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // GET /api/clientes/{id} - Obtener cliente por ID
    @GetMapping("/{id}")
    public ResponseEntity<Cliente> obtenerPorId(@PathVariable Long id) {
        try {
            Optional<Cliente> cliente = clienteService.obtenerPorId(id);
            return cliente.map(ResponseEntity::ok)
                          .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // POST /api/clientes - Crear nuevo cliente
    @PostMapping
    public ResponseEntity<?> crearCliente(@RequestBody Cliente cliente) {
        try {
            Cliente nuevoCliente = clienteService.crearCliente(cliente);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoCliente);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error interno del servidor");
        }
    }
    
    // PUT /api/clientes/{id} - Actualizar cliente
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCliente(@PathVariable Long id, @RequestBody Cliente cliente) {
        try {
            Cliente clienteActualizado = clienteService.actualizarCliente(id, cliente);
            return ResponseEntity.ok(clienteActualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error interno del servidor");
        }
    }
    
    // PATCH /api/clientes/{id}/toggle - Activar/Desactivar cliente
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        try {
            Cliente cliente = clienteService.toggleActivo(id);
            return ResponseEntity.ok(cliente);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error interno del servidor");
        }
    }
    
    // DELETE /api/clientes/{id} - Eliminar cliente
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCliente(@PathVariable Long id) {
        try {
            clienteService.eliminarCliente(id);
            return ResponseEntity.ok("Cliente eliminado exitosamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error interno del servidor");
        }
    }
    
    // GET /api/clientes/buscar?nombre={nombre} - Buscar por nombre
    @GetMapping("/buscar")
    public ResponseEntity<List<Cliente>> buscarPorNombre(@RequestParam String nombre) {
        try {
            List<Cliente> clientes = clienteService.buscarPorNombre(nombre);
            return ResponseEntity.ok(clientes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // GET /api/clientes/stats - Estadísticas
    @GetMapping("/stats")
    public ResponseEntity<?> obtenerEstadisticas() {
        try {
            Long clientesActivos = clienteService.contarClientesActivos();
            return ResponseEntity.ok(new EstadisticasResponse(clientesActivos));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Clase para respuesta de estadísticas
    public static class EstadisticasResponse {
        private Long clientesActivos;
        
        public EstadisticasResponse(Long clientesActivos) {
            this.clientesActivos = clientesActivos;
        }
        
        public Long getClientesActivos() { return clientesActivos; }
        public void setClientesActivos(Long clientesActivos) { this.clientesActivos = clientesActivos; }
    }
}