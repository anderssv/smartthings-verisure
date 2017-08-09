# Verisure integration

- [SmartApp](smartapps/smartthings-f12-no/verisure.src/verisure.groovy)
- [Alarm Device Handler](devicetypes/smartthings-f12-no/verisure-alarm.src/verisure-alarm.groovy)
- [Sensor Device Handler](devicetypes/smartthings-f12-no/verisure-sensor.src/verisure-sensor.groovy)

This SmartApp polls the Verisure alarm at given intervals to update it's state. Default is every 60 seconds, and minimum
at 15 seconds.

You'll also need to install the custom device handler to use this smartapp.

NOTE: If the app fails on installation the error is probably in your username/password. Make sure they are correct
and try again.

## Verisure Alarm
- Displays Alarm State
- Displays State Change Timestamp
- Displays State Change By

## Verisure Sensor
- Displays Temperature
- Displays Humidity
- Displays Type
- Displays Timestamp

# Known Issues
Together with webcore, temperature and humidity data does not behave as expected. 25,6 turns in to 256 for example not sure how to fix yet. Otherwise it works.