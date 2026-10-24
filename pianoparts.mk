# The Xiaomi apps: display features, pen, keyboard cover and bypass charging.
PRODUCT_SOONG_NAMESPACES += device/xiaomi/piano-xiaomi-parts
PRODUCT_PACKAGES += \
    XiaomiBypass \
    XiaomiDisplay \
    XiaomiKeyboard \
    XiaomiStylus
# The capture bridge handles Android 16 and 17 platform API revisions.
ifneq ($(filter 36 37,$(PLATFORM_SDK_VERSION)),)
PRODUCT_PACKAGES += PianoPencilEngine
endif

# Read-only HID access for the optional pen pressure bridge.
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += device/xiaomi/piano-xiaomi-parts/sepolicy/private
