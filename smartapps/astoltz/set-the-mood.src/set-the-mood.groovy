/**
 *  Dinner time
 *
 *  Copyright 2017 Andrew Stoltz
 *
 */
definition(
    name: "Set the mood.",
    author: "Andrew M. Stoltz",
    description: "Set lighting based on a routine.",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
}

preferences {
	page(name: "routineSelection")
    page(name: "lightSelection")
}

def routineSelection() {
	dynamicPage(name: "routineSelection", install: true, uninstall:true) {
    	section("Routine") {
        	def routines = location.helloHome?.getPhrases()*.label
    		input "routine1", "enum", title: "Routine", required: true, options: routines
        }
        
    	section("Lights on") {
        	input "lightson", "capability.light", title: "Lights", multiple: true, required: false, submitOnChange: true
            if (lightson) {
            	lightson.each {
                	if (it.capabilities.any { it.name == 'Switch Level' }) {
                		input "lightlevel_${it.id}", "number", title: "Light level of ${it.displayName}", required: false, defaultValue: 100, range: "0..100"
                    }
                }
			}
		}
        
    	section("Lights off") {
        	input "lightsoff", "capability.light", title: "Lights", multiple: true, required: false
		}

        section([mobileOnly:true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(location, "routineExecuted", routineChanged)
}

def routineChanged(evt) {
	def routines = location.helloHome?.getPhrases()*.label
    //def selectedRoutine = routines.get(routine1 as Integer)
    def selectedRoutine = routine1
	if (evt.displayName == selectedRoutine) {
    	// Lights on
        lightson.each {
            if (it.capabilities.any { it.name == 'Switch Level' }) {
            	def brightnessLevel = settings."lightlevel_${it.id}"
                log.debug "Brightness level for ${it.displayName} is ${brightnessLevel}"
                it.setLevel(brightnessLevel)
            } else {
                log.debug "Turning on ${it.displayName}"
                it.on()
            }
        }
        
        // Lights off
        lightsoff.off()
	} else {
    	log.debug "Expected ${selectedRoutine} but got ${evt.displayName}"
    }
}