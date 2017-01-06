/**
 *  Verisure
 *
 *  Copyright 2017 Anders Sveen
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
 */
metadata {
    definition(
            name: "Verisure Alarm",
            author: "Anders Sveen <anders@f12.no>",
            namespace: "smartthings.f12.no") {}

    simulator {}

    tiles {
        standardTile("alarmstate", "device.alarmstate", width: 2, height: 2, canChangeBackground: true, canChangeIcon: true) {
            state "armed", label: 'Armed', backgroundColor: "#79b821", icon: "st.Home.home3"
            state "unarmed", label: 'Unarmed', backgroundColor: "#ffcc00", icon: "st.Home.home2"
            state "armedhome", label: 'Armed Home', backgroundColor: "#79b821", icon: "st.Home.home2"
        }

        main "alarmstate"

        details(["alarmstate"])
    }
}

def poll() {
    def alarmstate = parent.poll()
    log.debug("Polled state for alarm: " + alarmstate)
    return createEvent(name: "alarmstate", value: alarmstate)
}

def parse(String description) {
    poll()
}

