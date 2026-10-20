# PianoParts with the Settings entry placed for the ROM being built.
PRODUCT_SOONG_NAMESPACES += device/xiaomi/piano-parts
ifneq ($(wildcard vendor/evolution),)
PRODUCT_PACKAGES += PianoPartsEvox
else
PRODUCT_PACKAGES += PianoParts
endif
# The capture bridge handles Android 16 and 17 platform API revisions.
ifneq ($(filter 36 37,$(PLATFORM_SDK_VERSION)),)
PRODUCT_PACKAGES += PianoPencilEngine
endif
PRODUCT_COPY_FILES += \
    device/xiaomi/piano-parts/privapp-permissions-pianoparts.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/privapp-permissions-pianoparts.xml

# ALXP Features, when device/xiaomi/piano-alxp-parts is synced.
$(call inherit-product-if-exists, device/xiaomi/piano-alxp-parts/product.mk)
