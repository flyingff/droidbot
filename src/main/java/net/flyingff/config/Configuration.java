package net.flyingff.config;

import java.io.IOException;
import java.util.Properties;

public class Configuration {
	public static final String X="";
	public static final Configuration INST = new Configuration();
	private final Properties p;
	public Configuration() {
		p = new Properties();
		try {
			p.load(Configuration.class.getResourceAsStream("config.properties"));
		} catch (IOException e) {
			throw new RuntimeException("Cannot load property.", e);
		}
	}
	public String get(String key) {
		return p.getProperty(key);
	}
	public int getInt(String key) {
		return Integer.parseInt(p.getProperty(key));
	}
	public boolean getBool(String key) {
		return Boolean.parseBoolean(p.getProperty(key));
	}
}
