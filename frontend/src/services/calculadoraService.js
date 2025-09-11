import API from '../utils/api';

export const calculadoraService = {
  // Calcular costos CFE
  calcular: async (request) => {
    const response = await API.post('/calculadora/calcular', request);
    return response.data;
  },

  // Obtener columnas disponibles para un cliente
  obtenerColumnas: async (clienteId) => {
    const response = await API.get(`/calculadora/cliente/${clienteId}/columnas`);
    return response.data;
  },

  // Obtener precios CFE por defecto
  obtenerPreciosDefault: async () => {
    const response = await API.get('/calculadora/precios-default');
    return response.data;
  }
};