package net.flyingff.framework;


public interface IAppContext {
	public static final String STARTER = "STARTER", PULSESUPPLIER = "PULSESUPPLIER";
	public <T> T getServices(Class<? super T> clazz);
	public <T> void registService(Class<? super T> clazz, T obj);
	
	public <T> T getPulse();
}
