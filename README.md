# Xiaomi Parts for the Xiaomi Pad 8 Pro (piano)

The apps for the tablet's Xiaomi accessories and features. Android 17.

Goes in `device/xiaomi/piano-xiaomi-parts`. `pianoparts.mk` adds the apps
below; the device tree builds without this repository.

| App | What it does |
|---|---|
| `XiaomiStylus` | Focus Pen: pairing, battery, pressure, button shortcuts, laser pointer, palette, firmware, 120 Hz while in use (Settings > Connected devices) |
| `XiaomiKeyboard` | Keyboard cover: settings, shortcut keys, keyboard and MCU firmware, touchpad scrolling (Settings > Connected devices) |
| `XiaomiBypass` | Bypass charging Quick Settings tile |
| `PianoParts` | Reading mode, sunlight mode, True Tone, color scheme, touch panel rotation |
| `PianoPencilEngine` | PencilEngine service used by Xiaomi Notes for pen drawing modes |

The ROM changes the features need (stylus laser pointer and palette, keyboard
cover layout, touchpad scrolling, the Settings and LineageParts entries these
apps replace, the Quick Settings category) are in `rom-patches/`.
`vendorsetup.sh` applies them when `build/envsetup.sh` is sourced, so a build
without this repository keeps the stock ROM behaviour.

## Tests

```sh
bash tests/run-mipp-tests.sh
bash tests/run-mipp-v2-tests.sh
bash tests/run-keyboard-input-tests.sh
python3 tests/check-devauth-policy.py
```

These check the code on a computer. Pen and keyboard still need testing on
the tablet.
