package inst;

import java.awt.EventQueue;

import net.flyingff.bsbridge.ADBCommander;
import net.flyingff.ui.PicFrame;

public class TestCapture {
	public static void main(String[] args) {
		PicFrame pf = new PicFrame(290, 550, null, null, null);
		ADBCommander ac = new ADBCommander();
		ac.captureViaVideo(270, 540, frame->{
			EventQueue.invokeLater(()->{
				pf.setPic(frame);
			});
		});
		System.exit(0);
	}
}
