#!/usr/bin/bash

# This script provisions the MDM client on a new board
# Arguments:
#   $1: The name of the board
#   $2: The IP of the board
#   $3: The path to the APK file

# Fail if any command fails
set -e

board_name=$1
board_ip=$2
apk_path=$3

# Check if any arguments are missing
if [ -z "$board_name" ] || [ -z "$board_ip" ] || [ -z "$apk_path" ]; then
    echo "Usage: $0 <board_name> <board_ip> <apk_path>"
    exit 1
fi

# Check if the APK file exists
if [ ! -f "$apk_path" ]; then
    echo "Error: APK file not found at $apk_path"
    exit 1
fi

adb connect "$board_ip" || {
    echo "Error: Unable to connect to $board_name at $board_ip"
    exit 1
}

# Write the board name to /storage/emulated/0/hmdm_device_id
adb -s "$board_ip" shell "printf '$board_name' > /storage/emulated/0/hmdm_device_id" || {
    echo "Error: Failed to write board name to device"
    exit 1
}

# Install the APK
adb -s "$board_ip" install -r "$apk_path" || {
    echo "Error: Failed to install the APK on $board_name"
    exit 1
}

# Grant device owner permission
adb -s "$board_ip" shell dpm set-device-owner com.hmdm.launcher/.AdminReceiver || {
    echo "Error: Failed to set device owner"
    exit 1
}

# Uninstall  com.android.ist.tvlauncher for user 0
adb -s "$board_ip" shell pm uninstall --user 0 com.android.ist.tvlauncher || {
    echo "Error: Failed to uninstall com.android.ist.tvlauncher"
    exit 1
}
