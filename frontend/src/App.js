import React, { useState } from 'react';
import ClientesPage from './pages/clientesPage';
import DescargaPage from './pages/DescargaPage';
import CalculadoraPage from './pages/CalculadoraPage';
import './App.css';

function App() {
  const [currentPage, setCurrentPage] = useState('clientes');

  const renderPage = () => {
    switch(currentPage) {
      case 'clientes':
        return <ClientesPage />;
      case 'descarga':
        return <DescargaPage />;
      case 'calculadora':
        return <CalculadoraPage />;
      default:
        return <ClientesPage />;
    }
  };

  return (
    <div className="App">
      <header className="app-header">
        <h1>⚡ eGauge Data Manager</h1>
        <p>Sistema completo de gestión de medidores eGauge</p>
      </header>
      
      <nav className="app-nav">
        <button 
          className={`nav-btn ${currentPage === 'clientes' ? 'active' : ''}`}
          onClick={() => setCurrentPage('clientes')}
        >
          👥 Clientes
        </button>
        <button 
          className={`nav-btn ${currentPage === 'descarga' ? 'active' : ''}`}
          onClick={() => setCurrentPage('descarga')}
        >
          📥 Descarga
        </button>
        <button 
          className={`nav-btn ${currentPage === 'calculadora' ? 'active' : ''}`}
          onClick={() => setCurrentPage('calculadora')}
        >
          🧾 Calculadora
        </button>
      </nav>
      
      <main className="app-main">
        {renderPage()}
      </main>
    </div>
  );
}

export default App;