package net.flyingff.framework;


public interface IAppContext {
	String STARTER = "STARTER", PULSESUPPLIER = "PULSESUPPLIER",
			PROPNAME_WIN32="net/flyingff/framework/fsm_win32.properties", PROPNAME_MACOS="net/flyingff/framework/fsm_mac.properties";
	<T> T getServices(Class<? super T> clazz);
	<T> void registryService(Class<? super T> clazz, T obj);
	
	<T> T getPulse();
}
