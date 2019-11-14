LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := android-common \
                               android-support-v4 \
                               android-support-v7-recyclerview
							   
LOCAL_STATIC_JAVA_AAR_LIBRARIES := aarfile

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res \
    frameworks/support/v7/recyclerview/res

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_SRC_FILES += \
	src/com/android/gphonemanager/applock/IApplockService.aidl \
	src/com/android/gphonemanager/IGPhoneManagerService.aidl
	
LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages fr.castorflex.android.circularprogressbar \
    --extra-packages android.support.v7.recyclerview

LOCAL_PACKAGE_NAME := GPhoneManager
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

include frameworks/base/packages/SettingsLib/common.mk

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := aarfile:libs/library-circular-release.aar
include $(BUILD_MULTI_PREBUILT)

#include $(CLEAR_VARS)
#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES : = \
#	libprogress:libs/com.github.castorflex.smoothprogressbar/library-circular/1.0.0/jars/classes.jar
#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libprogress:libs/smoothProgress.jar
#include $(BUILD_MULTI_PREBUILT)
