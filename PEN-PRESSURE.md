# Focus Pen Pro pressure bridge

Adapted from YAAP Pad 8 PenReports. Reads only the hidraw device whose Bluetooth HID identity is 0005:0022:5081, accepts only 15-byte report 5, and forwards unsigned bytes through stock TouchFeature setModeLongValue (transaction 8), mode 1088. Existing AIDL client numbering is intentionally unchanged.

The worker uses nonblocking read and 250 ms polling, bounded discovery/reopen retries, and cancellation on input disconnect/controller shutdown. Reader instances own their descriptors and stale readers cannot schedule replacement workers. HAL calls share the existing synchronized client. No firmware or refresh-rate changes.

XiaomiStylus shares system_app, so the optional policy grants read/open/getattr on hidraw_device to that shared domain, not solely to this package. It grants no write/ioctl access. HID identity filtering happens in the app. A dedicated package/domain would be required for package-isolated policy.

Compatibility remains unproven: source P81 set_mode_long_value explicitly implements mode 15, not mode 1088. The stock HAL/firmware may use another dispatch path. A successful Binder reply alone does not establish pressure support. Verify HID discovery/permissions, mode return, disconnect/reconnect, hover/contact pressure, pen keys/laser and other pen models on piano. The kernel HID filtering patch is separate and has not been imported by this change.

No device installation or runtime validation performed for this feature.
