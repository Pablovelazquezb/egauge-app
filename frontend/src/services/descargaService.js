import API from '../utils/api';

export const descargaService = {
  // Descargar datos para un cliente
  descargarDatos: async (clienteId, fechaInicio, fechaFin) => {
    const response = await API.post(`/descarga/cliente/${clienteId}`, {
      fechaInicio: fechaInicio,
      fechaFin: fechaFin
    });
    return response.data;
  },

  // Obtener estadísticas de un cliente
  obtenerEstadisticas: async (clienteId) => {
    const response = await API.get(`/descarga/cliente/${clienteId}/stats`);
    return response.data;
  },

  // Obtener último timestamp descargado
  obtenerUltimoTimestamp: async (clienteId) => {
    const response = await API.get(`/descarga/cliente/${clienteId}/ultimo-timestamp`);
    return response.data;
  }
};