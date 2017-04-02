package net.flyingff.framework;

import java.util.function.Consumer;

public class PureTimeSupplier implements IPulseSupplier {
	private boolean running = false, stopped = false;
	private long lastTm;
	@Override
	public Object getOne() {
		return System.currentTimeMillis();
	}

	@Override
	public void getAtDesireRate(int millseconds, Consumer<Object> listener) {
		if(running || stopped) {
			throw new IllegalStateException();
		}
		lastTm = 0;
		new Thread(()->{
			running = true;
			while(!stopped) {
				long now;
				try {
					Thread.sleep(1);
					now = System.currentTimeMillis();
					if(now - lastTm >= millseconds) {
						lastTm = now;
						listener.accept(System.currentTimeMillis());
					}
				} catch(Exception e) {e.printStackTrace(); }
			}
		}).start();
	}

	@Override
	public void stop() {
		stopped = true;
	}

}
