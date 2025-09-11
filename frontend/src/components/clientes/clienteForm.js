import React, { useState, useEffect } from 'react';
import { clienteService } from '../../services/clienteService';

const ClienteForm = ({ cliente, onSave, onCancel }) => {
  const [formData, setFormData] = useState({
    nombreCliente: '',
    hostname: '',
    urlCompleta: '',
    tablaNombre: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Llenar formulario si estamos editando
  useEffect(() => {
    if (cliente) {
      setFormData({
        nombreCliente: cliente.nombreCliente || '',
        hostname: cliente.hostname || '',
        urlCompleta: cliente.urlCompleta || '',
        tablaNombre: cliente.tablaNombre || ''
      });
    }
  }, [cliente]);

  // Función para extraer hostname de URL
  const extraerHostname = (url) => {
    try {
      if (url.startsWith('http')) {
        const urlObj = new URL(url);
        return urlObj.hostname;
      }
      return url.trim();
    } catch {
      return url.trim();
    }
  };

  // Función para limpiar nombre de tabla
  const limpiarNombreTabla = (nombre) => {
    if (!nombre.trim()) return 'cliente_sin_nombre';
    
    let tablaNombre = nombre.toLowerCase().replace(/ /g, '_');
    tablaNombre = tablaNombre.replace(/[-.()\[\]&@#$%^*+=|]/g, '_');
    
    while (tablaNombre.includes('__')) {
      tablaNombre = tablaNombre.replace('__', '_');
    }
    
    tablaNombre = tablaNombre.replace(/^_+|_+$/g, '');
    
    return tablaNombre || 'cliente_sin_nombre';
  };

  // Manejar cambios en el formulario
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));

    // Auto-completar campos relacionados
    if (name === 'urlCompleta' && value) {
      const hostname = extraerHostname(value);
      setFormData(prev => ({
        ...prev,
        hostname: hostname
      }));
    }

    if (name === 'nombreCliente' && value) {
      const tablaNombre = limpiarNombreTabla(value);
      setFormData(prev => ({
        ...prev,
        tablaNombre: tablaNombre
      }));
    }
  };

  // Enviar formulario
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validaciones básicas
    if (!formData.nombreCliente.trim()) {
      setError('El nombre del cliente es requerido');
      return;
    }
    
    if (!formData.hostname.trim()) {
      setError('El hostname es requerido');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      // Preparar datos
      const clienteData = {
        ...formData,
        tablaNombre: limpiarNombreTabla(formData.nombreCliente)
      };

      // Crear o actualizar
      if (cliente && cliente.id) {
        await clienteService.actualizar(cliente.id, clienteData);
      } else {
        await clienteService.crear(clienteData);
      }

      onSave(); // Callback para recargar lista
    } catch (err) {
      setError(err.response?.data || 'Error guardando cliente');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="cliente-form-overlay">
      <div className="cliente-form">
        <div className="form-header">
          <h3>{cliente ? '✏️ Editar Cliente' : '➕ Nuevo Cliente'}</h3>
          <button className="btn-close" onClick={onCancel}>✖️</button>
        </div>

        {error && <div className="error-message">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="nombreCliente">Nombre del Cliente *</label>
            <input
              type="text"
              id="nombreCliente"
              name="nombreCliente"
              value={formData.nombreCliente}
              onChange={handleChange}
              placeholder="Ej: Macro 2 Nave 4 SE 2"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="urlCompleta">URL del eGauge</label>
            <input
              type="url"
              id="urlCompleta"
              name="urlCompleta"
              value={formData.urlCompleta}
              onChange={handleChange}
              placeholder="https://egauge86216.egaug.es/63C1A/l/es/classic.html"
            />
          </div>

          <div className="form-group">
            <label htmlFor="hostname">Hostname *</label>
            <input
              type="text"
              id="hostname"
              name="hostname"
              value={formData.hostname}
              onChange={handleChange}
              placeholder="egauge86216.egaug.es"
              required
            />
            <small>Se completa automáticamente al escribir la URL</small>
          </div>

          <div className="form-group">
            <label htmlFor="tablaNombre">Nombre de Tabla</label>
            <input
              type="text"
              id="tablaNombre"
              name="tablaNombre"
              value={formData.tablaNombre}
              readOnly
              placeholder="Se genera automáticamente"
            />
            <small>Se genera automáticamente basado en el nombre del cliente</small>
          </div>

          <div className="form-actions">
            <button 
              type="button" 
              className="btn-secondary"
              onClick={onCancel}
              disabled={loading}
            >
              Cancelar
            </button>
            <button 
              type="submit" 
              className="btn-primary"
              disabled={loading}
            >
              {loading ? 'Guardando...' : (cliente ? 'Actualizar' : 'Crear Cliente')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ClienteForm;