package inst;

import java.awt.image.BufferedImage;

import net.flyingff.bsbridge.ADBCommander;
import net.flyingff.ui.PicFrame;

public class ColorHacker {
	public static void main(String[] args) {
		ADBCommander cmd = new ADBCommander();
		BufferedImage first = cmd.capture2(4);
		PicFrame pf = new PicFrame(first.getWidth(), first.getHeight(), 
				null, null, null);
		
		while(true) {
			pf.setPic(cmd.capture(false, 4));
		}
	}
}
