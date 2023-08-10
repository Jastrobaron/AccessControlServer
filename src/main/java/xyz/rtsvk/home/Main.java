package xyz.rtsvk.home;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import xyz.rtsvk.home.discovery.DiscoveryServer;
import xyz.rtsvk.home.handlers.MqttHandler;
import xyz.rtsvk.home.handlers.impl.AuthAction;
import xyz.rtsvk.home.handlers.impl.LockRegisterAction;
import xyz.rtsvk.home.handlers.impl.LogReceiveAction;
import xyz.rtsvk.home.util.Config;
import xyz.rtsvk.home.util.Database;
import xyz.rtsvk.home.util.Logger;

import java.util.Map;

public class Main {
	public static void main(String[] args) throws Exception {

		Logger logger = new Logger(Main.class);
		Config config = Config.fromArgs(args);

		if (config.containsKey("default-config")) {
			logger.info("Writing default config to file...");
			String value = config.getString("default-config");
			Config defCfg = Config.defaultConfig(value.equals("all"));
			defCfg.forEach(config::putIfAbsent);
			config.write("config.properties");
			return;
		}

		Database.init(config);
		Map<String, Object> dbMqttConfig = Database.getSystemValues("mqtt-broker-uri", "mqtt-user", "mqtt-password", "mqtt-client-id");
		if (dbMqttConfig != null) {
			dbMqttConfig.forEach(config::putIfAbsent);
		}

		MqttClient mqttClient = new MqttClient(
				config.getString("mqtt-broker-uri"),
				config.getString("mqtt-client-id"),
				new MemoryPersistence()
		);

		MqttConnectOptions ops = new MqttConnectOptions();
		ops.setCleanSession(true);
		ops.setUserName(config.getString("mqtt-user"));
		ops.setPassword(config.getString("mqtt-password").toCharArray());
		mqttClient.connect(ops);

		MqttHandler handler = new MqttHandler(mqttClient);
		handler.subscribe("home/auth", new AuthAction());
		handler.subscribe("home/log", new LogReceiveAction());
		handler.subscribe("home/lockreg", new LockRegisterAction());
		logger.info("Connected to MQTT broker");

		final DiscoveryServer discoveryServer = new DiscoveryServer();
		if (config.containsKey("discovery-port")) {
			int port = config.getInt("discovery-port");
			discoveryServer.start(port);
		}

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				logger.info("Disconnecting from MQTT broker");
				mqttClient.disconnect();

				if (discoveryServer.isRunning()) {
					logger.info("Stopping discovery server");
					discoveryServer.stopServer();
				}

				logger.info("Closing database connection");
				Database.close();
			} catch (Exception e) {
				logger.error("Error: " + e.getMessage());
			}
		}));
	}
}