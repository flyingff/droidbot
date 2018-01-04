package inst;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.flyingff.bsbridge.ADBCommander;
import net.flyingff.ui.PicFrame;

public class JumpHacker {
	private static final int SCALE = 4;
	private static PicFrame pf;
	private static int downX, downY;
	private static double msPerPixel = 5.8;
	public static void main(String[] args) throws Exception {
		ADBCommander cmd = new ADBCommander();
		SliderFrame sf = new SliderFrame(msPerPixel, x->msPerPixel = x);
		
		BufferedImage first = cmd.capture(false, SCALE);
		EventQueue.invokeAndWait(()->{
			pf = new PicFrame(first.getWidth(), first.getHeight(),
			e->{ 
				downX = e.getX();
				downY = e.getY();
			}, e->{
				int dx = e.getX() - downX, dy = e.getY() - downY;
				double d = Math.sqrt(dx * dx + dy * dy);
				if(d < 10) {
					cmd.tap(downX * SCALE, downY * SCALE);
				} else {
					System.out.println("Time = " + d * msPerPixel + "ms");
					cmd.press(downX * SCALE, downY * SCALE, (int) (d * msPerPixel));
				}
			}, e->{ });
			
			Point posMainWindow = pf.getLocation();
			sf.setLocation(posMainWindow.x + pf.getWidth() + 5, posMainWindow.y);
			
			pf.addComponentListener(new ComponentAdapter() {

				@Override
				public void componentMoved(ComponentEvent e) {
					Point posMainWindow = pf.getLocation();
					sf.setLocation(posMainWindow.x + pf.getWidth() + 5, posMainWindow.y);
				}
			});
		});
		while(true) {
			BufferedImage img = cmd.capture(false, 4);
			if(img != null) {
				Analyzer.markHead(img);
				EventQueue.invokeLater(()->{
					pf.setPic(img);
				});
			} else {
				EventQueue.invokeLater(()->{
					pf.setPic(null);
				});
				System.err.println("Failed to capture...");
				Thread.sleep(1000);
			}
		}
	}
}

class SliderFrame extends JFrame {
	private static final long serialVersionUID = -3319464914283823579L;
	public SliderFrame(double value, Consumer<Double> accepter) {
		super("Ratio");
		JSlider slider = new JSlider(JSlider.VERTICAL, 200, 800, (int) (value * 100));
		slider.setPreferredSize(new Dimension(80, 480));
		add(slider,BorderLayout.CENTER);
		JLabel lb = new JLabel(String.valueOf(value));
		add(lb, BorderLayout.NORTH);
		
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				double v = slider.getValue() / 100.0;
				lb.setText(String.valueOf(v));
				accepter.accept(v);
			}
		});
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setVisible(true);
	}
}
class Analyzer {
	public static void markHead(BufferedImage img) {
		int w = img.getWidth(), h = img.getHeight();
		int ref = 0x504977, refBG = img.getRGB(w - 1, h / 2);
		
		int maxX = -1, maxY = -1, minX = 0xFFFF, minY = 0xFFFF;
		
		for(int x = w / 6, xTo = w * 5 / 6; x < xTo; x++) {
			for(int y = h / 3, yTo = h * 2 / 3; y < yTo; y++) {
				if(dist(ref, img.getRGB(x, y)) <= 3) {
					if(x > maxX) { maxX = x; }
					if(x < minX) { minX = x; }
					if(y > maxY) { maxY = y; }
					if(y < minY) { minY = y; }
				}
			}
		}
		if(maxX > 0) {
			markTarget(img, 2, h / 4, w - 2, h * 2 / 3, refBG, (maxX + minX) / 2);
		
			Graphics g = img.getGraphics();
			g.setColor(Color.red);
			g.drawRect(minX, minY, maxX - minX + 1, maxY - minY + 1);
			g.dispose();
		}
	}
	
	private static float[] v = new float[3], vBG = new float[3];
	public static Point markTarget(BufferedImage img, int minX, int minY, int maxX, int maxY, int refBG, int peopleX) {
		Color.RGBtoHSB(((refBG >> 16) & 0xFF), ((refBG >> 8) & 0xFF), (refBG & 0xFF), vBG);
		int firstX = -1, firstY = -1, firstXMax = -1, secondX = -1, secondY = -1;
		int firstToEqualY = -1, lastToEqualY = -1;
		boolean left = false;
		for(int y = minY; y <= maxY; y++) {
			int rowMinX = 0xFFFF, rowMaxX = -1;
			for(int x = minX; x <= maxX; x++) {
				int c = img.getRGB(x, y);
				Color.RGBtoHSB(((c >> 16) & 0xFF), ((c >> 8) & 0xFF), (c & 0xFF), v);
				
				if(!(vBG[2] - v[2] > -0.1f && 
						Math.abs(vBG[0] - v[0]) < 0.2f &&
						Math.abs(vBG[1] - v[1]) < 0.1f)) {
					if((firstX == -1 || y == firstY) && Math.abs(x - peopleX) > 18) {
						if(firstX == -1) {
							firstXMax = firstX = secondX = x;
							firstY = secondY = y;
							left = firstX < peopleX;
							// XXX debug output
							System.out.println(firstX + "," + peopleX + ", toLeft: " + left);
						} else {
							firstXMax = x;
						}
						img.setRGB(x, y, 0x0000FF);
					} else { 
						if((left && x < firstX && x + 4 < peopleX) ||
							(!left && x > firstXMax && x - 4 > peopleX)){
							if(x < rowMinX) rowMinX = x;
							if(x > rowMaxX) rowMaxX = x;
						}
						//img.setRGB(x, y, 0x00FF00);
					}
				}
			}
			if(firstY != -1 && y > firstY) {
				if(left && rowMinX <= secondX) {
					if(rowMinX == secondX) {
						lastToEqualY = y;
						if(firstToEqualY == -1) {
							firstToEqualY = y;
						}
					}
					secondX = rowMinX;
				} else if (!left && rowMaxX >= secondX) {
					if(rowMaxX == secondX) {
						lastToEqualY = y;
						if(firstToEqualY == -1) {
							firstToEqualY = y;
						}
					}
					secondX = rowMaxX;
				} else {break;};
				secondY = y;
			}
		}
		if(firstX != -1) {
			int targetX = (firstX + firstXMax) / 2, targetY;
			if(firstToEqualY != -1) {
				if(lastToEqualY - firstToEqualY > 10) {
					// considered as cubic
					targetY = firstToEqualY - 1;
				} else {
					// considered as round
					targetY = (firstToEqualY + lastToEqualY) / 2;
				}
			} else {
				targetY = secondY;
			}
			img.setRGB(targetX, targetY, 0xFF00FF);
			return new Point(targetX, targetY);
		}
		return null;
	}
	
	private static float[] hsv1 = new float[3];
	private static float[] hsv2 = new float[3];
	private static int dist(int rgb1, int rgb2) {
		Color.RGBtoHSB(((rgb1 >> 16) & 0xFF), ((rgb1 >> 8) & 0xFF), (rgb1 & 0xFF), hsv1);
		Color.RGBtoHSB(((rgb2 >> 16) & 0xFF), ((rgb2 >> 8) & 0xFF), (rgb2 & 0xFF), hsv2);
		
		int diff = (int) ((Math.abs(hsv1[0] - hsv2[0]) +
				Math.abs(hsv1[1] - hsv2[1]) + 
				Math.abs(hsv1[2] - hsv2[2])) * 255);
		return diff;
	}
}
