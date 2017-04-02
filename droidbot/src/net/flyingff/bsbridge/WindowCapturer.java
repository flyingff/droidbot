package net.flyingff.bsbridge;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;

import net.flyingff.framework.IPulseSupplier;

public class WindowCapturer implements IPulseSupplier{
	private static final User32 u32 = User32.INSTANCE;
	private static final GDI32 g32 = GDI32.INSTANCE;
	private BufferedImage image;
	private int width, height;
	private HWND target;
	private HDC hdcTarget, hdcTargetMem;
	private HBITMAP hBitmap;
	private Memory buffer;
	private BITMAPINFO bmi;
	private int[] jbuf;
	
	public WindowCapturer(){
		this("BS2CHINAUI", "Bluestacks App Player", "_ctl.Window");
	}
	public WindowCapturer(String className, String title, String subWinTitle) {
		HWND hwnd = u32.FindWindow(className, title);
		if(subWinTitle == null) {
			target = hwnd;
		} else {
			char[] arr = new char[256];
			u32.EnumChildWindows(hwnd, (hchild, p)->{
				int len = u32.GetWindowText(hchild, arr, 255);
				String text = new String(arr, 0, len);
				if(subWinTitle.equals(text)) {
					target = hchild;
				}
				return true;
			}, Pointer.NULL);
		}
		
		u32.ShowWindow(hwnd, User32.SW_RESTORE);
		prepare();
	}
	
	private final void prepare() {
		RECT rect = new RECT();
		if (!u32.GetWindowRect(target, rect)) {
			throw new Win32Exception(Native.getLastError());
		}
		Rectangle jRectangle = rect.toRectangle();
		width = jRectangle.width;
		height = jRectangle.height;
		
		if (width == 0 || height == 0) {
			throw new IllegalStateException("Window width and/or height were 0 even though GetWindowRect did not appear to fail.");
		}
		hdcTarget = u32.GetDC(target);
		if (hdcTarget == null) {
			throw new Win32Exception(Native.getLastError());
		}
		
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		hdcTargetMem = g32.CreateCompatibleDC(hdcTarget);
		if (hdcTargetMem == null) {
			throw new Win32Exception(Native.getLastError());
		}

		hBitmap = g32.CreateCompatibleBitmap(hdcTarget, width, height);
		if (hBitmap == null) {
			throw new Win32Exception(Native.getLastError());
		}

		g32.SelectObject(hdcTargetMem, hBitmap);
		
		bmi = new BITMAPINFO();
		bmi.bmiHeader.biWidth = width;
		bmi.bmiHeader.biHeight = -height;
		bmi.bmiHeader.biPlanes = 1;
		bmi.bmiHeader.biBitCount = 32;
		bmi.bmiHeader.biCompression = WinGDI.BI_RGB;
		
		buffer = new Memory(width * height * 4);
		jbuf = new int[width * height];
	}
	
	public final BufferedImage capture() {
		// draw to the bitmap
		if (!g32.BitBlt(hdcTargetMem, 0, 0, width, height, hdcTarget, 0, 0, GDI32.SRCCOPY)) {
			throw new Win32Exception(Native.getLastError());
		}

		int resultOfDrawing = g32.GetDIBits(hdcTarget, hBitmap, 0, height, buffer, bmi,
				WinGDI.DIB_RGB_COLORS);
		if (resultOfDrawing == 0 || resultOfDrawing == WinError.ERROR_INVALID_PARAMETER) {
			throw new Win32Exception(Native.getLastError());
		}

		buffer.read(0, jbuf, 0, width*height);
		image.setRGB(0, 0, width, height, jbuf, 0,
				width);
		return image;
	}
	
	public final void dispose() {

		Win32Exception we = null;
		if (hBitmap != null) {
			if (!g32.DeleteObject(hBitmap)) {
				we = new Win32Exception(Native.getLastError());
			}
		}

		if (hdcTargetMem != null) {
			// get rid of the device context when done
			if (!g32.DeleteDC(hdcTargetMem)) {
				Win32Exception ex = new Win32Exception(Native.getLastError());
				if (we != null) {
					ex.addSuppressed(we);
				}
				we = ex;
			}
		}

		if (hdcTarget != null) {
			if (0 == u32.ReleaseDC(target, hdcTarget)) {
				throw new IllegalStateException("Device context did not release properly.");
			}
		}

		if (we != null) {
			throw we;
		}
	}
	
	private Thread th;
	private long lastTm = 0;
	private boolean running = false;
	public final void intervalCap(long interval, Consumer<BufferedImage> frameListener) {
		if(th != null)  throw new IllegalStateException("Already called.");
		th = new Thread(()->{
			while(running) {
				long now = System.currentTimeMillis();
				if(now - lastTm >= interval) {
					frameListener.accept(capture());
					lastTm = now;
				} else {
					try { Thread.sleep(1); } catch (Exception e) { }
				}
			}
		});
		th.setDaemon(false);
		th.start();
		running = true;
	}
	public void stop() {
		running = false;
	}
	public boolean isRunning() {
		return running;
	}
	
	public static void waitForWindow() {
		HWND handle = u32.FindWindow("BS2CHINAUI", "Bluestacks App Player");
		
		while(handle == null || WinBase.INVALID_HANDLE_VALUE.equals(handle)) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) { }
			handle = u32.FindWindow("BS2CHINAUI", "Bluestacks App Player");
		}
		// window may flash once, 
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) { }
	}
	@Override
	public Object getOne() {
		return image;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void getAtDesireRate(int millseconds, Consumer<Object> listener) {
		System.out.println("Log. Waiting for window...");
		waitForWindow();
		System.out.println("Log. Window detected.");
		intervalCap((long)millseconds, (Consumer<BufferedImage>)(Object)listener);
	}
}
