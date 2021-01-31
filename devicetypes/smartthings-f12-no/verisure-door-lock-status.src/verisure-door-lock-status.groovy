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
 * ------------------------------------------------------
 *  Only Status locked/inlocked is displayed in APP
 *  Some more attributed visible in DEV console
 * ------------------------------------------------------
 *
 *  CHANGE LOG
 *  - 0.1   - Initial release - Status only, no actions to lock/unlock
 *
 * Version: 0.1
 *
 */
metadata {
    definition(
            name: "Verisure Door Lock Status",
            author: "Igor Paunov",
            namespace: "smartthings.f12.no") {
        capability "Lock"
        attribute "rawLock", "string"
        attribute "method", "string"
        attribute "paired", "string"
        attribute "motorJam", "string"
        attribute "timestamp", "string"
    }

    simulator {}

    tiles {


        standardTile("lock", "device.lock", width: 2, height: 2, canChangeBackground: true, canChangeIcon: true) {
            state "locked", label: 'Locked', backgroundColor: "#6cd18e", icon: "st.locks.lock.locked"
            state "unknown", label: 'Unknown', backgroundColor: "#6cd18e", icon: "st.locks.lock.unknown"
            state "unlocked", label: 'Unlocked', backgroundColor: "#6cd18e", icon: "st.locks.lock.unlocked"
            state "unlocked with timeout", label: 'Unlocked Timeout', backgroundColor: "#6cd18e", icon: "st.locks.lock.unlocked"
        }

        valueTile("method", "device.method", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "method", label: '${currentValue}', unit: ""
        }
        valueTile("paired", "device.paired", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "paired", label: '${currentValue}', unit: ""
        }
        valueTile("motorJam", "device.motorJam", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "motorJam", label: '${currentValue}', unit: ""
        }
        valueTile("rawLock", "device.rawLock", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "rawLock", label: '${currentValue}', unit: ""
        }

        valueTile("timestamp", "device.timestamp", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "timestamp", label: '${currentValue}% timestamp', unit: ""
        }

        main(["lock", "method"])
        details(["lock", "method", "paired", "motorJam", "rawLock", "timestamp"])
    }
}

def parse(String description) {
    log.debug("[doorLockStatus.parse] ")

    def evnt01 = createEvent(name: "lock", value: device.currentValue("lock"))
    def evnt02 = createEvent(name: "method", value: device.currentValue("method"))
    def evnt03 = createEvent(name: "paired", value: device.currentValue("paired"))
    def evnt04 = createEvent(name: "motorJam", value: device.currentValue("motorJam"))
    def evnt05 = createEvent(name: "timestamp", value: device.currentValue("timestamp"))
    def evnt06 = createEvent(name: "rawLock", value: device.currentValue("rawLock"))

    return [evnt01, evnt02, evnt03, evnt04, evnt05, evnt06]
}
