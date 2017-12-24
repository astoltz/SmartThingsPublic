definition(
    name: 'Turn It On With Motion',
    author: 'Andrew M. Stoltz',
    description: 'Turn something on when there is motion. Shut off with inactivity.',
    iconUrl: 'https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png',
    iconX2Url: 'https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png'
)

preferences {
    page(name: 'configSetup')
}

def configSetup() {
    dynamicPage(name: 'configSetup', install: true, uninstall:true) {
        section('Activity') {
            input 'motion1', 'capability.motionSensor', title: 'Motion sensors', required: false, multiple: true, hideWhenEmpty: true
            input 'contact1', 'capability.contactSensor', title: 'Contact sensors', required: false, multiple: true, hideWhenEmpty: true
        }
        
        section('Lights on') {
            input 'switches', 'capability.light', title: 'Lights', multiple: true, required: true, submitOnChange: true
        }

        switches.each { light ->
            if (light.capabilities.any { it.name == 'Switch Level' }) {
                section("Light level for ${light} per mode") {
                    location.modes.each { mode ->
                        input "lightlevel_${light.id}_${mode}", 'number', title: mode, required: false, defaultValue: 100, range: '0..100'
                    }
                }
            }
        }

        section('Inactivity delay per mode') {
            location.modes.each { mode ->
                input "offAfterMinutes_${mode}", 'number', title: mode, required: false, defaultValue: 10
            }
        }
        
        section([mobileOnly:true]) {
            label title: 'Assign a name', required: false
            mode title: 'Set for specific mode(s)', required: false
        }
    }
}

def installed() {
    subscribe()
}

def updated() {
    unsubscribe()
    subscribe()
}

def subscribe() {
    state.managingLights = false
    subscribe(motion1, 'motion.active', motion)
    subscribe(contact1, 'contact.open', motion)

    subscribe(motion1, 'motion.inactive', beginOff)
    subscribe(contact1, 'contact.closed', beginOff)

    subscribe(switches, 'switch.off', externalOff)

    subscribe(location, modeChangeHandler)
}

// Motion sensor

def motion(evt) {
    // We only turn on lights for initial motion/contact event
    if (state.managingLights) {
        unschedule(shutOff)
        return
    }

    if (switches.every { it?.latestValue('switch') == 'on' }) {
        log.debug 'Designated lights are already on.'
    } else {
        state.managingLights = true
        switches.each {
            if (it.capabilities.any { it.name == 'Switch Level' }) {
                def brightnessLevel = settings."lightlevel_${it.id}_${location.currentMode}"
                if (brightnessLevel == null || brightnessLevel == '') {
                    brightnessLevel = 100
                }

                // Sometimes lights report 99% instead of 100% so allow some tollerance
                def currentLevel = it.latestValue('level')
                if (it.latestValue('switch') != 'on' || currentLevel < brightnessLevel - 1) {
                    log.debug "Setting brightness level for ${it.displayName} to ${brightnessLevel} because of mode ${location.currentMode}."
                    it.setLevel(brightnessLevel)
                } else {
                    log.debug "Brightness level for ${it.displayName} mode ${location.currentMode} is already ${currentLevel}."
                }
            } else if (it.latestValue('switch') != 'on') {
                log.debug "Turning on ${it.displayName}"
                it.on()
            } else {
                log.debug "Switch ${it.displayName} is already on."
            }
        }
        
        unschedule(shutOff)
    }
}

def beginOff(evt) {
    if (state.managingLights) {
        def minutes = settings."offAfterMinutes_${location.currentMode}"
        def delay = (minutes != null && minutes != '') ? minutes * 60 : 600
        runIn(delay, shutOff, [overwrite: true])
    } else {
        log.debug "beginOff() but not managing lights"
    }
}

def shutOff() {
    if (state.managingLights) {
        def shutOff = true

        def motionState = motion1.any { it?.latestValue('motion') == 'active' }
        if (motionState) {
            log.debug 'Shut off called but motion is still active.'
            shutOff = false
        }

        def contactState = contact1.any { it?.latestValue('contact') == 'open' }
        if (contactState) {
            log.debug 'Shut off called but contact is still open.'
            shutOff = false
        }

        if (shutOff) {
            log.debug 'Shut off lights.'
            switches.off()
            state.managingLights = false
        }
    } else {
        log.debug "shutOff() but not managing lights"
    }
}

def externalOff(evt) {
    if (state.managingLights && switches.every { it?.latestValue('switch') == 'off' }) {
        log.debug 'All lights were shut off externally. Cancelling timer.'
        state.managingLights = false
        unschedule(shutOff)
    }
}

def modeChangeHandler(evt) {
    if (state.managingLights) {
        log.debug "Mode changed to ${evt.value}"
        switches.each {
            if (it.capabilities.any { it.name == 'Switch Level' }) {
                def brightnessLevel = settings."lightlevel_${it.id}_${location.currentMode}"
                if (brightnessLevel == null || brightnessLevel == '') {
                    brightnessLevel = 100
                }

                // Sometimes lights report 99% instead of 100% so allow some tollerance
                def currentLevel = it.latestValue('level')
                if (it.latestValue('switch') == 'on') {
                    if (currentLevel != brightnessLevel) {
                        log.debug "Setting brightness level for ${it.displayName} to ${brightnessLevel} because of mode ${location.currentMode}."
                        it.setLevel(brightnessLevel)
                    } else {
                        log.debug "Brightness level for ${it.displayName} mode ${location.currentMode} is already ${currentLevel}."
                    }
                }
            }
        }
    } else {
        def motionState = motion1.any { it?.latestValue('motion') == 'active' }
        def contactState = contact1.any { it?.latestValue('contact') == 'open' }
        def allLightsOff = switches.every { it?.latestValue('switch') == 'off' }
        if (allLightsOff && (motionState || contactState)) {
            // We are in a state where we went from an unmanaged mode to a managed mode and
            // the managed lights are all off but there is detected motion or contact so
            // without this call, no events will trigger the lights to come on.
            log.debug "Mode changed to ${evt.value} from previously unmanaged state."
            motion(evt)
        }
    }
}
