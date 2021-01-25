/**
 *  Verisure
 *
 *  Copyright 2021 Igor Paunov
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  CHANGE LOG
 *  - 0.1   - Initial release
 *
 * Version: 0.1
 *
 */
metadata {
    definition(
            name: "Verisure Contact Sensor",
            author: "Igor Paunov",
            namespace: "smartthings.f12.no") {
        capability "Contact sensor"
        attribute "timestamp", "string"
    }

    simulator {}

    tiles {

        standardTile("contact", "device.contact", width: 2, height: 2, canChangeBackground: true, canChangeIcon: true) {
            state "closed", label: 'Closed', backgroundColor: "#6cd18e", icon: "st.contact.contact.closed"
            state "open", label: 'Open', backgroundColor: "#c36cd1", icon: "st.contact.contact.open"
        }

        valueTile("timestamp", "device.timestamp", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "timestamp", label: '${currentValue}% timestamp', unit: ""
        }

        main("contact")
        details(["contact", "timestamp"])
    }
}

def parse(String description) {
    log.debug("[doorWindowDevice.parse] ")
    log.debug("[doorWindowDevice.contact]" + device.currentValue("contact"))
    log.debug("[doorWindowDevice.timestamp]" + device.currentValue("timestamp"))

    def evnt01 = createEvent(name: "contact", value: device.currentValue("contact"))
    def evnt02 = createEvent(name: "timestamp", value: device.currentValue("timestamp"))

    return [evnt01, evnt02]
}