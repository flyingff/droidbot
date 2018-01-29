package net.flyingff.bsbridge;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.RawImage;
import com.twilight.h264.player.H264StreamDecoder;

import net.flyingff.framework.ICommander;

public class ADBCommander implements ICommander, IShellOutputReceiver {
	private final IDevice device;
	
	private static void waitDeviceList(AndroidDebugBridge bridge) {
		int count = 0;
		for (; count < 300 && bridge.hasInitialDeviceList() == false; count++) {
			try { Thread.sleep(100); count++; } catch (InterruptedException e) { }
		}
		if(count >= 300) {
			throw new RuntimeException("Timeout when waiting for device list");
		}
	}
	
	public ADBCommander() {
		try {
			// make sure start-up adb
			Runtime.getRuntime().exec("adb start-server").waitFor();
			
			AndroidDebugBridge.initIfNeeded(true);
			AndroidDebugBridge bridge = AndroidDebugBridge.createBridge();
			
			waitDeviceList(bridge);
			
			IDevice devices[] = bridge.getDevices();
			if(devices == null || devices.length <= 0) {
				throw new RuntimeException("No device detected.");
			}
			device = devices[0];
			System.out.println("Connected, device is " + device.getName());
		} catch(Exception e) {
			if(e instanceof RuntimeException) {
				throw (RuntimeException)e;
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	public void tap(int x, int y) {
		execf("input tap %d %d", x, y);
	}
	public void press(int x, int y, int tm) {
		execf("input swipe %d %d %d %d %d", x, y, x, y, tm);
	}
	public void swipe(int x0, int y0, int x1, int y1, int tm) {
		execf("input swipe %d %d %d %d %d", x0, y0, x1, y1, tm);
	}
	public void back() {
		exec("input keyevent 4");
	}
	public void home() {
		exec("input keyevent 3");
	}
	public void power() {
		exec("input keyevent 26");
	}
	public BufferedImage capture(boolean landscape, int sample) {
		try {
			RawImage raw = device.getScreenshot();

			if (raw == null) {
				return null;
			}
			
			int w = (landscape ? raw.height : raw.width) / sample;
			int h = (landscape ? raw.width : raw.height) / sample;
			
			BufferedImage ret = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
			
			int index = 0;
			int indexInc = raw.bpp >> 3;
			if (landscape) {
				for (int y = 0; y < raw.height; y += sample) {
					for (int x = 0; x < raw.width; x += sample, index += indexInc * sample) {
						ret.setRGB(y / sample, (raw.width - x - 1) / sample, raw.getARGB(index));
					}
					index += indexInc * raw.width * (sample - 1);
				}
			} else {
				for (int y = 0; y < raw.height; y += sample) {
					for (int x = 0; x < raw.width; x += sample, index += indexInc * sample) {
						ret.setRGB(x / sample, y / sample, raw.getARGB(index));
					}
					index += indexInc * raw.width * (sample - 1);
				}
			}
			return ret;
		} catch (Exception e) {
			return null;
		}
	}
	
	private ThreadLocal<ByteBuffer> buffers = new ThreadLocal<ByteBuffer>() {
		@Override
		protected ByteBuffer initialValue() {
			// 4M bytes per buffer, which is quiet sufficient.
			return ByteBuffer.allocate(1024 *1024 * 4);
		}
	};
	private IShellOutputReceiver capReceiver = new IShellOutputReceiver() {
		@Override public boolean isCancelled() { return false; }
		@Override public void flush() { }
		@Override public void addOutput(byte[] data, int offset, int length) {
			buffers.get().put(data, offset, length);
		}
	};
	public BufferedImage capture2(int scale) {
		try {
			ByteBuffer buf = buffers.get();
			buf.clear();
			device.executeShellCommand("screencap -p", capReceiver);
			buf.flip();
			BufferedImage img = ImageIO.read(
					new ByteArrayInputStream(buf.array(), 0, buf.limit()));
			if(img != null && scale > 1) {
				int w = img.getWidth() / scale, h = img.getHeight() / scale;
				BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
				Graphics g = scaled.getGraphics();
				g.drawImage(img, 0, 0, w, h, 0, 0, w * scale, h * scale, null);
				return scaled;
			}
			return img;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private ThreadLocal<IShellOutputReceiver> receiver = new ThreadLocal<>();
	public void captureViaVideo(int w, int h, Consumer<BufferedImage> listener) {
		try (PipedOutputStream pos = new PipedOutputStream();
			PipedInputStream pis = new PipedInputStream(pos);){
			String cmd = String.format(
					"screenrecord --size %dx%d --time-limit %d --output-format=h264 -",
					w, h, 180);
			new Thread(new H264StreamDecoder(pis, origin->{
				if(origin.getWidth() != w || origin.getHeight() != h) {
					BufferedImage imgNew = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
					Graphics g = imgNew.getGraphics();
					g.drawImage(origin, 0, 0, w, h, 0, 0, w, h, null);
					g.dispose();
					listener.accept(imgNew);
				} else {
					listener.accept(origin);
				}
			}, true)).start();
			
			receiver.set(new IShellOutputReceiver() {
				@Override
				public boolean isCancelled() {
					return false;
				}
				@Override
				public void flush() { }
				@Override
				public void addOutput(byte[] data, int offset, int length) {
					try {
						pos.write(data, offset, length);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			while(true) {
				device.executeShellCommand(cmd, receiver.get(), 16, TimeUnit.SECONDS);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void addOutput(byte[] data, int offset, int length) {
		System.out.write(data, offset, length);
	}
	@Override
	public void flush() {
		System.out.flush();
	}
	@Override
	public boolean isCancelled() {
		return false;
	}
	
	private class TextResopnseCollector implements IShellOutputReceiver {
		private ByteArrayOutputStream bos = new ByteArrayOutputStream();
		private String str;
		@Override
		public void addOutput(byte[] data, int offset, int length) {
			bos.write(data, offset, length);
		}
		@Override
		public void flush() {
			try {
				str = bos.toString("utf-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			bos.reset();
		}
		@Override
		public boolean isCancelled() { return false; }
		public String get() {
			return str;
		}
	}
	private ThreadLocal<TextResopnseCollector> respCollector = new ThreadLocal<TextResopnseCollector>() {
		@Override protected TextResopnseCollector initialValue() { return new TextResopnseCollector(); }
	};
	
	private void execf(String cmd, Object... args) {
		exec(String.format(cmd, args));
	}
	@SuppressWarnings("unused")
	private String execfForResult(String cmd, Object... args) {
		return execForResult(String.format(cmd, args));
	}
	private void exec(String cmd) {
		try {
			device.executeShellCommand(cmd, this, 20, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	private String execForResult(String cmd) {
		try {
			TextResopnseCollector coll;
			device.executeShellCommand(cmd, coll = respCollector.get(), 20, TimeUnit.SECONDS);
			return coll.get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public String whoAmI() {
		return execForResult("whoami");
	}
	public Date readDate() {
		String unix = execForResult("date +%s");
		return new Date(Long.parseLong(unix.replaceAll("[\\r\\n\\\"]", "")) * 1000);
	}
	private static final SimpleDateFormat DATE_SET_FORMATTER = new SimpleDateFormat("MMddHHmmyyyy.ss");
	public void setDate(Date d) {
		execf("su -c date %s", DATE_SET_FORMATTER.format(d));
	}
	public void launch(String pkg, String className) {
		exec("am start " + pkg + "/" + className);
	}

	public void kill(String pkg) {
		exec("am force-stop " + pkg);
	}
	
	private static final Pattern PATTERN_TOP_ACTIVITY = Pattern.compile("topActivity=ComponentInfo\\{([^/]+)/([^/]+)\\}");
	public String[] topActivity() {
		Matcher mt = PATTERN_TOP_ACTIVITY.matcher(execForResult("am stack list"));
		if(mt.find()) {
			return new String[] {
				mt.group(1),
				mt.group(2)
			};
		} else {
			return null;
		}
	}
}

