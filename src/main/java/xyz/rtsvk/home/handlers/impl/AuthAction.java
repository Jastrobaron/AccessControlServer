package xyz.rtsvk.home.handlers.impl;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import xyz.rtsvk.home.handlers.Action;
import xyz.rtsvk.home.util.Database;
import xyz.rtsvk.home.util.Logger;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AuthAction implements Action {

	private Logger logger;

	public AuthAction() {
		this.logger = new Logger(this.getClass());
	}

	@Override
	public void run(MqttClient mqtt, String topic, String message) throws Exception {
		this.logger.info("Received auth request: " + message);

		JSONParser parser = new JSONParser();
		JSONObject json = null;
		try {
			json = (JSONObject) parser.parse(message);
		}
		catch (ParseException e) {
			this.logger.error("Error parsing JSON: " + e.getMessage());
			Map<String, Object> response = Map.of("status", false, "message", "Error parsing JSON");
			mqtt.publish("home/auth/response", new JSONObject(response).toJSONString().getBytes(), 0, false);
			return;
		}

		String cardId = String.valueOf(json.get("card_id"));
		String lockId = String.valueOf(json.get("lock_id"));
		boolean granted = Database.authorize(cardId, lockId);
		String cardName = Database.getCardName(cardId);

		// if the card is not in the database, add it with the name "John Doe"
		if (!granted && cardName == null) {
			Database.addCard(cardId, "John Doe");
		}

		Map<String, Object> response = Map.of(
				"status", granted,
				"card_id", cardId,
				"lock_id", lockId,
				"message", cardName == null ? "John Doe" : cardName
		);

		String jsonResp = new JSONObject(response).toJSONString();
		this.logger.debug("Sending auth response: " + jsonResp);
		mqtt.publish("home/auth/response", jsonResp.getBytes(StandardCharsets.UTF_8), 0, false);
		this.logger.debug("Auth response sent");
	}
}
