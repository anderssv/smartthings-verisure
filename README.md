# Verisure integration

This is an unofficial intergation. No guaranantees. :)

- [SmartApp](smartapps/smartthings-f12-no/verisure.src/verisure.groovy)
- [Alarm Device Handler](devicetypes/smartthings-f12-no/verisure-alarm.src/verisure-alarm.groovy)
- [Sensor Device Handler](devicetypes/smartthings-f12-no/verisure-sensor.src/verisure-sensor.groovy)

This SmartApp polls the server every minute.

# Installation

Install the smartapp AND the custom device handlers (Alarm and devices).

Watch your logs after install. If you entered the wrong username and/or password your account could get blocked if you
let the application try too many times!

# Features

- Execute a Routine for different states of Home, Armed and ArmedHome.

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

- If you have multiple locations in Veirsure, it will only pick the first one.
- Not all log entries are shown in the console. This might only occur if you have remote logging enabled.

# Support

- Register [issues here on Github](https://github.com/anderssv/smartthings-verisure/issues).
- Talk to us at [the thread in the forums](https://community.smartthings.com/t/release-verisure-integration/201617).