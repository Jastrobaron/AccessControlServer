package xyz.rtsvk.home.handlers.impl;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import xyz.rtsvk.home.handlers.Action;
import xyz.rtsvk.home.util.Logger;

public class LogReceiveAction implements Action {
	@Override
	public void run(MqttClient mqtt, String topic, String message) throws Exception {
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(message);

		Logger.print(
				json.get("tag").toString(),
				json.get("message").toString(),
				json.get("severity").toString(),
				json.get("timestamp").toString()
		);
	}
}
