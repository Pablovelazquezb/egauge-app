import React, { useState, useEffect } from 'react';
import { clienteService } from '../../services/clienteService';
import { calculadoraService } from '../../services/calculadoraService';

const CalculadoraCFE = () => {
  const [clientes, setClientes] = useState([]);
  const [clienteSeleccionado, setClienteSeleccionado] = useState('');
  const [columnas, setColumnas] = useState([]);
  const [columnaSeleccionada, setColumnaSeleccionada] = useState('');
  const [fechaInicio, setFechaInicio] = useState('');
  const [fechaFin, setFechaFin] = useState('');
  const [precios, setPrecios] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [resultado, setResultado] = useState(null);

  // Cargar clientes y precios default al inicializar
  useEffect(() => {
    cargarDatosIniciales();
    configurarFechasDefault();
  }, []);

  const cargarDatosIniciales = async () => {
    try {
      const [clientesData, preciosData] = await Promise.all([
        clienteService.obtenerActivos(),
        calculadoraService.obtenerPreciosDefault()
      ]);
      
      setClientes(clientesData);
      setPrecios(preciosData);
    } catch (err) {
      setError('Error cargando datos iniciales');
    }
  };

  const configurarFechasDefault = () => {
    const hoy = new Date();
    const primerDiaMes = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
    
    setFechaInicio(primerDiaMes.toISOString().slice(0, 16));
    setFechaFin(hoy.toISOString().slice(0, 16));
  };

  // Cargar columnas cuando se selecciona un cliente
  const handleClienteChange = async (clienteId) => {
    setClienteSeleccionado(clienteId);
    setColumnaSeleccionada('');
    setColumnas([]);
    
    if (clienteId) {
      try {
        const columnasData = await calculadoraService.obtenerColumnas(clienteId);
        setColumnas(columnasData.columnas || []);
        if (columnasData.columnas && columnasData.columnas.length > 0) {
          setColumnaSeleccionada(columnasData.columnas[0]);
        }
      } catch (err) {
        setError('Error cargando columnas del cliente');
      }
    }
  };

  const handleCalcular = async () => {
    if (!clienteSeleccionado) {
      setError('Selecciona un cliente');
      return;
    }

    if (!fechaInicio || !fechaFin) {
      setError('Selecciona fechas de inicio y fin');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      setResultado(null);

      const request = {
        clienteId: parseInt(clienteSeleccionado),
        fechaInicio: fechaInicio,
        fechaFin: fechaFin,
        columnaSensor: columnaSeleccionada,
        precios: precios
      };

      const data = await calculadoraService.calcular(request);
      setResultado(data);

    } catch (err) {
      setError(err.response?.data?.error || 'Error en el cálculo');
    } finally {
      setLoading(false);
    }
  };

  const handlePrecioChange = (campo, valor) => {
    setPrecios(prev => ({
      ...prev,
      [campo]: parseFloat(valor) || 0
    }));
  };

  const clienteInfo = clientes.find(c => c.id === parseInt(clienteSeleccionado));

  return (
    <div className="calculadora-cfe">
      <div className="form-header">
        <h2>🧾 Calculadora CFE</h2>
        <p>Calcula costos eléctricos basados en datos descargados</p>
      </div>

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      <div className="calculadora-form">
        
        {/* Selector de Cliente */}
        <div className="form-group">
          <label htmlFor="cliente">Cliente *</label>
          <select
            id="cliente"
            value={clienteSeleccionado}
            onChange={(e) => handleClienteChange(e.target.value)}
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

        {/* Información del Cliente */}
        {clienteInfo && (
          <div className="cliente-info">
            <h4>📋 Cliente: {clienteInfo.nombreCliente}</h4>
            <p><strong>Tabla de datos:</strong> {clienteInfo.tablaNombre}</p>
          </div>
        )}

        {/* Selector de Columna/Sensor */}
        {columnas.length > 0 && (
          <div className="form-group">
            <label htmlFor="columna">Sensor a usar para cálculos *</label>
            <select
              id="columna"
              value={columnaSeleccionada}
              onChange={(e) => setColumnaSeleccionada(e.target.value)}
              disabled={loading}
            >
              {columnas.map(columna => (
                <option key={columna} value={columna}>
                  {columna}
                </option>
              ))}
            </select>
            <small>Sensor que se usará para calcular consumos y demandas</small>
          </div>
        )}

        {/* Rango de Fechas */}
        <div className="form-row">
          <div className="form-group">
            <label htmlFor="fechaInicio">Fecha Inicio *</label>
            <input
              type="datetime-local"
              id="fechaInicio"
              value={fechaInicio}
              onChange={(e) => setFechaInicio(e.target.value)}
              disabled={loading}
            />
          </div>
          <div className="form-group">
            <label htmlFor="fechaFin">Fecha Fin *</label>
            <input
              type="datetime-local"
              id="fechaFin"
              value={fechaFin}
              onChange={(e) => setFechaFin(e.target.value)}
              disabled={loading}
            />
          </div>
        </div>

        {/* Configuración de Precios */}
        {precios && (
          <div className="precios-section">
            <h4>💰 Configuración de Precios CFE</h4>
            
            <div className="precios-grid">
              <div className="form-group">
                <label>Precio Base ($/kWh)</label>
                <input
                  type="number"
                  step="0.01"
                  value={precios.precioBase}
                  onChange={(e) => handlePrecioChange('precioBase', e.target.value)}
                  disabled={loading}
                />
              </div>
              
              <div className="form-group">
                <label>Precio Intermedio ($/kWh)</label>
                <input
                  type="number"
                  step="0.01"
                  value={precios.precioIntermedio}
                  onChange={(e) => handlePrecioChange('precioIntermedio', e.target.value)}
                  disabled={loading}
                />
              </div>
              
              <div className="form-group">
                <label>Precio Punta ($/kWh)</label>
                <input
                  type="number"
                  step="0.01"
                  value={precios.precioPunta}
                  onChange={(e) => handlePrecioChange('precioPunta', e.target.value)}
                  disabled={loading}
                />
              </div>
              
              <div className="form-group">
                <label>Precio Capacidad ($/kW)</label>
                <input
                  type="number"
                  step="0.01"
                  value={precios.precioCapacidad}
                  onChange={(e) => handlePrecioChange('precioCapacidad', e.target.value)}
                  disabled={loading}
                />
              </div>
              
              <div className="form-group">
                <label>Precio Distribución ($/kW)</label>
                <input
                  type="number"
                  step="0.01"
                  value={precios.precioDistribucion}
                  onChange={(e) => handlePrecioChange('precioDistribucion', e.target.value)}
                  disabled={loading}
                />
              </div>
              
              <div className="form-group">
                <label>Cargo Fijo ($)</label>
                <input
                  type="number"
                  step="0.01"
                  value={precios.cargoFijo}
                  onChange={(e) => handlePrecioChange('cargoFijo', e.target.value)}
                  disabled={loading}
                />
              </div>
            </div>

            {/* DAP (Servicio de Alumbrado Público) */}
            <div className="dap-section">
              <div className="form-group">
                <label>
                  <input
                    type="checkbox"
                    checked={precios.incluirDap}
                    onChange={(e) => handlePrecioChange('incluirDap', e.target.checked)}
                    disabled={loading}
                  />
                  Incluir Servicio de Alumbrado Público (DAP)
                </label>
              </div>
              
              {precios.incluirDap && (
                <div className="form-group">
                  <label>Porcentaje DAP (%)</label>
                  <input
                    type="number"
                    step="0.1"
                    min="0"
                    max="10"
                    value={precios.porcentajeDap}
                    onChange={(e) => handlePrecioChange('porcentajeDap', e.target.value)}
                    disabled={loading}
                  />
                </div>
              )}
            </div>
          </div>
        )}

        {/* Botón Calcular */}
        <div className="form-actions">
          <button 
            onClick={handleCalcular}
            className="btn-primary"
            disabled={loading || !clienteSeleccionado}
          >
            {loading ? '🔄 Calculando...' : '🧮 Calcular Costos CFE'}
          </button>
        </div>
      </div>

      {/* Resultados */}
      {resultado && (
        <div className="resultado-cfe">
          <h3>📊 Resultados del Cálculo</h3>
          
          {/* Resumen Principal */}
          <div className="resumen-principal">
            <div className="total-final">
              <span>💰 TOTAL A PAGAR:</span>
              <span className="monto">${resultado.total.toLocaleString()}</span>
            </div>
            <div className="periodo-info">
              <span>📅 Período: {resultado.diasPeriodo} días</span>
              <span>🔌 Sensor: {resultado.columnaSensor}</span>
            </div>
          </div>

          {/* Consumos y Demandas */}
          <div className="datos-tecnicos">
            <h4>⚡ Consumos y Demandas</h4>
            <div className="tabla-datos">
              <table>
                <thead>
                  <tr>
                    <th>Tarifa</th>
                    <th>Consumo (kWh)</th>
                    <th>Demanda Máx (kW)</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>Base</td>
                    <td>{resultado.kwhBase.toLocaleString()}</td>
                    <td>{resultado.maxBase.toLocaleString()}</td>
                  </tr>
                  <tr>
                    <td>Intermedio</td>
                    <td>{resultado.kwhIntermedio.toLocaleString()}</td>
                    <td>{resultado.maxIntermedio.toLocaleString()}</td>
                  </tr>
                  <tr>
                    <td>Punta</td>
                    <td>{resultado.kwhPunta.toLocaleString()}</td>
                    <td>{resultado.maxPunta.toLocaleString()}</td>
                  </tr>
                  <tr className="total-row">
                    <td><strong>TOTAL</strong></td>
                    <td><strong>{(resultado.kwhBase + resultado.kwhIntermedio + resultado.kwhPunta).toLocaleString()}</strong></td>
                    <td><strong>{resultado.demandaFacturable.toLocaleString()}</strong></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          {/* Desglose de Costos */}
          <div className="desglose-costos">
            <h4>💸 Desglose de Costos</h4>
            <div className="costos-lista">
              <div className="costo-item">
                <span>Energía Base:</span>
                <span>${resultado.costoBase.toLocaleString()}</span>
              </div>
              <div className="costo-item">
                <span>Energía Intermedia:</span>
                <span>${resultado.costoIntermedio.toLocaleString()}</span>
              </div>
              <div className="costo-item">
                <span>Energía Punta:</span>
                <span>${resultado.costoPunta.toLocaleString()}</span>
              </div>
              <div className="costo-item">
                <span>Capacidad:</span>
                <span>${resultado.costoCapacidad.toLocaleString()}</span>
              </div>
              <div className="costo-item">
                <span>Distribución:</span>
                <span>${resultado.costoDistribucion.toLocaleString()}</span>
              </div>
              <div className="costo-item subtotal">
                <span><strong>Energía Total:</strong></span>
                <span><strong>${resultado.energia.toLocaleString()}</strong></span>
              </div>
              <div className="costo-item">
                <span>Cargo Fijo:</span>
                <span>${resultado.cargoFijo.toLocaleString()}</span>
              </div>
              <div className="costo-item subtotal">
                <span><strong>Subtotal:</strong></span>
                <span><strong>${resultado.subtotal.toLocaleString()}</strong></span>
              </div>
              {resultado.dap > 0 && (
                <div className="costo-item">
                  <span>DAP (Alumbrado Público):</span>
                  <span>${resultado.dap.toLocaleString()}</span>
                </div>
              )}
              <div className="costo-item">
                <span>IVA (16%):</span>
                <span>${resultado.iva.toLocaleString()}</span>
              </div>
              <div className="costo-item total">
                <span><strong>TOTAL FINAL:</strong></span>
                <span><strong>${resultado.total.toLocaleString()}</strong></span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default CalculadoraCFE;