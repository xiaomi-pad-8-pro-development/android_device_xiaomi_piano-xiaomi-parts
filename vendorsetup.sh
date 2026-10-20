#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

# Apply the ROM changes Xiaomi Parts needs whenever build/envsetup.sh is sourced.
(cd "$(gettop)" && bash device/xiaomi/piano-xiaomi-parts/rom-patches/apply-patches.sh \
    piano-xiaomi-parts device/xiaomi/piano-xiaomi-parts/rom-patches) ||
    echo "piano-xiaomi-parts: applying ROM changes failed, see the errors above" >&2
