definition(
    name: 'Lampy Lamp Contact Bridge',
    author: 'Andrew M. Stoltz',
    description: 'Sync a contact sensor with a Lampy Lamp Bird LED',
    iconUrl: 'https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png',
    iconX2Url: 'https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png'
)

preferences {
	section('Switch') {
    	input 'contact1', 'capability.contactSensor', title: 'Contact', required: true
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
    subscribe(contact1, 'contact', contactTrigger)
}

def contactTrigger(evt) {
    def contactValue = contact1.latestValue('contact')
	if (contactValue == 'open') {
        if (lampyBridgeTo == 'blueJay') {
            lampyLamp1.blueJayLedOn()
        } else if (lampyBridgeTo == 'cardinal') {
            lampyLamp1.cardinalLedOn()
        }
    } else {
        if (lampyBridgeTo == 'blueJay') {
            lampyLamp1.blueJayLedOff()
        } else if (lampyBridgeTo == 'cardinal') {
            lampyLamp1.cardinalLedOff()
        }
    }
}
