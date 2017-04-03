package net.flyingff.bsbridge;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.asprise.ocr.Ocr;

public class AspriceOCRBridge implements IOCRBridge {
	private static AspriceOCRBridge INST;
	public static final IOCRBridge inst() {
		if(INST == null) {
			INST = new AspriceOCRBridge();
		}
		return INST;
	}
	private Ocr ocr;
	private int cnt = 0;
	public AspriceOCRBridge() {
		Ocr.setUp(); // one time setup
		ocr = new Ocr(); // create a new OCR engine
		ocr.startEngine("eng", Ocr.SPEED_FASTEST);
	}
	public void restart() {
		ocr.stopEngine();
		ocr = new Ocr();
		ocr.startEngine("eng", Ocr.SPEED_FASTEST);
		cnt = 0;
	}
	@Override
	public String ocrOneLineDigits(BufferedImage img, Rectangle rect) {
		if(cnt++ >= 50) {
			restart();
		}
		if(rect != null) {
			BufferedImage imgRect = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_3BYTE_BGR);
			imgRect.getGraphics().drawImage(img, 0, 0, rect.width, rect.height, rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, null);
			img = imgRect;
		}
		String raw = ocr.recognize(img,
			Ocr.RECOGNIZE_TYPE_TEXT, Ocr.OUTPUT_FORMAT_PLAINTEXT);
		return raw.replaceAll("[^0-9]", "");
	}

}
