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
 *  CHANGE LOG
 *  - 0.1 - Added option for Splunk logging
 *
 */
definition(
        name: "Verisure",
        namespace: "smartthings.f12.no",
        author: "Anders Sveen",
        description: "Lets you trigger automations whenever your Verisure alarm changes state.",
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

        section("Errors and logging") {
            input "logUrl", "text", title: "Splunk URL to log to", required: false
            input "logToken", "text", title: "Splunk Authorization Token", required: false
        }
    }
}

def installed() {
    debug("Verisure Installed")
    addChildDevice(app.namespace, "Verisure Alarm", "verisure-alarm", null, [alarmstate: "unknown"])

    initialize()
}

def updated() {
    debug("Verisure Alarm Updated")

    unsubscribe()
    unschedule()
    initialize()
}

def uninstalled() {
    debug("Uninstalling Verisure Alarm app, removing all devices")
    removeChildDevices(getChildDevices())
}

def initialize() {
    state.app_version = "0.1"
    try {
        debug("Verifying credentials by doing first fetch of values")
        updateAlarmState()
        debug("Scheduling Verisure Alarm updates...")
        runIn(pollinterval, checkPeriodically)
    } catch (e) {
        error("Could not initialize Verisure app", e)
    }
}

def getAlarmState() {
    debug("Retrieving cached alarm state")
    return state.previousAlarmState
}

def checkPeriodically() {
    debug("Periodic check from timer")
    try {
        updateAlarmState()
    } catch (Exception e) {
        error("Error updating alarm state", e)
    }
    runIn(pollinterval, checkPeriodically)
}

def updateAlarmState() {
    def baseUrl = "https://mypages.verisure.com"
    def loginUrl = baseUrl + "/j_spring_security_check?locale=en_GB"

    def alarmState = null

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

    debug("Verisure Alarm state updated and is: " + alarmState)
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
    debug("Executing action ${action}")
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
        debug("Response from Verisure: " + response.data)
        return response.data.findAll { it."type" == "ARM_STATE" }[0]."status"
    }
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

private error(text, e) {
    log.error(text, e)
    if (logUrl) {
        httpLog("error", text, e)
    }
}

private debug(text) {
    log.debug(text)
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
        log.error("Could not log to remote http", logError)
    }
}