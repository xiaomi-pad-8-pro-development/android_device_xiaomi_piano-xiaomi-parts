# Xiaomi Parts for Xiaomi Pad 8 Pro (piano)

The settings app for the tablet's Xiaomi features. Android 17.

Goes in `device/xiaomi/piano-parts`. `pianoparts.mk` places the Xiaomi
settings entry for the ROM being built: Evolution X when `vendor/evolution`
exists, LineageOS otherwise. With
[ALXPParts](https://github.com/xiaomi-pad-8-pro-development/android_device_xiaomi_piano-alxp-parts)
synced to `device/xiaomi/piano-alxp-parts`, it is included too.

## Features

- Xiaomi Pen: pairing, battery, pressure, button shortcuts, color picker
- Xiaomi keyboard: pairing and shortcuts
- Tap to wake (off, single tap, double tap)
- Reading mode, including rhythmic (time of day) mode
- Display and refresh rate settings
- PencilEngine service used by Xiaomi Notes for pen drawing modes

## Tests

```sh
bash tests/run-mipp-tests.sh
python3 tests/check-devauth-policy.py
```

These check the code on a computer. Pen and keyboard still need testing on
the tablet.
