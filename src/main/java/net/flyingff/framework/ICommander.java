package net.flyingff.framework;

public interface ICommander {
	void tap(int x, int y);
	void press(int x, int y, int tm);
	void swipe(int x0, int y0, int x1, int y1, int tm);
}
