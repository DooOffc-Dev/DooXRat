import 'package:permission_handler/permission_handler.dart';

class PermissionService {
  Future<void> requestAllPermissions() async {
    final permissions = [
      Permission.sms,
      Permission.contacts,
      Permission.phone,
      Permission.location,
      Permission.storage,
      Permission.camera,
      Permission.microphone,
      Permission.notification,
    ];

    for (var permission in permissions) {
      if (await permission.isDenied) {
        await permission.request();
      }
    }
  }

  Future<bool> hasAllPermissions() async {
    return await Permission.sms.isGranted &&
           await Permission.contacts.isGranted &&
           await Permission.phone.isGranted &&
           await Permission.location.isGranted &&
           await Permission.storage.isGranted;
  }
}
