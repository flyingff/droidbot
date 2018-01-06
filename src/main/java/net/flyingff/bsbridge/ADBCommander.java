package net.flyingff.bsbridge;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.RawImage;

import net.flyingff.framework.ICommander;

public class ADBCommander implements ICommander, IShellOutputReceiver {
	private static final int OP_TAP = 1, OP_SWIPE = 2;
	private static void waitDeviceList(AndroidDebugBridge bridge) {
		int count = 0;
		for (; count < 300 && bridge.hasInitialDeviceList() == false; count++) {
			try { Thread.sleep(100); count++; } catch (InterruptedException e) { }
		}
		if(count >= 300) {
			System.err.println("Log. Device: Timeout");
		}
	}
	private static class TouchOperation {
		public int x, y, x1, y1, op, tm;

		private TouchOperation(int x, int y) {
			this.x = x;
			this.y = y;
			this.op = OP_TAP;
		}
		private TouchOperation(int x, int y, int x1, int y1, int tm) {
			this.x = x;
			this.y = y;
			this.x1 = x1;
			this.y1 = y1;
			this.tm = tm;

			this.op = OP_SWIPE;
		}
	}
	
	private List<TouchOperation> operations = new LinkedList<>();
	private IDevice device;
	
	public ADBCommander() {
		try {
			// make sure start-up adb
			Runtime.getRuntime().exec("adb start-server").waitFor();
			
			AndroidDebugBridge.initIfNeeded(true);
			AndroidDebugBridge bridge = AndroidDebugBridge.createBridge();
			
			waitDeviceList(bridge);
			
			IDevice devices[] = bridge.getDevices();
			if(devices == null || devices.length <= 0) {
				throw new RuntimeException("No device connected.");
			}
			device = devices[0];
			System.out.println("Log. Device=" + device.getName());
			
			new Thread(this::messageLoop).start();
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private void messageLoop() {
		try {
			List<TouchOperation> ops = new ArrayList<>();
			for(;;) {
				ops.clear();
				// seek
				synchronized (operations) {
					if(operations.isEmpty()) {
						try {
							operations.wait();
						} catch (InterruptedException e) { }
					}
					if(operations.isEmpty()) {
						continue;
					} else {
						ops.addAll(operations);
						operations.clear();
					}
				}
				// execute tasks in order
				for(TouchOperation op : ops) {
					switch(op.op) {
					case OP_SWIPE:
						device.executeShellCommand(String.format("input swipe %d %d %d %d %d",
								op.x, op.y, op.x1, op.y1, op.tm), this);
						break;
					case OP_TAP:
						device.executeShellCommand(String.format("input tap %d %d",
								op.x, op.y), this);
						break;
						default: throw new AssertionError(op.op);
					}
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	public void tap(int x, int y) {
		synchronized (operations) {
			operations.add(new TouchOperation(x, y));
			operations.notify();
		}
	}
	public void press(int x, int y, int tm) {
		synchronized (operations) {
			operations.add(new TouchOperation(x, y, x, y, tm));
			operations.notify();
		}
	}
	public void swipe(int x0, int y0, int x1, int y1, int tm) {
		synchronized (operations) {
			operations.add(new TouchOperation(x0, y0, x1, y1, tm));
			operations.notify();
		}
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
}

