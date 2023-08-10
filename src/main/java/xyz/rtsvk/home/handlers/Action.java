package xyz.rtsvk.home.handlers;

import org.eclipse.paho.client.mqttv3.MqttClient;

public interface Action {
	void run(MqttClient mqtt, String topic, String message) throws Exception;
}
