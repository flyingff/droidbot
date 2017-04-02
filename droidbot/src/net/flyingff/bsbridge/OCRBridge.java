package net.flyingff.bsbridge;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

import javax.imageio.ImageIO;

import net.flyingff.util.TempFile;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class OCRBridge {
	public static final OCRBridge INST = new OCRBridge();
	private final ITesseract t;
	private OCRBridge () {
		File dataPath = TempFile.extractTempClasspathResources("net.flyingff.bsbridge.tessdata", "tsconfig__/tessdata", x->true);
		
		t = new Tesseract();
		t.setDatapath(dataPath.getAbsolutePath());
		t.setPageSegMode(8);
		t.setConfigs(Arrays.asList("digits"));
	}
	
	public String ocrOneLineDigits(BufferedImage img, Rectangle rect) {
		String raw;
		try {
			raw = t.doOCR(img, rect);
		} catch (TesseractException e) {
			throw new RuntimeException(e);
		}
		return raw.replaceAll("[^0-9]", "");
	}
	
	public static void main(String[] args) throws Exception {
		
		BufferedImage bi = ImageIO.read(new File("d:\\x.png"));
		for(int i = 0; i < 100; i++) {
			long tm = System.currentTimeMillis();
			String result = OCRBridge.INST.ocrOneLineDigits(bi, new Rectangle(1, 1, 16, 26));
			System.out.print(result + ", ");
			System.out.println(System.currentTimeMillis() - tm);
		}
	}
}
