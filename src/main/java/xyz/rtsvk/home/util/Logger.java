package xyz.rtsvk.home.util;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

	public static final PrintStream STDOUT = System.out;
	private String tag;
	public Logger(String tag) {
		this.tag = tag;
	}

	public Logger(Class<?> cl) {
		this(cl.getSimpleName());
	}

	public static void print(String tag, String message, String severity) {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);;
		print(tag, message, severity, timestamp);
	}
	public static void print(String tag, String message, String severity, String timestamp) {

		System.out.println(timestamp + " " + severity + " at " + tag + " -> " + message);

		// if the message is tagged as DEBUG, do not save it
		if (severity.equals("DEBUG")) return;

		// if the database is not connected, do not log it there
		if (!Database.isInitialized()) return;

		// if the message is short enough, log it in the database
		if (message.length() <= 256) {
			Database.log(severity, tag, message, timestamp);
		}
		// if the message is too long, split it into parts
		else while (message.length() > 256) {
			String messagePart = message.substring(0, 256);
			message = message.substring(256);
			Database.log(timestamp, severity, tag, messagePart);    // log that shit in the database
		}
	}

	public static void debug(String tag, String msg) {
		print(tag, msg, "DEBUG");
	}

	public static void info(String tag, String msg) {
		print(tag, msg, "INFO");
	}

	public static void warn(String tag, String msg) {
		print(tag, msg, "WARNING");
	}

	public static void error(String tag, String msg) {
		print(tag, msg, "ERROR");
	}

	public void debug(String msg) {
		debug(this.tag, msg);
	}

	public void info(String msg) {
		info(this.tag, msg);
	}

	public void warn(String msg) {
		warn(this.tag, msg);
	}

	public void error(String msg) {
		error(this.tag, msg);
	}
}
