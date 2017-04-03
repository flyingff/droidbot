package droidbot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;

import net.flyingff.bsbridge.ImageFinder;
import net.flyingff.bsbridge.OCRBridge;

public class TestTrace {
	
	@Test
	public void TestOCRNumber() throws Exception{
		BufferedImage bi = new BufferedImage(50, 25, BufferedImage.TYPE_INT_BGR);
		Graphics g = bi.getGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, 50, 25);
		g.setColor(Color.black);
		g.drawString("123", 5, 20);
		for(int i = 0; i < 3; i++) {
			long tm = System.currentTimeMillis();
			String result = OCRBridge.INST.ocrOneLineDigits(bi, new Rectangle(1, 1, 26, 26));
			System.out.println("OCR time=" + (System.currentTimeMillis() - tm));
			Assert.assertEquals(result, "123");
		}
	}
	@Test
	public void testPicFind() throws Exception{
		BufferedImage big = ImageIO.read(TestTrace.class.getClassLoader().getResourceAsStream("big.png"));
		BufferedImage part = ImageIO.read(TestTrace.class.getClassLoader().getResourceAsStream("sample.png"));
		
		ImageFinder fd = new ImageFinder(1, 1.2f, big.getWidth(), big.getHeight());
		Point pt = fd.findInPic(big, null, fd.img2Part(part, 0.8f));
		Assert.assertNotNull(pt);
		Assert.assertTrue(pt.x > 130 && pt.x < 140);
		Assert.assertTrue(pt.y > 120 && pt.y < 130);
	}
}
