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
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.flyingff.bsbridge.ADBCommander;
import net.flyingff.ui.PicFrame;

public class JumpHacker {
	private static final int SCALE = 4;
	private static PicFrame pf;
	private static SliderFrame sfK, sfB;
	private static int downX = 10, downY = 10;
	private static Point last = null;
	private static final boolean AUTO = System.currentTimeMillis() > 0;
	private static long lastJumpTime = 0;
	private static double  lastd = 0;
	
	public static void main(String[] args) throws Exception {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		
		ADBCommander cmd = new ADBCommander();

		BufferedImage first = cmd.capture2(SCALE);
		
		DoubleBinaryOperator jump = (d, jitter) -> {
			double k = sfK.getValue(), b = sfB.getValue();
			
			int tm = (int) ((d + (Math.random() - 0.5) * jitter) * k + b);
			System.out.println("dist = " + (int)d + "px, \thold time = " + tm + "ms");
			cmd.press(downX * SCALE, downY * SCALE, tm);
			lastJumpTime = System.currentTimeMillis();
			return tm;
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
					if(last != null) {
						jump.applyAsDouble(new Point(e.getX(), e.getY()).distance(last), 0);
					} else {
						cmd.tap(downX * SCALE, downY * SCALE);
					}
				} else {
					jump.applyAsDouble(d, 0);
				}
			}, e->{ });
			sfK = new SliderFrame("K", 5.2, 6, 4, 2);
			sfK.toLeftOf(pf);
			sfB = new SliderFrame("B", 60, 80, 40, 0);
			sfB.toLeftOf(sfK);
		});
		
		
		while(true) {
			BufferedImage img = cmd.capture2(SCALE);
			if(img != null) {
				if(Analyzer.detectGameOn(img)) {
					FindInfo info = Analyzer.markPerson(img);
					if(info.peoplePos != null) {
						Point pTarget = null;
						if(info.exactPos == null &&
								AUTO &&
								System.currentTimeMillis() - lastJumpTime > 6000 &&
								info.peoplePos.equals(last)) {
							pTarget = info.finder.get();
						} else if(info.exactPos != null &&
								System.currentTimeMillis() - lastJumpTime > 2100){
							System.out.print("Jump To exact,\t");
							pTarget = info.exactPos;
						}
						
						if(pTarget != null) {
							double d = pTarget.distance(info.peoplePos);
							if(d == lastd) {							
								// draw point info
								Graphics g = img.getGraphics();
								g.setColor(Color.blue);
								g.fillOval(pTarget.x - 3, pTarget.y - 2, 6, 4);
								g.setColor(Color.red);
								g.fillOval(info.peoplePos.x - 3, info.peoplePos.y - 2, 6, 4);
								g.setColor(Color.yellow);
								g.drawLine(pTarget.x, pTarget.y, info.peoplePos.x, info.peoplePos.y);
								g.dispose();
								
								downX = (int) (info.peoplePos.x + Math.random() * 10);
								downY = (int) (info.peoplePos.y + Math.random() * 10);
								
								jump.applyAsDouble(d, 0);
							}
							lastd = d;
						}
					}
					last = info.peoplePos;
				} else {
					System.err.println("Game not detected");
					lastd = -1;
					last = null;
					lastJumpTime = 0;
				}
				EventQueue.invokeLater(()->{
					pf.setPic(img);
				});
				/*
				if(delay > 0) {
					Thread.sleep((long) (delay + 800 + Math.random() * ));
				}
				*/
				
				// if(j) Thread.sleep((long) (Math.random() * 1500));
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
	private int p;
	private double value, max, min;
	private JSlider slider;
	private JLabel lb;
	public SliderFrame(String name, double value, double max, double min, int precision) {
		super(name);
		p = (int) Math.pow(10, precision);
		this.max = max;
		this.min = min;
		slider = new JSlider(JSlider.VERTICAL, (int)(min * p), (int)(max * p),
				(int) (value * p));
		slider.setPreferredSize(new Dimension(80, 480));
		add(slider,BorderLayout.CENTER);
		lb = new JLabel(String.valueOf(value));
		add(lb, BorderLayout.NORTH);
		
		this.value = value;
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				SliderFrame.this.value = slider.getValue() / (double)p;
				lb.setText(String.valueOf(SliderFrame.this.value));
			}
		});
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setVisible(true);
	}
	public double getValue() {
		return value;
	}
	public void setValue(double value) {
		if(value > max || value < min) throw new RuntimeException("Out of range.");
		
		int v = (int) (value * p);
		this.value = value;
		slider.setValue(v);
		lb.setText(String.valueOf(value));
	}
	private void moveToLeftOf(JFrame frame) {
		Point loc = frame.getLocation();
		setLocation(loc.x + frame.getWidth() + 5, loc.y);
		/*Arrays.stream(getComponentListeners()).forEach(it->{
			it.componentMoved(null);
		});
		*/
	}
	public void toLeftOf(JFrame frame) {
		moveToLeftOf(frame);
		frame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				moveToLeftOf(frame);
			}
			@Override
			public void componentMoved(ComponentEvent e) {
				moveToLeftOf(frame);
			}
		});
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
		dfsArea = 0;
	}
	private static IntBinaryOperator dfsDeeper;
	private static int maxMX, minMX, maxMY, minMY, dfsArea = 0;
	public static FindInfo markPerson(BufferedImage img) {
		FindInfo info = new FindInfo();
		int w = img.getWidth(), h = img.getHeight();
		int ref = 0x504977, refBG = img.getRGB(w - 1, h / 2);
		
		int threashold = 3;
		int minDist = 0xFFFF;
		
		int maxX = -1, maxY = -1, minX = 0xFFFF, minY = 0xFFFF, exactY = -1, exactX = -1;
		while(maxX == -1) {
			for(int y = h * 2 / 3 - 1, yTo = h / 3; y >= yTo; y--) {
				int lastExactLeft = -1, lastExactRight = -1;
				for(int x = w / 6, xTo = w * 5 / 6; x < xTo; x++) {
					int rgb = img.getRGB(x, y) & 0xFFFFFF;
					int dist = dist(ref, rgb);
					if(dist <= threashold) {
						if(x > maxX) { maxX = x; }
						if(x < minX) { minX = x; }
						if(y > maxY) { maxY = y; }
						if(y < minY) { minY = y; }
					}
					if(dist < minDist) { minDist = dist; }
					if(rgb == 0xF5F5F5) {
						if(lastExactRight == x - 1) {
							lastExactRight = x;
						} else {
							lastExactLeft = lastExactRight = x;
						}
						img.setRGB(x, y, 0xFF00FF);
					}
				}
				int d = lastExactRight - lastExactLeft;
				if(8 <= d && d <= 9) {
					exactY = y;
					exactX = (lastExactLeft + lastExactRight) / 2;
				}
			}
			threashold = minDist;
		}
		
		if(exactX > 0) {
			info.exactPos = new Point(exactX, exactY);
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
						dfsArea++;
					}
				}
			}
			return 0;
		};
		try {
		dfsDeeper.applyAsInt(maxX, maxY);
		} catch (StackOverflowError e) {
			System.err.println("Stack overflow when DFS");
			return info;
		}
		if(dfsArea < 10) return info;
		
		if(Math.abs((maxMX - minMX) - (maxMY - minMY)) < 8) {
			maxMY += 36;
		}
		
		Graphics g = img.getGraphics();
		g.setColor(Color.red);
		g.drawRect(minMX, minMY, maxMX - minMX + 1, maxMY - minMY + 1);
		g.dispose();
		
		final int peopleX = (maxMX + minMX) / 2;
		info.finder = () -> markTarget(img, 2, h / 4, w - 2, h * 2 / 3, refBG, peopleX);
		
		info.peoplePos = new Point(peopleX, maxMY - 2);
		return info;
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
							System.out.print("Jump to " + (left ? "left" : "right") + ",\t");
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
	public static boolean detectGameOn(BufferedImage img) {
		int w = img.getWidth();
		int ref = img.getRGB(w / 2, 36) & 0xFFFFFF;
		// bright > 128
		if (Math.max(Math.max(((ref >> 16) & 0xFF), ((ref >> 8) & 0xFF)),
				(ref & 0xFF)) < 128) {
			return false;
		}
		// same color at line
		for(int i = 0; i < w; i++) {
			if((img.getRGB(i, 36) & 0xFFFFFF) != ref) {
				return false;
			}
		}
		return true;
	}
}
class FindInfo {
	public Point peoplePos;
	public Point exactPos;
	public Supplier<Point> finder;
}
