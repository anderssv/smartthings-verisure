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
definition(
        name: "Verisure",
        namespace: "smartthings.f12.no",
        author: "Anders Sveen",
        description: "Yes",
        category: "Safety & Security",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
}


preferences {
    page(name: "setupPage")
}


def setupPage() {
    dynamicPage(name: "setupPage", title: "Configure Verisure", install: true, uninstall: true) {

        section("Authentication") {
            input "username", "text", title: "Username"
            input "password", "password", title: "Password"
        }

        def actions = location.helloHome?.getPhrases()*.label
        actions.sort()

        section("Action when disarmed") {
            input "unarmedAction", "enum", title: "Action for unarmed", options: actions, required: false
            input "armedAction", "enum", title: "Action for armed", options: actions, required: false
            input "armedHomeAction", "enum", title: "Action for armed home", options: actions, required: false
        }
        
        section("Alarm polling") {
            input "pollinterval", "number", title: "Poll interval (seconds, minimum 15)", range: "15..*", defaultValue: 60, required: true
        }
    }
}

def installed() {
    log.debug "Verisure Installed"
    addChildDevice(app.namespace, "Verisure Alarm", "verisure-alarm", null, [alarmstate: "unknown"])

    initialize()
}

def updated() {
    log.debug "Verisure Alarm Updated"

    unsubscribe()
    initialize()
}

def uninstalled() {
    log.debug("Uninstalling Verisure Alarm app, removing all devices")
    removeChildDevices(getChildDevices())
}

def initialize() {
    log.debug("Scheduling Verisure Alarm updates...")
    //schedule("? 0/30 * * * ?", schedulePollUpdate)
    poll()
}

def getAlarmState() {
	log.debug("Retrieving cached alarm state")
	return state.previousAlarmState
}

def poll() {
    def baseUrl = "https://mypages.verisure.com"
    def loginUrl = baseUrl + "/j_spring_security_check?locale=en_GB"
	
    def alarmState = null

	try {
	    def sessionCookie = login(loginUrl)
		alarmState = getAlarmState(baseUrl, sessionCookie)

    	if (state.previousAlarmState == null) {
        	state.previousAlarmState = alarmState
    	}

		getChildDevices().each { device ->
        	device.sendEvent(name: "alarmstate", value: alarmState)
    	}
    	
        if (alarmState != state.previousAlarmState) {
        	log.debug("Verisure Alarm state changed, execution actions")
        	state.previousAlarmState = alarmState
        	triggerActions(alarmState)
    	}

	    log.debug("Verisure Alarm state updated and is: " + alarmState)
    } catch (Exception e) {
    	log.error("Error updating alarm state", e)
    }

	schedulePollUpdate()
    return alarmState
}

def schedulePollUpdate() {
    runIn(pollinterval, poll)
}

def triggerActions(alarmState) {
    if (alarmState == "armed" && armedAction) {
        executeAction(armedAction)
    } else if (alarmState == "unarmed" && unarmedAction) {
        executeAction(unarmedAction)
    } else if (alarmState == "armedhome" && armedHomeAction) {
        executeAction(armedHomeAction)
    }
}

def executeAction(action) {
    log.debug("Executing action ${action}")
    location.helloHome?.execute(action)
}


def login(loginUrl) {
    def params = [
            uri               : loginUrl,
            requestContentType: "application/x-www-form-urlencoded",
            body              : [
                    j_username: username,
                    j_password: password
            ]
    ]

    httpPost(params) { response ->
        if (response.status != 200) {
            throw new IllegalStateException("Could not authenticate. Got response code ${response.status} . Is the username and password correct?")
        }

        def cookieHeader = response.headers.'Set-Cookie'
        if (cookieHeader == null) {
            throw new RuntimeException("Could not get session cookie! ${response.status} - ${response.data}")
        }

        return cookieHeader.split(";")[0]
    }
}

def getAlarmState(baseUrl, sessionCookie) {
    def alarmParams = [
            uri    : baseUrl + "/remotecontrol",
            headers: [
                    Cookie: sessionCookie
            ]
    ]

    return httpGet(alarmParams) { response ->
        log.debug("Response from Verisure: " + response.data)
        return response.data.findAll { it."type" == "ARM_STATE" }[0]."status"
    }
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}