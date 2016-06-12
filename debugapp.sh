#/bin/sh

DBGAPKPATH="build/outputs/apk/OpenDelta_RS-debug.apk"
ADBARGS=""
CONTINUEXEC=true
COMPILEAPP=false
APPK="com.dirtyunicorns.duupdater"

if [ "$1" == "--adbArgs" ]; then
  ADBARGS="$2 $3 $4 $5 $6"
  COMPILEAPP=true
fi

if [ -z "$1" ]; then source buildDebugApp.sh
elif [ COMPILEAPP ]; then source buildDebugApp.sh; fi

echo "Requesting root..."
adb root

if [ "$1" == "-l" ]; then
  adb logcat -v tag -s OpenDelta:*
  CONTINUEXEC=false
elif [ "$1" == "--cleardata" ]; then
  adb shell pm clear $APPK
  CONTINUEXEC=false
elif [ "$1" == "-i" ]; then
  adb push $DBGAPKPATH /sdcard/odr.apk
  adb shell pm set-install-location 1
  adb shell pm install -rdtf /sdcard/odr.apk
  CONTINUEXEC=false
elif [ "$1" == "--start" ]; then
  adb shell am start -n $APPK/.MainActivity
  CONTINUEXEC=false
elif [ "$1" == "--uninstall" ]; then
  adb shell pm uninstall $APPK
  CONTINUEXEC=false
elif [ "$1" == "--reinstall" ]; then
  adb shell pm uninstall $APPK
  adb push $DBGAPKPATH /sdcard/odr.apk
  adb shell pm set-install-location 1
  adb shell pm install -rdtf /sdcard/odr.apk
  CONTINUEXEC=false
fi

if [ CONTINUEXEC ]; then
  adb $ADBARGS push $DBGAPKPATH /sdcard/odr.apk
  adb $ADBARGS root>/dev/null
  adb $ADBARGS wait-for-device
  adb $ADBARGS shell pm set-install-location 1
  adb $ADBARGS shell pm install -rdtf /sdcard/odr.apk
  adb $ADBARGS shell am start -n $APPK/.MainActivity
  if [ "$1" == "--grp" ]; then adb $ADBARGS logcat -v tag -s OpenDelta:* | grep $2
  else adb $ADBARGS logcat -v tag -s OpenDelta:*
  fi
fi
