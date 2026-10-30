# Xiaomi Parts for the Xiaomi Pad 8 Pro (piano)

| | |
| --- | --- |
| Path | `device/xiaomi/piano-xiaomi-parts` |
| Optional | yes |

| App | What |
| --- | --- |
| `XiaomiStylus` | Focus Pen: pairing, battery, pressure, buttons, laser, firmware |
| `XiaomiKeyboard` | Keyboard cover: shortcuts, firmware, touchpad |
| `XiaomiBypass` | Bypass charging tile |
| `XiaomiDisplay` | Reading, sunlight, True Tone, color |
| `PianoPencilEngine` | Pen engine for Xiaomi Notes |

ROM changes they need are in `rom-patches/`, applied by `vendorsetup.sh`.

```sh
bash tests/run-mipp-tests.sh && bash tests/run-mipp-v2-tests.sh && bash tests/run-keyboard-input-tests.sh
```
