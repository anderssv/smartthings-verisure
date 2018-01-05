/**
 *  Verisure
 *
 *  Copyright 2017 Anders Sveen & Martin Carlsson
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
 *  - 0.1   - Added option for Splunk logging
 *  - 0.1.1 - Removed option to set update frequency. Need to use cron to have realiable updates, and it won't poll faster than each minute.
 *  - 0.2   - Added support to grab climate data.
 *  - 0.2.1 - Fixed issue with parsing numbers for climate data.
 *  - 0.2.2 - Cleaned up some code
 *  - 0.2.3 - Added enable/disable option
 *
 * Version: 0.2.3
 *
 */
definition(
        name: "Verisure",
        namespace: "smartthings.f12.no",
        author: "Anders Sveen & Martin Carlsson",
        description: "Lets you trigger automations whenever your Verisure alarm changes state or temperature changes.",
        category: "Safety & Security",
        iconUrl: "https://pbs.twimg.com/profile_images/448742746266677248/8RSgcRVz.jpeg",
        iconX2Url: "https://pbs.twimg.com/profile_images/448742746266677248/8RSgcRVz.jpeg",
        iconX3Url: "https://pbs.twimg.com/profile_images/448742746266677248/8RSgcRVz.jpeg") {
}


preferences {
    page(name: "setupPage")
}

def setupPage() {
    dynamicPage(name: "setupPage", title: "Configure Verisure", install: true, uninstall: true) {
		
        section("Disable updating here") {
			input "enabled", "bool", defaultValue: "true", title: "Enabled?"
		}
        
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

        section("Errors and logging") {
            input "logUrl", "text", title: "Splunk URL to log to", required: false
            input "logToken", "text", title: "Splunk Authorization Token", required: false
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    debug("updated", "Settings updated")
    unsubscribe()
    unschedule()
    initialize()
}

def uninstalled() {
    debug("uninstalled", "Uninstalling Verisure and removing child devices")
    removeChildDevices(getChildDevices())
}

def initialize() {
    state.app_version = "0.2.3"
    try {
        debug("initialize", "Verifying Credentials")
        updateAlarmState()
        debug("initialize", "Scheduling polling")
        schedule("0 0/1 * * * ?", checkPeriodically)
    } catch (e) {
        error("initialize", "Could not initialize app", e)
    }
}

def getAlarmState() {
    debug("getAlarmState", "Retrieving cached alarm state")
    return state.previousAlarmState
}

def checkPeriodically() {
    debug("checkPeriodically", "Periodic check from timer")
    if (enabled != null && !enabled) {
    	debug("checkPeriodically", "Not updating status as alarm is disabled in settings.")
        return
    }
    try {
        updateAlarmState()
    } catch (Exception e) {
        error("checkPeriodically", "Error updating alarm state", e)
    }
}

def tryCatch(Closure closure, String context) {
    try {
        return closure()
    } catch (Exception e) {
        error(context, "Caught error when trying to '" + context + "'. Rethrowing.", e)
        throw e
    }
}

def updateAlarmState() {
    def baseUrl = "https://mypages.verisure.com"
    def loginUrl = baseUrl + "/j_spring_security_check?locale=en_GB"

    def sessionCookie = tryCatch({ login(loginUrl) }, "Logging in")
    def alarmState = tryCatch({ getAlarmState(baseUrl, sessionCookie) }, "Getting alarm state")
    def climateState = tryCatch({ getClimateState(baseUrl, sessionCookie) }, "Getting climate state")

    if (state.previousAlarmState == null) {
        state.previousAlarmState = alarmState
    }

    //Add or Update Sensors
    climateState.each { updatedJsonDevice ->
        def tempNumber = Double.parseDouble(updatedJsonDevice.temperature.replace("&#176;", "").replace(",", "."))
        def humidityNumber = updatedJsonDevice.humidity != "" ? Double.parseDouble(updatedJsonDevice.humidity.replace("%", "").replace(",", ".")) : "0"

        if (!hasChildDevice(updatedJsonDevice.id)) {
            addChildDevice(app.namespace, "Verisure Sensor", updatedJsonDevice.id, null, [label: updatedJsonDevice.location, timestamp: updatedJsonDevice.timestamp, humidity: humidityNumber, type: updatedJsonDevice.type, temperature: tempNumber])
            debug("climateDevice.created", updatedJsonDevice.toString())
        } else {
            def existingDevice = getChildDevice(updatedJsonDevice.id)

            debug("climateDevice.updated", updatedJsonDevice.location + " | Humidity: " + humidityNumber + " | Temperature: " + tempNumber)
            existingDevice.sendEvent(name: "humidity", value: humidityNumber)
            existingDevice.sendEvent(name: "timestamp", value: updatedJsonDevice.timestamp)
            existingDevice.sendEvent(name: "type", value: updatedJsonDevice.type)
            existingDevice.sendEvent(name: "temperature", value: tempNumber)
        }
    }

    //Add & update main alarm
    if (!hasChildDevice('verisure-alarm')) {
        debug("alarmDevice.created", alarmDevice)
        addChildDevice(app.namespace, "Verisure Alarm", "verisure-alarm", null, [status: alarmState.status, loggedBy: alarmState.name, loggedWhen: alarmState.date])
    } else {
        def alarmDevice = getChildDevice('verisure-alarm')

        debug("alarmDevice.updated", alarmDevice.getDisplayName() + " | Status: " + alarmState.status + " | LoggedBy: " + alarmState.name + " | LoggedWhen: " + alarmState.date)
        alarmDevice.sendEvent(name: "status", value: alarmState.status)
        alarmDevice.sendEvent(name: "loggedBy", value: alarmState.name)
        alarmDevice.sendEvent(name: "loggedWhen", value: alarmState.date)
    }

    if (alarmState.status != state.previousAlarmState.status) {
        debug("updateAlarmState", "State changed, execution actions")
        state.previousAlarmState = alarmState
        triggerActions(alarmState.status)
    }
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
    debug("executeAction", "Executing action ${action}")
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
        //debug("Alarm", "Response from Verisure: " + response.data)
        return response.data.findAll { it."type" == "ARM_STATE" }[0]
    }
}

def getClimateState(baseUrl, sessionCookie) {
    def alarmParams = [
            uri    : baseUrl + "/overview/climatedevice",
            headers: [
                    Cookie: sessionCookie
            ]
    ]

    return httpGet(alarmParams) { response ->
        //debug("Climate", "Response from Verisure: " + response.data)
        return response.data
    }
}

private hasChildDevice(id) {
    return getChildDevice(id) != null
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

/**
 * The following methods has been added to ease remote debugging as well as enforce a certain way of logging.
 *
 */

private createLogString(String context, String message) {
    return "[verisure." + context + "] " + message
}

private error(String context, String text, Exception e) {
    log.error(createLogString(context, text), e)
    if (logUrl) {
        httpLog("error", text, e)
    }
}

private debug(String context, String text) {
    log.debug(createLogString(context, text))
    if (logUrl) {
        httpLog("debug", text, null)
    }
}

private httpLog(level, text, e) {
    def message = text
    if (e) {
        message = message + "\n" + e
    }

    def time = new Date()

    def json_body = [
            time : time.getTime(),
            host : location.id + ".smartthings.com",
            event: [
                    time       : time.format("E MMM dd HH:mm:ss.SSS z yyyy"),
                    smartapp_id: app.id,
                    location_id: location.id,
                    namespace  : app.namespace,
                    app_name   : app.name,
                    app_version: state.app_version,
                    level      : level,
                    message    : message
            ]
    ]

    def json_params = [
            uri    : logUrl,
            headers: [
                    'Authorization': "Splunk ${logToken}"
            ],
            body   : json_body
    ]

    try {
        httpPostJson(json_params)
    } catch (logError) {
        log.warn("Could not log to remote http", logError)
    }
}