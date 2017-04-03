package net.flyingff.framework;

import java.util.function.Consumer;

public interface IPulseSupplier {
	Object getOne();
	void getAtDesireRate(int millseconds, Consumer<Object> listener);
	void stop();
}
