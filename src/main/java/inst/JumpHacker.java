package inst;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.Supplier;

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
	private static int downX = 10, downY = 10;
	private static double msPerPixel = 5.5;
	public static void main(String[] args) throws Exception {
		ADBCommander cmd = new ADBCommander();

		BufferedImage first = cmd.capture(false, SCALE);
		
		DoubleConsumer jump = d -> {
			int tm = (int) (d * msPerPixel + 40);
			System.out.println("dist = " + (int)d + "px, hold time = " + tm + "ms");
			cmd.press(downX * SCALE, downY * SCALE, tm);
		};
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
					jump.accept(d);
				}
			}, e->{ });
		});
		Point last = null;
		@SuppressWarnings("unchecked")
		Supplier<Point>[] jumper = new Supplier[1];
		while(true) {
			BufferedImage img = cmd.capture(false, 4);
			if(img != null) {
				Point pt = Analyzer.markHead(img, jumper);
				boolean j;
				if(j = (pt != null && pt.equals(last))) {
					Point pTarget = jumper[0].get();
					jump.accept(pTarget.distance(pt));
				}
				last = pt;
				jumper[0] = null;
				
				EventQueue.invokeLater(()->{
					pf.setPic(img);
				});
				if(j) Thread.sleep(3000);
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
	private static final boolean[][] MARK = new boolean[270][540];
	private static final void clearMark() {
		for(int i = 0; i < 270; i++) {
			for(int j = 0; j < 540; j++) {
				MARK[i][j] = false;
			}
		}
		maxMX = maxMY = -1;
		minMX = minMY = 0xFFFF;
	}
	private static IntBinaryOperator dfsDeeper;
	private static int maxMX, minMX, maxMY, minMY;
	public static Point markHead(BufferedImage img, Supplier<Point>[] jumper) {
		int w = img.getWidth(), h = img.getHeight();
		int ref = 0x504977, refBG = img.getRGB(w - 1, h / 2);
		
		int threashold = 3;
		int minDist = 0xFFFF;
		
		int maxX = -1, maxY = -1, minX = 0xFFFF, minY = 0xFFFF;
		while(maxX == -1) {
			for(int x = w / 6, xTo = w * 5 / 6; x < xTo; x++) {
				for(int y = h / 3, yTo = h * 2 / 3; y < yTo; y++) {
					int dist = dist(ref, img.getRGB(x, y));
					if(dist <= threashold) {
						if(x > maxX) { maxX = x; }
						if(x < minX) { minX = x; }
						if(y > maxY) { maxY = y; }
						if(y < minY) { minY = y; }
					}
					if(dist < minDist) { minDist = dist; }
				}
			}
			threashold = minDist;
		}
		
		// DFS for full-body
		clearMark();
		dfsDeeper = (x, y) -> {
			int color = img.getRGB(x, y);
			for(int i = -1; i <= 1; i += 2) {
				for(int j = -1; j <= 1; j += 2) {
					int px = x + i, py = y + j;
					if(px < 0 || px >= 270 || py < 0 || py >= 540) continue;
					if(MARK[px][py]) continue;
					if(dist(color, img.getRGB(px, py)) <= 30) {
						MARK[px][py] = true;
						if(px > maxMX) { maxMX = px; }
						if(px < minMX) { minMX = px; }
						if(py > maxMY) { maxMY = py; }
						if(py < minMY) { minMY = py; }
						dfsDeeper.applyAsInt(px, py);
					}
				}
			}
			return 0;
		};
		dfsDeeper.applyAsInt(maxX, maxY);
		/*
		if(Math.abs((maxMX - minMX) - (maxMY - minMY)) < 8) {
			maxMY += 32;
		}
		*/
		
		Graphics g = img.getGraphics();
		g.setColor(Color.red);
		g.drawRect(minMX, minMY, maxMX - minMX + 1, maxMY - minMY + 1);
		g.dispose();
		
		final int peopleX = (maxMX + minMX) / 2;
		jumper[0] = () -> markTarget(img, 2, h / 4, w - 2, h * 2 / 3, refBG, peopleX);
		return null; // new Point((minMX + maxMX) / 2, maxMY - 6);
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
							// debug output
							// System.out.println(firstX + "," + peopleX + ", toLeft: " + left);
							System.out.print("Jump to " + (left ? "left" : "right") + ", ");
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
