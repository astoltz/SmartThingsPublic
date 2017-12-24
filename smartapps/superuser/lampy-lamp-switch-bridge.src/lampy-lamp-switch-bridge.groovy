definition(
    name: 'Lampy Lamp Switch Bridge',
    author: 'Andrew M. Stoltz',
    description: 'Sync a switch with a Lampy Lamp Bird LED',
    iconUrl: 'https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png',
    iconX2Url: 'https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png'
)

preferences {
    section('Switch') {
        input 'switch1', 'capability.switch', title: 'Switch', required: true
    }
    
    section('Lampy Lamp') {
        input 'lampyLamp1', 'device.lampyLamp', title: 'Lampy Lamp', required: true
        input 'lampyBridgeTo', 'enum', title: 'Bird', required: true, options: ['blueJay', 'cardinal']
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
    subscribe(switch1, 'switch', switchTrigger)
    subscribe(lampyLamp1, stateToCheck(), lampyLampTrigger)
}

def switchTrigger(evt) {
    def switchState = switch1.latestValue('switch')
    def ledStatus = lampyLamp1.latestValue(stateToCheck())

    if (switchState == 'on') {
        if (ledStatus == 'off') {
            if (lampyBridgeTo == 'blueJay') {
                lampyLamp1.blueJayLedOn()
            } else if (lampyBridgeTo == 'cardinal') {
                lampyLamp1.cardinalLedOn()
            }
        }
    } else {
        if (ledStatus != 'off') {
            if (lampyBridgeTo == 'blueJay') {
                lampyLamp1.blueJayLedOff()
            } else if (lampyBridgeTo == 'cardinal') {
                lampyLamp1.cardinalLedOff()
            }
        }
    }
}

def lampyLampTrigger(evt) {
    if (lampyLamp1.latestValue(stateToCheck()) == 'off') {
        switch1.off()
    } else {
        switch1.on()
    }
}

def stateToCheck() {
    if (lampyBridgeTo == 'blueJay') {
        return 'blueJayLEDState'
    } else if (lampyBridgeTo == 'cardinal') {
        return 'cardinalLEDState'
    }
    return stateToCheck
}
