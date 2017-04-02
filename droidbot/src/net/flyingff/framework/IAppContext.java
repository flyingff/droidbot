package net.flyingff.framework;

import java.awt.image.BufferedImage;

public interface IAppContext {
	public <T> T getServices(Class<? super T> clazz);
	public <T> void registService(Class<? super T> clazz, T obj);
	
	public BufferedImage getScreen();
}
