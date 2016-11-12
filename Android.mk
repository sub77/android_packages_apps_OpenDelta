LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := OpenDelta
LOCAL_MODULE_TAGS := optional
LOCAL_AAPT_FLAGS += --rename-manifest-package com.dirtyunicorns.duupdater2
LOCAL_PRIVILEGED_MODULE := true

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JNI_SHARED_LIBRARIES := libopendelta
LOCAL_REQUIRED_MODULES := libopendelta
LOCAL_PROGUARD_FLAG_FILES := proguard-project.txt

#Include res dir from libraries
cardview_dir := ../../../$(SUPPORT_LIBRARY_ROOT)/v7/cardview/res
#design_dir := ../../../$(SUPPORT_LIBRARY_ROOT)/design/res
#appcompat_dir := ../../../$(SUPPORT_LIBRARY_ROOT)/v7/appcompat/res
#recyclerview_dir := ../../../$(SUPPORT_LIBRARY_ROOT)/v7/recyclerview/res
#design_dir := ../../../$(SUPPORT_LIBRARY_ROOT)/design/res

res_dirs := res $(cardview_dir)
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-cardview
#$(appcompat_dir) $(recyclerview_dir) $(design_dir)
#LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4
#LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-appcompat
#LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-recyclerview
#LOCAL_STATIC_JAVA_LIBRARIES += android-support-v13
#LOCAL_STATIC_JAVA_LIBRARIES += android-support-design

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))
LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.cardview

#--extra-packages android.support.v7.appcompat:android.support.v7.cardview:android.support.v7.recyclerview:android.support.design

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
