package com.semaifour.facesix.beacon;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.semaifour.facesix.mqtt.DefaultMqttMessageReceiver;

public class ScannerMqttMessageHandler extends DefaultMqttMessageReceiver {

	Logger LOG = LoggerFactory.getLogger(this.getClass());

	@Override
	public boolean messageArrived(String topic, MqttMessage message) {
		return messageArrived(topic, message.toString());
	}

	@Override
	public boolean messageArrived(String topic, String message) {
		return false;
	}


	@Override
	public String getName() {
		return "ScannerMqttMessageHandler";
	}

}
