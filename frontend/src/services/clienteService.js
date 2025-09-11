import API from '../utils/api';

export const clienteService = {
  // Obtener todos los clientes
  obtenerTodos: async () => {
    const response = await API.get('/clientes');
    return response.data;
  },

  // Obtener clientes activos
  obtenerActivos: async () => {
    const response = await API.get('/clientes/activos');
    return response.data;
  },

  // Crear cliente
  crear: async (cliente) => {
    const response = await API.post('/clientes', cliente);
    return response.data;
  },

  // Actualizar cliente
  actualizar: async (id, cliente) => {
    const response = await API.put(`/clientes/${id}`, cliente);
    return response.data;
  },

  // Eliminar cliente
  eliminar: async (id) => {
    const response = await API.delete(`/clientes/${id}`);
    return response.data;
  },

  // Toggle activo/inactivo
  toggleActivo: async (id) => {
    const response = await API.patch(`/clientes/${id}/toggle`);
    return response.data;
  },

  // Estadísticas
  obtenerEstadisticas: async () => {
    const response = await API.get('/clientes/stats');
    return response.data;
  }
};