package inst;

import java.awt.EventQueue;
import java.awt.Point;

import net.flyingff.bsbridge.ADBCommander;
import net.flyingff.ui.PicFrame;

public class TestCapture {
	private static int downX, downY;
	public static void main2(String[] args) {
		ADBCommander ac = new ADBCommander();
		PicFrame pf = new PicFrame(1200, 800, ev->{
			downX = ev.getX();
			downY = ev.getY();
		}, ev->{
			if(Point.distance(ev.getX(), ev.getY(), downX, downY) > 20) {
				ac.swipe(downX, downY, ev.getX(), ev.getY(), 200);
			} else {
				ac.tap(ev.getX(), ev.getY());
			}
		}, ev->{
			
		});
		ac.captureViaVideo(1200, 800, frame->{
			EventQueue.invokeLater(()->{
				pf.setPic(frame);
			});
		});
		System.exit(0);
	}
}
