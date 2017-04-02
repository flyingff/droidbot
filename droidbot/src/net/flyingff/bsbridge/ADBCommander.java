package net.flyingff.bsbridge;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;

public class ADBCommander {
	private static final int OP_DOWN = 1, OP_MOVE = 3, OP_UP = 2;
	private static void waitDeviceList(AndroidDebugBridge bridge) {
		int count = 0;
		for (; count < 300 && bridge.hasInitialDeviceList() == false; count++) {
			try { Thread.sleep(100); count++; } catch (InterruptedException e) { }
		}
		if(count >= 300) {
			System.err.println("Log. Device: Timeout");
		}
	}
	private static class PtOperation {
		public int x, y, op;

		public PtOperation(int x, int y, int op) {
			super();
			this.x = x;
			this.y = y;
			this.op = op;
		}
	}
	
	private List<PtOperation> operations = new LinkedList<>();
	private IDevice device;
	
	public ADBCommander() {
		try {
			// start-up adb
			Runtime.getRuntime().exec("adb start-server").waitFor();
			
			AndroidDebugBridge.initIfNeeded(true);
			AndroidDebugBridge bridge = AndroidDebugBridge.createBridge();
			
			waitDeviceList(bridge);
			
			IDevice devices[] = bridge.getDevices();
			device = devices[0];
			System.out.println("Log. Device=" + device.getName());
			
			new Thread(this::messageLoop).start();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private File fKey;
	private void messageLoop() {
		RandomAccessFile raf = null;
		try {
			fKey = File.createTempFile("key", "");
			fKey.deleteOnExit();
		} catch (Exception e) { throw new RuntimeException(e); }
		
		// DEBUG
		// System.out.println("key=" + fKey);
		
		String fPath = fKey.getAbsolutePath();
		ByteBuffer buffer = ByteBuffer.allocate(65536);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		try {
			raf = new RandomAccessFile(fPath, "rw");
			List<PtOperation> ops = new ArrayList<>();
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
				/*   
				 *  // down 
				 *  cmds.add("sendevent /dev/input/event8 3 53 " + op.x * 0x7fff / width);
						cmds.add("sendevent /dev/input/event8 3 54 " + op.y * 0x7fff / height);
						cmds.add("sendevent /dev/input/event8 0 2 0");
						cmds.add("sendevent /dev/input/event8 0 0 0");
						// up
						cmds.add("sendevent /dev/input/event8 0 2 0");
						cmds.add("sendevent /dev/input/event8 0 0 0");
				 */
				// execute tasks in order
				buffer.clear();
				for(PtOperation op : ops) {
					switch(op.op) {
					case OP_DOWN:
					case OP_MOVE:
						buffer.putInt(0); buffer.putInt(0);
						buffer.putShort((short)3);buffer.putShort((short)53); buffer.putInt(op.x);
						buffer.putInt(0); buffer.putInt(0);
						buffer.putShort((short)3);buffer.putShort((short)54); buffer.putInt(op.y);
					case OP_UP:
						buffer.putInt(0); buffer.putInt(0);
						buffer.putShort((short)0);buffer.putShort((short)2); buffer.putInt(0);
						buffer.putInt(0); buffer.putInt(0);
						buffer.putShort((short)0);buffer.putShort((short)0); buffer.putInt(0);
						break;
					}
				}
				int len = buffer.position();
				raf.setLength(len);
				raf.seek(0);
				raf.write(buffer.array(), 0, len);
				device.pushFile(fPath, "/dev/input/event8");
			}
		}catch(Exception e) {
			e.printStackTrace();
			if(raf != null) try { raf.close(); } catch (IOException e1) { }
		}
	}
	
	public void mouseDown(int x, int y) {
		synchronized (operations) {
			operations.add(new PtOperation(x, y, OP_DOWN));
			operations.notify();
		}
	}
	public void mouseUp(int x, int y) {
		synchronized (operations) {
			operations.add(new PtOperation(x, y, OP_UP));
			operations.notify();
		}
	}
	public void mouseMove(int x, int y) {
		synchronized (operations) {
			operations.add(new PtOperation(x, y, OP_MOVE));
			operations.notify();
		}
	}
	
	/*public static void main(String[] args) throws Exception {
		RandomAccessFile raf = new RandomAccessFile("d:\\key", "rw");
		MappedByteBuffer buf = raf.getChannel().map(MapMode.READ_WRITE, 0, 96);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		for(int i = 0; i < 300; i++) {
			long tmStart = System.currentTimeMillis();
			buf.putShort(8, (short) 3);
			buf.putShort(10, (short) 53);
			buf.putInt(12, 2457);
			
			buf.putShort(24, (short) 3);
			buf.putShort(26, (short) 54);
			buf.putInt(28, 22572);
			
			buf.putShort(40, (short) 0);
			buf.putShort(42, (short) 2);
			buf.putInt(44, 0);
			
			buf.putShort(56, (short) 0);
			buf.putShort(58, (short) 0);
			buf.putInt(60, 0);
			
			buf.putShort(72, (short) 0);
			buf.putShort(74, (short) 2);
			buf.putInt(76, 0);
			
			buf.putShort(88, (short) 0);
			buf.putShort(90, (short) 0);
			buf.putInt(92, 0);
			
			System.out.println(System.currentTimeMillis() - tmStart);
			Thread.sleep(1000);
		}
		raf.close();
	}*/
}

