package xyz.rtsvk.home.util;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Database {
	private static boolean initialized;
	private static Logger logger;
	private static Connection conn;

	public static void init(Config config) {
		init(
				config.getString("db-host"),
				config.getString("db-user"),
				config.getString("db-password"),
				config.getString("db-name")
		);

		config.entrySet().stream()
				.filter(e -> e.getKey().startsWith("mqtt-"))
				.forEach(e -> setSystemValue(e.getKey(), e.getValue().toString()));
		logger.info("Saved MQTT config to database");
	}

	public static void init(String host, String user, String password, String db) {
		logger = new Logger(Database.class);
		initialized = false;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			String url = "jdbc:mysql://" + user + ":" + password + "@" + host;
			logger.info("Connecting to database at " + url + "...");

			conn = DriverManager.getConnection(url);
			Statement st = conn.createStatement();

			st.addBatch("CREATE DATABASE IF NOT EXISTS `" + db + "`;");
			st.addBatch("USE `" + db + "`;");
			st.executeBatch();

			st.addBatch("CREATE TABLE IF NOT EXISTS `system_info` (`vkey` varchar(64), `value` varchar(128), PRIMARY KEY(`vkey`));");
			st.addBatch("CREATE TABLE IF NOT EXISTS `logging`(`id` int NOT NULL AUTO_INCREMENT, `timestamp` varchar(64), `level` varchar(16), `tag` varchar(32), `message` varchar(256), PRIMARY KEY(`id`));");
			st.addBatch("CREATE TABLE IF NOT EXISTS `cards`(`uid` varchar(16) PRIMARY KEY, `name` varchar(64));");
			st.addBatch("CREATE TABLE IF NOT EXISTS `locks`(`id` int AUTO_INCREMENT PRIMARY KEY, `name` varchar(64));");
			st.addBatch("CREATE TABLE IF NOT EXISTS `access`(`id` int AUTO_INCREMENT PRIMARY KEY, `card_id` varchar(16), `lock_id` varchar(32), `active` boolean);");
			st.executeBatch();

			st.close();

			initialized = true;
			logger.info("Database wrapper class initialized successfully.");
		}
		catch (Exception e){
			logger.error("An error occured while trying to initialize the database wrapper class.");
			throw new RuntimeException(e);
		}
	}

	public static void close() {
		if (!initialized) return;
		try {
			conn.close();
			initialized = false;
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static boolean isInitialized() {
		return initialized;
	}

	// method to push a new log message into the logging table
	public static void log(String level, String tag, String message, String timestamp) {
		if (!initialized) return;

		try {
			Statement st = conn.createStatement();

			String sql = "INSERT INTO `logging`(`timestamp`, `level`, `tag`, `message`) VALUES (";
			sql += "'" + timestamp + "'," +
					"'" + level + "'," +
					"'" + tag + "'," +
					"'" + message + "'";
			sql += ");";
			st.execute(sql);

			st.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// method to add a new card to the database
	public static boolean addCard(String uid, String name) {
		if (!initialized) return false;

		try {
			Statement st = conn.createStatement();

			// check if card already exists
			ResultSet set = st.executeQuery("SELECT `uid` FROM `cards` WHERE `uid`='" + uid + "';");
			if (set.next()) {
				set.close();
				st.close();
				logger.error("Card " + uid + " already exists in the database.");
				return false;
			}

			// add card
			String sql = "INSERT INTO `cards`(`uid`, `name`) VALUES (";
			sql += "'" + uid + "'," +
					"'" + name + "'";
			sql += ");";
			st.execute(sql);

			set.close();
			st.close();
			logger.info("Card " + uid + " added to the database.");
			return true;

		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("An error occured while trying to add card " + uid + " to the database.");
			return false;
		}
	}

	public static boolean authorize(String cardUID, String lockId) {
		if (!initialized) return false;

		try {
			Statement st = conn.createStatement();

			// check if the lock exists by name
			ResultSet set = st.executeQuery("SELECT `id` FROM `locks` WHERE `name`='" + lockId + "';");
			if (set.next()) {
				lockId = set.getString("id");
				set.close();
			}
			else {
				st.close();
				set.close();
				logger.warn("Card " + cardUID + " tried to access lock " + lockId + " but the lock does not exist.");
				return false;
			}

			set = st.executeQuery("SELECT `active` FROM `access` WHERE `card_id`='" + cardUID + "' AND `lock_id`='" + lockId + "';");
			if (set.next()) {
				boolean result = set.getBoolean("active");
				st.close();
				set.close();
				logger.info("Card " + cardUID + " tried to access lock " + lockId + " and was " + (result ? "granted" : "denied") + " access.");
				return result;
			}
			else {
				st.close();
				set.close();
				logger.warn("Card " + cardUID + " tried to access lock " + lockId + " but is not authorized.");
				return false;
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
			logger.error("An error occured while trying to authorize card " + cardUID + " for lock " + lockId + ".");
			return false;
		}
	}

	public static String getCardName(String cardId) {
		try {
			ResultSet set = conn.createStatement().executeQuery("SELECT `name` FROM `cards` WHERE `uid`='" + cardId + "';");
			if (set.next()) {
				String result = set.getString("name");
				if (result == null) result = "Unknown";
				set.close();
				return result;
			}
			else {
				set.close();
				return null;
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Map<String, Object> getSystemValues(String... keys) {
		// fetch values from the system_info table
		try {

			StringBuilder sql = new StringBuilder("SELECT * FROM `system_info` WHERE ");

			for (int i = 0; i < keys.length; i++) {
				sql.append("`vkey`='").append(keys[i]).append("'");
				if (i < keys.length - 1) sql.append(" OR ");
			}
			sql.append(";");

			ResultSet set = conn.createStatement().executeQuery(sql.toString());
			Map<String, Object> result = new HashMap<>();
			if (set.isBeforeFirst()) {
				while (set.next()) {
					String key = set.getString("vkey");
					String value = set.getString("value");
					result.put(key, value);
				}
				set.close();
				return result;
			}
			else {
				set.close();
				return null;
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Object getSystemValue(String key) {
		Map<String, Object> result = getSystemValues(key);
		if (result == null) return null;
		return result.get(key);
	}

	// method to set a value in the system_info table
	public static void setSystemValue(String key, String value) {
		try {
			Statement st = conn.createStatement();

			// check if key already exists
			ResultSet set = st.executeQuery("SELECT `vkey` FROM `system_info` WHERE `vkey`='" + key + "';");
			if (set.next()) {
				set.close();
				st.close();
				logger.info("Key " + key + " already exists in the database. Updating value.");
				st = conn.createStatement();
				st.execute("UPDATE `system_info` SET `value`='" + value + "' WHERE `vkey`='" + key + "';");
			}
			else {
				set.close();
				st.close();
				logger.info("Key " + key + " does not exist in the database. Creating new entry.");
				st = conn.createStatement();
				st.execute("INSERT INTO `system_info`(`vkey`, `value`) VALUES ('" + key + "','" + value + "');");
			}

			st.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
			logger.error("An error occured while trying to set key " + key + " to value " + value + ".");
		}
	}

	public static void addLock(String name) {
		try {
			Statement st = conn.createStatement();

			// check if the lock exists by name
			ResultSet set = st.executeQuery("SELECT `id` FROM `locks` WHERE `name`='" + name + "';");
			if (set.next()) {
				set.close();
				st.close();
				logger.error("Lock " + name + " already exists in the database.");
				return;
			}

			// add lock
			String sql = "INSERT INTO `locks`(`name`) VALUES (";
			sql += "'" + name + "'";
			sql += ");";
			st.execute(sql);

			set.close();
			st.close();
			logger.info("Lock " + name + " added to the database.");

		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("An error occured while trying to add lock " + name + " to the database.");
		}
	}
}
