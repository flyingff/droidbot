package test;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;

import net.flyingff.bsbridge.ADBCommander;
import net.flyingff.ui.PicFrame;

public class TMain {

	private static PicFrame pf;
	public static void nonmain(String[] args) throws Exception {
		ADBCommander cmd = new ADBCommander();
		BufferedImage first = cmd.capture(false, 4);
		EventQueue.invokeAndWait(()->{
			pf = new PicFrame(first.getWidth(), first.getHeight(),
			e->{ }, e->{
				cmd.tap(e.getX(), e.getY());
			}, e->{ });
		});
		while(true) {
			BufferedImage img = cmd.capture(false, 4);
			if(img != null) {
				EventQueue.invokeLater(()->{
					pf.setPic(img);
				});
			} else {
				System.err.println("Failed to capture...");
				Thread.sleep(1000);
			}
		}
	}
}
