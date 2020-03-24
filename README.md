# ble-andro-ardu
For experiments with Bluetooth BLE, Android and Arduino

This git provide a hack to use BLE to get the values from the proximity, rgbc, temperature, humidity and barometer sensors of an Arduino Nano 33 Sense BLE, to an Android phone.

The BleArdu folder contains the Android app. The code has been built OK with Android Studio version 3.6.1 on Ubuntu 19.10.

The arduino-sense folder contains the arduino-sense.ino file, this has build and flashed OK with Arduino IDE 1.8.12.   

The rpi contains a bash script to get the sensor data to a Linux device. I have tested this OK on a Raspberry Pi Zero W. Note that I have hardcoded the MAC adress on my Arduino Nano 33 Sense BLE device. 
