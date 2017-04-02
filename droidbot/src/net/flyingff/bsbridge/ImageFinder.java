package net.flyingff.bsbridge;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.sun.jna.Library;
import com.sun.jna.Native;

import net.flyingff.util.TempFile;

public class ImageFinder {
	public static interface GaussianFilterLib extends Library {
		void gaussian (int[] dataIn, int[] buf, int rows, int cols, float[] serial, int serialSize);
		int find(int[] dataBig, int rowsBig, int colsBig, int[] dataSmall, int rowsSmall, int colsSmall, int threashold);
		
	}
	public static class PartOfImage {
		private int[] data;
		private int w, h;
		private float tolerance;
	}
	private static final GaussianFilterLib lib;
	static {
		GaussianFilterLib lb = null;
		try {
			File tmpDll = TempFile.createTempFile("__gaussian_filter__.dll");
			byte[] data = new byte[65536];
			int len;
			try(FileOutputStream fos = new FileOutputStream(tmpDll);
				InputStream is = GaussianFilterLib.class.getResourceAsStream("jna/GaussianFilter.dll")) {
				while((len = is.read(data)) > 0) {
					fos.write(data, 0, len);
				}
			}
			lb = Native.loadLibrary(tmpDll.getAbsolutePath(), GaussianFilterLib.class);
		} catch(Exception e) {
			e.printStackTrace();
		}
		lib = lb;
	}
	
	private final float[] gaussianTemp;
	//private final int w, h;
	private final int[] data, buf;
	public ImageFinder(int tempR, float sigma, int maxW, int maxH) {
		//this.w = maxW;
		//this.h = maxH;
		data = new int[maxW * maxH];
		buf = new int[data.length];
		gaussianTemp = new float[tempR * 2 + 1];
		fillGaussian(gaussianTemp, sigma);
	}
	private int[] gaussian(int[] arr, BufferedImage img, Rectangle rect) {
		int x, y, width, height;
		if(rect == null) {
			x = y = 0;
			width = img.getWidth();
			height = img.getHeight();
		} else {
			x = rect.x; y = rect.y;
			width = rect.width; height = rect.height;
		}
		img.getRGB(x, y, width, height, arr, 0, width);
		lib.gaussian(arr, buf, height, width, gaussianTemp, gaussianTemp.length);
		return arr;
	}
	
	public PartOfImage img2Part(BufferedImage img, float tolerance) {
		PartOfImage part = new PartOfImage();
		int w = img.getWidth(), h = img.getHeight();
		part.data = new int[w * h];
		img.getRGB(0, 0, w, h, part.data, 0, w);
		gaussian(part.data, img, null);
		part.tolerance = tolerance;
		part.w = w;
		part.h = h;
		return part;
	}
	public Point findInPic(BufferedImage screen, Rectangle rect, PartOfImage part) {
		if(rect == null) {
			rect = new Rectangle(0, 0, screen.getWidth(), screen.getHeight());
		}
		gaussian(data, screen, rect);
		int x = lib.find(data, rect.height, rect.width, part.data, part.h, part.w, (int) ((1 - part.tolerance) * 255));
		if(x == -1) {
			return null;
		} else {
			return new Point(x & 0xFFFF, (x >> 16) & 0xFFFF);
		}
	}

	public static void main(String[] args) throws Exception {
		BufferedImage big = ImageIO.read(new File("D:\\big.png"));
		BufferedImage sample = ImageIO.read(new File("D:\\sample.png"));
		ImageFinder ifd = new ImageFinder(1, 1.0f, 1920, 1080);
		PartOfImage token = ifd.img2Part(sample, 0.8f);
		
		long tmStart = System.currentTimeMillis();
		System.out.println(ifd.findInPic(big, null, token));

		System.out.println(System.currentTimeMillis() - tmStart);
	}
	
	private static void fillGaussian(float[] data, float sigma) {
		int center = data.length / 2;
		float sum = 0;
		for(int i = 0; i < data.length; i++) {
			sum += data[i] = (float) (1 / (sigma * Math.sqrt(2 * Math.PI)) * Math.exp(-sq(i - center) / (2 * sq(sigma))));
		}
		for(int i = 0; i < data.length; i++) {
			data[i] /= sum;
		}
	}
	private static final double sq(double x) {
		return x * x;
	}
}
