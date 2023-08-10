package xyz.rtsvk.home.discovery;

import org.json.simple.JSONObject;
import xyz.rtsvk.home.util.Database;
import xyz.rtsvk.home.util.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;

public class DiscoveryServer extends Thread {

	private final Logger logger;
	private int port;
	private boolean running;

	public DiscoveryServer() {
		this.logger = new Logger(this.getClass());
		this.running = true;
	}

	@Override
	public void run() {
		// create a UDP socket to listen for discovery broadcasts
		// if a broadcast is received, send a response with the server's IP address
		try (DatagramSocket udpSrv = new DatagramSocket(this.port)) {

			byte[] buffer = new byte[1024];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

			while (this.running) {
				udpSrv.receive(packet);
				String msg = new String(packet.getData(), 0, packet.getLength()).toLowerCase();
				if (msg.equals("acc_discover")) {

					Map<String, Object> response = Database.getSystemValues(
							"mqtt-uri",
							"mqtt-username",
							"mqtt-password"
					);
					if (response == null) {
						logger.info("Error getting MQTT config from database");
						continue;
					}

					byte[] data = new JSONObject(response).toJSONString().getBytes();
					DatagramPacket responsePacket = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
					udpSrv.send(responsePacket);
				}
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isRunning() {
		return this.running;
	}

	public void stopServer() {
		this.running = false;
	}

	public void start(int port) {
		this.port = port;
		this.start();
	}
}
