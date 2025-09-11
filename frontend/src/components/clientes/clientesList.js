import React, { useState, useEffect } from 'react';
import { clienteService } from '../../services/clienteService';

const ClientesList = ({ onEdit, onToggleForm }) => {
  const [clientes, setClientes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Cargar clientes al iniciar
  useEffect(() => {
    cargarClientes();
  }, []);

  const cargarClientes = async () => {
    try {
      setLoading(true);
      const data = await clienteService.obtenerTodos();
      setClientes(data);
      setError(null);
    } catch (err) {
      setError('Error cargando clientes');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleActivo = async (id) => {
    try {
      await clienteService.toggleActivo(id);
      cargarClientes(); // Recargar lista
    } catch (err) {
      setError('Error cambiando estado del cliente');
    }
  };

  const handleEliminar = async (id, nombre) => {
    if (window.confirm(`¿Eliminar cliente "${nombre}"?`)) {
      try {
        await clienteService.eliminar(id);
        cargarClientes(); // Recargar lista
      } catch (err) {
        setError('Error eliminando cliente');
      }
    }
  };

  if (loading) return <div className="loading">Cargando clientes...</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div className="clientes-list">
      <div className="clientes-header">
        <h2>👥 Gestión de Clientes</h2>
        <button 
          className="btn-primary"
          onClick={onToggleForm}
        >
          ➕ Agregar Cliente
        </button>
      </div>

      {clientes.length === 0 ? (
        <div className="empty-state">
          <p>No hay clientes registrados</p>
          <button className="btn-primary" onClick={onToggleForm}>
            Agregar primer cliente
          </button>
        </div>
      ) : (
        <div className="clientes-grid">
          {clientes.map(cliente => (
            <div key={cliente.id} className={`cliente-card ${!cliente.activo ? 'inactivo' : ''}`}>
              <div className="cliente-info">
                <h3>{cliente.nombreCliente}</h3>
                <p><strong>Hostname:</strong> {cliente.hostname}</p>
                <p><strong>Tabla:</strong> {cliente.tablaNombre}</p>
                <p className={`status ${cliente.activo ? 'activo' : 'inactivo'}`}>
                  {cliente.activo ? '✅ Activo' : '❌ Inactivo'}
                </p>
              </div>
              
              <div className="cliente-actions">
                <button 
                  className="btn-secondary"
                  onClick={() => onEdit(cliente)}
                >
                  ✏️ Editar
                </button>
                
                <button 
                  className={`btn-toggle ${cliente.activo ? 'btn-warning' : 'btn-success'}`}
                  onClick={() => handleToggleActivo(cliente.id)}
                >
                  {cliente.activo ? '⏸️ Desactivar' : '▶️ Activar'}
                </button>
                
                <button 
                  className="btn-danger"
                  onClick={() => handleEliminar(cliente.id, cliente.nombreCliente)}
                >
                  🗑️ Eliminar
                </button>
                
                {cliente.urlCompleta && (
                  <a 
                    href={cliente.urlCompleta} 
                    target="_blank" 
                    rel="noopener noreferrer"
                    className="btn-link"
                  >
                    🔗 Ver eGauge
                  </a>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default ClientesList;