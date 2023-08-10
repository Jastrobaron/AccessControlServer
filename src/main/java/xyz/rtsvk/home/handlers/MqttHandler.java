package xyz.rtsvk.home.handlers;

import org.eclipse.paho.client.mqttv3.*;
import xyz.rtsvk.home.util.Logger;

import java.util.HashMap;
import java.util.Map;

public class MqttHandler implements MqttCallback {

	private final Logger logger;
	private final Map<String, Action> actions;
	private final MqttClient client;

	public MqttHandler(MqttClient client) {
		this.logger = new Logger(this.getClass());
		this.actions = new HashMap<>();
		this.client = client;
		this.client.setCallback(this);
	}

	public void subscribe(String topic, Action action) throws MqttException {
		this.client.subscribe(topic);
		this.actions.put(topic, action);
	}

	public void cleanup() throws MqttException {
		for (String topic : this.actions.keySet())
			this.client.unsubscribe(topic);
		this.actions.clear();
	}

	@Override
	public void connectionLost(Throwable cause) {
		logger.error("Connection lost: " + cause.getMessage());
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String msg = new String(message.getPayload());
		if (this.actions.containsKey(topic)) {
			new Thread(() -> {
				try {
					this.actions.get(topic).run(this.client, topic, msg);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}).start();
		}
		else {
			logger.error("No action for topic: " + topic);
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		logger.info("Message delivered: " + token.getMessageId());
	}
}
