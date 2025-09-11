import React from 'react';
import ClientesPage from './pages/clientesPage';
import './App.css';

function App() {
  return (
    <div className="App">
      <header className="app-header">
        <h1>⚡ eGauge Data Manager</h1>
        <p>Sistema de gestión de medidores eGauge</p>
      </header>
      
      <main className="app-main">
        <ClientesPage />
      </main>
    </div>
  );
}

export default App;