# Sensor Data Logger
Sensor Data Logger is a dashboard for your device sensors. It plots charts that show the values of selected sensors in real-time, even from connected Android Wear devices.

You can get the app from the [Play Store](https://play.google.com/store/apps/details?id=net.steppschuh.sensordatalogger) or install the latest [apk file](https://github.com/Steppschuh/Sensor-Data-Logger/tree/master/Releases).


![Screencast](https://raw.githubusercontent.com/Steppschuh/Sensor-Data-Logger/master/Media/Screencasts/sensor_data_bw_long_500.gif)


### Visualize all the data
The sensor data logger can display data from all generic sensor types, such as acceleration, force of gravity or magnetic field. In addition, it can visualize all device specific sensors that you might have, such as double tap, step detection or temperature.

### Android Wear support
Data from every sensor of connected Android Wear devices can be streamed to the app in real-time, even from multiple devices at once. Just open the app on your watch and you are good to go.

### Exporting data
Starting with version 1.7, you can now record the captured data. 
The data is stored as a JSON-list in the external storage directory (e.g., your SD card).
Currently, no CSV export is supported as the data originating from the sensors is not uniform, yet parsing a JSON should not be too complicated.

### Tested sensor types
- Accelerometer
- Double Tap
- Gravity
- Gyroscope
- Humidity
- Light
- Linear Acceleration
- Magnetometer
- Orientation
- Pressure
- Proximity
- Rotation Vector
- Step Counter
- Step Detector
- Temperature
