package net.flyingff.bsbridge;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.sun.jna.Platform;

public interface IOCRBridge {
	public String ocrOneLineDigits(BufferedImage img, Rectangle rect);
	static IOCRBridge INST() {
		if(Platform.isWindows()) {
			return TessOCRBridge.inst();
		} else {
			return AspriceOCRBridge.inst();
		}
	}
}
