# Verisure integration

- [SmartApp](smartapps/smartthings-f12-no/verisure.src/verisure.groovy)
- [Alarm Device Handler](devicetypes/smartthings-f12-no/verisure-alarm.src/verisure-alarm.groovy)

This SmartApp polls the Verisure alarm at given intervals to update it's state. Default is every 60 seconds, and minimum
at 15 seconds.

You'll also need to install the custom device handler to use this smartapp.

NOTE: If the app fails on installation the error is probably in your username/password. Make sure they are correct
and try again.