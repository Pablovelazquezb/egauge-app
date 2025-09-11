import React, { useState } from 'react';
import ClientesList from '../components/clientes/clientesList';
import ClienteForm from '../components/clientes/clienteForm';

const ClientesPage = () => {
  const [showForm, setShowForm] = useState(false);
  const [clienteEditar, setClienteEditar] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);

  // Mostrar formulario para nuevo cliente
  const handleToggleForm = () => {
    setClienteEditar(null);
    setShowForm(!showForm);
  };

  // Mostrar formulario para editar cliente
  const handleEditCliente = (cliente) => {
    setClienteEditar(cliente);
    setShowForm(true);
  };

  // Cerrar formulario
  const handleCloseForm = () => {
    setShowForm(false);
    setClienteEditar(null);
  };

  // Guardar cliente (crear o actualizar)
  const handleSaveCliente = () => {
    setShowForm(false);
    setClienteEditar(null);
    // Forzar recarga de la lista
    setRefreshKey(prev => prev + 1);
  };

  return (
    <div className="clientes-page">
      {/* Lista de clientes */}
      <ClientesList
        key={refreshKey} // Para forzar recarga
        onEdit={handleEditCliente}
        onToggleForm={handleToggleForm}
      />

      {/* Formulario modal */}
      {showForm && (
        <ClienteForm
          cliente={clienteEditar}
          onSave={handleSaveCliente}
          onCancel={handleCloseForm}
        />
      )}
    </div>
  );
};

export default ClientesPage;