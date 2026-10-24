# The Xiaomi apps: pen, keyboard cover, bypass charging, and the display
# features PianoParts still holds.
PRODUCT_SOONG_NAMESPACES += device/xiaomi/piano-xiaomi-parts
PRODUCT_PACKAGES += \
    PianoParts \
    XiaomiBypass \
    XiaomiKeyboard \
    XiaomiStylus
# The capture bridge handles Android 16 and 17 platform API revisions.
ifneq ($(filter 36 37,$(PLATFORM_SDK_VERSION)),)
PRODUCT_PACKAGES += PianoPencilEngine
endif
PRODUCT_COPY_FILES += \
    device/xiaomi/piano-xiaomi-parts/privapp-permissions-pianoparts.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/privapp-permissions-pianoparts.xml

# Read-only HID access for the optional pen pressure bridge.
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += device/xiaomi/piano-xiaomi-parts/sepolicy/private
