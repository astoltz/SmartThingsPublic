definition(
    name: 'Lampy Lamp Alarm Bridge',
    author: 'Andrew M. Stoltz',
    description: 'Sync an alarm with the Lampy Lamp',
    iconUrl: 'https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png',
    iconX2Url: 'https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png'
)

preferences {
    section('Alarm') {
        input 'alarm1', 'capability.alarm', title: 'Alarm', required: true
    }
    
    section('Lampy Lamp') {
        input 'lampyLamp1', 'device.lampyLamp', title: 'Lampy Lamp', required: true
        input 'lampyBridgeTo', 'enum', title: 'Bird', required: true, options: ['blueJay', 'cardinal']
        input 'silentModes', 'mode', title: 'No chirp during modes', required: false, multiple: true
    }

    section('Shut Off') {
        input 'presence1', 'capability.presenceSensor', title: 'Which person', required: false
        input 'offAfterMinutes', 'number', title: 'Minutes of inactivity', defaultValue: 30, required: false
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
    state.hasSirened = false
    state.isStrobing = false

    subscribe(alarm1, 'alarm', processTrigger)
    subscribe(presence1, 'presence.present', processTrigger)
    if (lampyBridgeTo == 'blueJay') {
        subscribe(lampyLamp1, 'blueJayLEDState.off', processTrigger)
    } else if (lampyBridgeTo == 'cardinal') {
        subscribe(lampyLamp1, 'cardinalLEDState.off', processTrigger)
    } else {
        log.debug "Unknown lampyBridgeTo of ${lampyBridgeTo}"
    }
}

def processTrigger(evt) {
    setupCurrentState()
}

def setupCurrentState() {
    def alarmShouldSiren = alarm1.latestValue('alarm') in ['both', 'siren']
    def alarmLightShouldBeOn = alarm1.latestValue('alarm') in ['both', 'strobe']

    if (presence1.any { it?.latestValue('presence') == 'present' }) {
        alarmLightShouldBeOn = false
        alarmShouldSiren = false

        if (alarm1.latestValue('alarm') != 'off') {
            alarm1.off()
        }
    }

    if (alarmShouldSiren) {
        alarmSiren()
    }

    if (alarmLightShouldBeOn) {
        alarmStrobe()
    } else {
        alarmOff()
    }
}

def alarmOff() {
    if (lampyBridgeTo == 'blueJay') {
        if (lampyLamp1.latestValue('blueJayLEDState') != 'off') {
            log.debug 'Shutting off the Blue Jay LED'
            lampyLamp1.blueJayLedOff()
        }
    } else if (lampyBridgeTo == 'cardinal') {
        if (lampyLamp1.latestValue('cardinalLEDState') != 'off') {
            log.debug 'Shutting off the Cardinal LED'
            lampyLamp1.cardinalLedOff()
        }
    }
    state.hasSirened = false
    state.isStrobing = false
}

def alarmSiren() {
    if (state.hasSirened) {
        return;
    }

    if (location.currentMode in silentModes) {
        log.debug "Alarm siren was requested but ${location.currentMode} is a quiet mode."
    } else if (lampyBridgeTo == 'blueJay') {
        log.debug 'Sounding the Blue Jay alarm!'
        lampyLamp1.blueJayTweet()
    } else if (lampyBridgeTo == 'cardinal') {
        log.debug 'Sounding the Cardinal alarm!'
        lampyLamp1.cardinalTweet()
    }

    state.hasSirened = true
}

def alarmStrobe() {
    if (lampyBridgeTo == 'blueJay') {
        if (lampyLamp1.latestValue('blueJayLEDState') == 'off') {
            log.debug 'Strobing Blue Jay LED'
            lampyLamp1.blueJayLedFlash()
        }
    } else if (lampyBridgeTo == 'cardinal') {
        if (lampyLamp1.latestValue('cardinalLEDState') == 'off') {
            log.debug 'Strobing Cardinal LED'
            lampyLamp1.cardinalLedFlash()
        }
    }

    if (! state.isStrobing) {
        def delay = (offAfterMinutes != null && offAfterMinutes != '') ? offAfterMinutes * 60 : 1800
        runIn(delay, alarmTimeout, [overwrite: true])
        state.isStrobing = true
    }
}

def alarmTimeout() {
    log.debug 'Alarm timed out'
    unschedule(alarmTimeout)
    alarm1.off()
}
