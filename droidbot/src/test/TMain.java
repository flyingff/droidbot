package test;

import java.awt.EventQueue;

import net.flyingff.bsbridge.ADBCommander;
import net.flyingff.bsbridge.WindowCapturer;
import net.flyingff.ui.PicFrame;

public class TMain {

	private static PicFrame pf;
	public static void main(String[] args) throws Exception {
		WindowCapturer wcap = new WindowCapturer();
		ADBCommander cmd = new ADBCommander();
		EventQueue.invokeAndWait(()->{
			pf = new PicFrame(e->{
				cmd.mouseDown(e.getX() * 0x7FFF / 800, e.getY() * 0x7FFF / 450);
			}, e->{
				cmd.mouseUp(e.getX() * 0x7FFF / 800, e.getY() * 0x7FFF / 450);
			}, e->{
				cmd.mouseMove(e.getX() * 0x7FFF / 800, e.getY() * 0x7FFF / 450);
			});
		});
		wcap.intervalCap(40, im->{
			EventQueue.invokeLater(()->{
				pf.setPic(im);
			});
		});
	}
}
