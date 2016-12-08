/**
 *  Lampy Lamp
 *
 *  Copyright 2016 Andrew M. Stoltz
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
 
import groovy.json.JsonSlurper

metadata {
	definition (name: "Lampy Lamp", namespace: "astoltz", author: "Andrew M. Stoltz") {
		capability "Light"
        capability "Refresh"
        capability "Alarm"
        capability "Switch"

		attribute "lampState", "string"
		attribute "blueJayLEDStatus", "string"
		attribute "blueJayLEDState", "string"
		attribute "blueJayTweeting", "string"
		attribute "cardinalLEDStatus", "string"
		attribute "cardinalLEDState", "string"
		attribute "cardinalTweeting", "string"

		command "flash"
		command "blueJayLedOn"
		command "blueJayLedOff"
		command "blueJayLedFlash"
		command "blueJayTweet"
		command "cardinalLedOn"
		command "cardinalLedOff"
		command "cardinalLedFlash"
		command "cardinalTweet"
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
        standardTile("switch", "device.switch", width: 3, height: 1, canChangeIcon: true) {
            state "off", label: '${currentValue}', action: "light.on",
                  icon: "st.switches.light.off"
            state "on", label: '${currentValue}', action: "light.off",
                  icon: "st.switches.light.on"
        }
        
        standardTile("lampState", "lampState", canChangeIcon: true) {
            state "off", label: '${currentValue}', action: "flash",
                  icon: "st.switches.light.off"
            state "on", label: '${currentValue}', action: "flash",
                  icon: "st.switches.light.on"
            state "flashing", label: '${currentValue}', action: "light.off",
                  icon: "st.switches.light.on"
        }

        standardTile("blueJayLEDStatus", "blueJayLEDStatus", canChangeIcon: true) {
            state "off", label: '${currentValue}', action: "blueJayLedOn",
                  icon: "st.switches.light.off", backgroundColor: "#0000aa"
            state "on", label: '${currentValue}', action: "blueJayLedOff",
                  icon: "st.switches.light.on", backgroundColor: "#0000ff"
        }
        
        standardTile("blueJayLEDState", "blueJayLEDState", canChangeIcon: true) {
            state "off", label: '${currentValue}', action: "blueJayLedFlash",
                  icon: "st.switches.light.off", backgroundColor: "#0000aa"
            state "on", label: '${currentValue}', action: "blueJayLedFlash",
                  icon: "st.switches.light.on", backgroundColor: "#0000ff"
            state "flashing", label: '${currentValue}', action: "blueJayLedOff",
                  icon: "st.switches.light.on", backgroundColor: "#0000ff"
        }

        standardTile("blueJayTweeting", "blueJayTweeting", width: 2, canChangeIcon: true) {
            state "off", label: 'Tweet', action: "blueJayTweet",
                  icon: "st.alarm.alarm.alarm", backgroundColor: "#0000aa"
            state "on", label: 'Tweeting',
                  icon: "st.alarm.alarm.alarm", backgroundColor: "#0000ff"
        }

        standardTile("cardinalLEDStatus", "cardinalLEDStatus", canChangeIcon: true) {
            state "off", label: '${currentValue}', action: "cardinalLedOn",
                  icon: "st.switches.light.off", backgroundColor: "#aa0000"
            state "on", label: '${currentValue}', action: "cardinalLedOff",
                  icon: "st.switches.light.on", backgroundColor: "#ff0000"
        }
        
        standardTile("cardinalLEDState", "cardinalLEDState", canChangeIcon: true) {
            state "off", label: '${currentValue}', action: "cardinalLedFlash",
                  icon: "st.switches.light.off", backgroundColor: "#aa0000"
            state "on", label: '${currentValue}', action: "cardinalLedFlash",
                  icon: "st.switches.light.on", backgroundColor: "#ff0000"
            state "flashing", label: '${currentValue}', action: "cardinalLedOff",
                  icon: "st.switches.light.on", backgroundColor: "#ff0000"
        }

        standardTile("cardinalTweeting", "cardinalTweeting", width: 2, canChangeIcon: true) {
            state "off", label: 'Tweet', action: "cardinalTweet",
                  icon: "st.alarm.alarm.alarm", backgroundColor: "#aa0000"
            state "on", label: 'Tweeting',
                  icon: "st.alarm.alarm.alarm", backgroundColor: "#ff0000"
        }
        
        main("switch")
        details(["switch", "blueJayLEDStatus", "blueJayTweeting", "cardinalLEDStatus", "cardinalTweeting", "lampState", "blueJayLEDState", "cardinalLEDState"])
	}
}

// parse events into attributes
def parse(String description) {
    def map = [:]
    def descMap = parseDescriptionAsMap(description)
    def body = new String(descMap["body"].decodeBase64())
    def slurper = new JsonSlurper()
    def result = slurper.parseText(body)
    
    if (result.containsKey("lamp_status")) {
    	sendEvent(name: "switch", value: result.lamp_status ? "on" : "off")
	}
    //if (result.containsKey("lamp_state")) {
    //	sendEvent(name: "lampState", value: result.lamp_state)
	//}
    if (result.containsKey("blue_jay_led_status")) {
    	sendEvent(name: "blueJayLEDStatus", value: result.blue_jay_led_status ? "on" : "off")
	}
    if (result.containsKey("blue_jay_led_state")) {
    	sendEvent(name: "blueJayLEDState", value: result.blue_jay_led_state)
	}

    if (result.containsKey("blue_jay_tweeting") || result.containsKey("lamp_state")) {
    	def lampState = result.containsKey("lamp_state") ? result.lamp_state : "off";
        def blueJayTweeting = result.containsKey("blue_jay_tweeting") ? result.blue_jay_tweeting : false;

    	if (result.blue_jay_tweeting && lampState == "flashing") {
        	sendEvent(name: "alarm", value: "both")
        } else if (result.blue_jay_tweeting) {
        	sendEvent(name: "alarm", value: "siren")
        } else if (lampState == "flashing") {
        	sendEvent(name: "alarm", value: "strobe")
        } else {
	        sendEvent(name: "alarm", value: "off")
        }
    
		sendEvent(name: "lampState", value: result.lamp_state)
        sendEvent(name: "blueJayTweeting", value: result.blue_jay_tweeting ? "on" : "off")
	}
    
    if (result.containsKey("cardinal_led_status")) {
    	sendEvent(name: "cardinalLEDStatus", value: result.cardinal_led_status ? "on" : "off")
	}
    if (result.containsKey("cardinal_led_state")) {
    	sendEvent(name: "cardinalLEDState", value: result.cardinal_led_state)
	}
    if (result.containsKey("cardinal_tweeting")) {
    	sendEvent(name: "cardinalTweeting", value: result.cardinal_tweeting ? "on" : "off")
	}
}

def parseDescriptionAsMap(description) {
	description.split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
}

private setDeviceNetworkId(ip,port){
  	def iphex = convertIPtoHex(ip)
  	def porthex = convertPortToHex(port)
  	device.deviceNetworkId = "$iphex:$porthex"
  	log.debug "Device Network Id set to ${iphex}:${porthex}"
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}

// handle commands
def off() {
	log.debug "Executing 'off'"
    setDeviceNetworkId("192.168.1.141","80") 
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/lamp/off"
    )
    return result
}

def on() {
	log.debug "Executing 'on'"
    setDeviceNetworkId("192.168.1.141","80") 
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/lamp/on"
    )
    return result
}

def both() {
	return [flash(), blueJayTweet(), cardinalTweet()];
}

def siren() {
	return blueJayTweet();
}

def strobe() {
	return flash();
}

def flash() {
	log.debug "Executing 'flash'"
    setDeviceNetworkId("192.168.1.141","80") 
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/lamp/flash"
    )
    return result
}

def blueJayLedOn() {
	log.debug "Executing 'blueJayLedOn'"
    setDeviceNetworkId("192.168.1.141","80") 
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/bluejay/led/on"
    )
    return result
}

def blueJayLedOff() {
	log.debug "Executing 'blueJayLedOff'"
    setDeviceNetworkId("192.168.1.141","80") 
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/bluejay/led/off"
    )
    return result
}

def blueJayLedFlash() {
	log.debug "Executing 'blueJayLedFlash'"
    setDeviceNetworkId("192.168.1.141","80") 
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/bluejay/led/flash"
    )
    return result
}

def blueJayTweet() {
	log.debug "Executing 'blueJayTweet'"
    setDeviceNetworkId("192.168.1.141","80") 
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/bluejay/tweet"
    )
    return result
}

def cardinalLedOn() {
	log.debug "Executing 'cardinalLedOn'"
    setDeviceNetworkId("192.168.1.141","80") 
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/cardinal/led/on"
    )
    return result
}

def cardinalLedOff() {
	log.debug "Executing 'cardinalLedOff'"
    setDeviceNetworkId("192.168.1.141","80") 
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/cardinal/led/off"
    )
    return result
}

def cardinalLedFlash() {
	log.debug "Executing 'cardinalLedFlash'"
    setDeviceNetworkId("192.168.1.141","80") 
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/cardinal/led/flash"
    )
    return result
}

def cardinalTweet() {
	log.debug "Executing 'cardinalTweet'"
    setDeviceNetworkId("192.168.1.141","80") 
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/cardinal/tweet"
    )
    return result
}

def refresh() {
	log.debug "Executing 'refresh'"
    setDeviceNetworkId("192.168.1.141","80") 
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/status"
    )
    return result
}


