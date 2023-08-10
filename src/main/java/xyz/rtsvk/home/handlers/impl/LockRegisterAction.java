package xyz.rtsvk.home.handlers.impl;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import xyz.rtsvk.home.handlers.Action;
import xyz.rtsvk.home.util.Database;

public class LockRegisterAction implements Action {
	@Override
	public void run(MqttClient mqtt, String topic, String message) throws Exception {
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(message);

		String lock = json.get("lock").toString();
		Database.addLock(lock);
	}
}
