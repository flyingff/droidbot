package net.flyingff.framework;


public interface IAppContext {
	public static final String STARTER = "STARTER", PULSESUPPLIER = "PULSESUPPLIER",
			PROPNAME_WIN32="fsm_win32.properties", PROPNAME_MACOS="fsm_mac.properties";
	public <T> T getServices(Class<? super T> clazz);
	public <T> void registService(Class<? super T> clazz, T obj);
	
	public <T> T getPulse();
}
