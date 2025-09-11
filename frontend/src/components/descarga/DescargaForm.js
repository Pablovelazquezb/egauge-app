import React, { useState, useEffect } from 'react';
import { clienteService } from '../../services/clienteService';
import { descargaService } from '../../services/descargaService';

const DescargaForm = () => {
  const [clientes, setClientes] = useState([]);
  const [clienteSeleccionado, setClienteSeleccionado] = useState('');
  const [fechaInicio, setFechaInicio] = useState('');
  const [fechaFin, setFechaFin] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [progreso, setProgreso] = useState(0);

  // Cargar clientes al inicializar
  useEffect(() => {
    cargarClientes();
    // Configurar fechas por defecto (último mes)
    const hoy = new Date();
    const mesAnterior = new Date(hoy.getFullYear(), hoy.getMonth() - 1, 1);
    
    setFechaFin(hoy.toISOString().slice(0, 16)); // YYYY-MM-DDTHH:MM
    setFechaInicio(mesAnterior.toISOString().slice(0, 16));
  }, []);

  const cargarClientes = async () => {
    try {
      const data = await clienteService.obtenerActivos();
      setClientes(data);
    } catch (err) {
      setError('Error cargando clientes');
    }
  };

  const calcularPuntosEstimados = () => {
    if (!fechaInicio || !fechaFin) return 0;
    
    const inicio = new Date(fechaInicio);
    const fin = new Date(fechaFin);
    const diferenciaDias = (fin - inicio) / (1000 * 60 * 60 * 24);
    
    return Math.ceil(diferenciaDias * 24); // 24 puntos por día (cada hora)
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!clienteSeleccionado) {
      setError('Selecciona un cliente');
      return;
    }

    if (!fechaInicio || !fechaFin) {
      setError('Selecciona fechas de inicio y fin');
      return;
    }

    if (new Date(fechaInicio) >= new Date(fechaFin)) {
      setError('La fecha de inicio debe ser anterior a la fecha de fin');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      setResultado(null);
      setProgreso(0);

      // Simular progreso (en una implementación real, esto vendría del backend)
      const interval = setInterval(() => {
        setProgreso(prev => {
          if (prev >= 90) {
            clearInterval(interval);
            return 90;
          }
          return prev + 10;
        });
      }, 500);

      // Ejecutar descarga
      const data = await descargaService.descargarDatos(
        clienteSeleccionado,
        fechaInicio,
        fechaFin
      );

      clearInterval(interval);
      setProgreso(100);
      setResultado(data);

    } catch (err) {
      setError(err.response?.data?.error || 'Error en la descarga');
    } finally {
      setLoading(false);
    }
  };

  const clienteInfo = clientes.find(c => c.id === parseInt(clienteSeleccionado));
  const puntosEstimados = calcularPuntosEstimados();

  return (
    <div className="descarga-form">
      <div className="form-header">
        <h2>📥 Descarga de Datos eGauge</h2>
        <p>Descarga y clasifica datos de consumo eléctrico</p>
      </div>

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="descarga-form-content">
        
        {/* Selector de Cliente */}
        <div className="form-group">
          <label htmlFor="cliente">Cliente *</label>
          <select
            id="cliente"
            value={clienteSeleccionado}
            onChange={(e) => setClienteSeleccionado(e.target.value)}
            required
            disabled={loading}
          >
            <option value="">Selecciona un cliente...</option>
            {clientes.map(cliente => (
              <option key={cliente.id} value={cliente.id}>
                {cliente.nombreCliente} ({cliente.hostname})
              </option>
            ))}
          </select>
        </div>

        {/* Información del Cliente Seleccionado */}
        {clienteInfo && (
          <div className="cliente-info">
            <h4>📋 Cliente Seleccionado:</h4>
            <p><strong>Nombre:</strong> {clienteInfo.nombreCliente}</p>
            <p><strong>Hostname:</strong> {clienteInfo.hostname}</p>
            <p><strong>Tabla:</strong> {clienteInfo.tablaNombre}</p>
            {clienteInfo.urlCompleta && (
              <p>
                <strong>URL:</strong> 
                <a href={clienteInfo.urlCompleta} target="_blank" rel="noopener noreferrer">
                  Ver eGauge
                </a>
              </p>
            )}
          </div>
        )}

        {/* Rango de Fechas */}
        <div className="form-row">
          <div className="form-group">
            <label htmlFor="fechaInicio">Fecha y Hora Inicio *</label>
            <input
              type="datetime-local"
              id="fechaInicio"
              value={fechaInicio}
              onChange={(e) => setFechaInicio(e.target.value)}
              required
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="fechaFin">Fecha y Hora Fin *</label>
            <input
              type="datetime-local"
              id="fechaFin"
              value={fechaFin}
              onChange={(e) => setFechaFin(e.target.value)}
              required
              disabled={loading}
            />
          </div>
        </div>

        {/* Estimación */}
        {puntosEstimados > 0 && (
          <div className="estimacion">
            <h4>📊 Estimación de Descarga:</h4>
            <p><strong>Puntos de datos:</strong> ~{puntosEstimados.toLocaleString()}</p>
            <p><strong>Tiempo estimado:</strong> ~{Math.ceil(puntosEstimados / 120)} minutos</p>
            <p><small>* Basado en 1 punto cada hora y 2 puntos por segundo</small></p>
          </div>
        )}

        {/* Botón de Descarga */}
        <div className="form-actions">
          <button 
            type="submit" 
            className="btn-primary"
            disabled={loading || !clienteSeleccionado}
          >
            {loading ? '🔄 Descargando...' : '🚀 Iniciar Descarga'}
          </button>
        </div>
      </form>

      {/* Barra de Progreso */}
      {loading && (
        <div className="progreso-container">
          <h4>⏳ Descarga en Progreso...</h4>
          <div className="progress-bar">
            <div 
              className="progress-fill" 
              style={{ width: `${progreso}%` }}
            ></div>
          </div>
          <p>{progreso}% completado</p>
        </div>
      )}

      {/* Resultado */}
      {resultado && (
        <div className="resultado-container">
          <h4>✅ Descarga Completada</h4>
          <div className="resultado-stats">
            <div className="stat">
              <span className="stat-label">Cliente:</span>
              <span className="stat-value">{resultado.cliente}</span>
            </div>
            <div className="stat">
              <span className="stat-label">Puntos procesados:</span>
              <span className="stat-value">{resultado.totalPuntos.toLocaleString()}</span>
            </div>
            <div className="stat">
              <span className="stat-label">Filas insertadas:</span>
              <span className="stat-value">{resultado.filasInsertadas.toLocaleString()}</span>
            </div>
            <div className="stat">
              <span className="stat-label">Errores:</span>
              <span className="stat-value">{resultado.errores}</span>
            </div>
            <div className="stat">
              <span className="stat-label">Éxito:</span>
              <span className={`stat-value ${resultado.exito ? 'success' : 'error'}`}>
                {resultado.exito ? '✅ Sí' : '❌ No'}
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DescargaForm;