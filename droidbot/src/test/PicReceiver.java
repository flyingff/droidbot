package test;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class PicReceiver {
	private boolean running = true;
	private Socket currentSocket;
	
	private JFrame fr;
	private ImageIcon ic;
	
	public PicReceiver() {
		fr = new JFrame("Pic");
		fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		fr.setResizable(false);
		fr.add(new JLabel(ic = new ImageIcon()));
		fr.pack();
		fr.setLocationRelativeTo(null);
		fr.setVisible(true);
		
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(20086);
		} catch (IOException e1) { }
		while(running) try {
			currentSocket = ss.accept();
			System.out.println("Accept one:" + currentSocket.getRemoteSocketAddress());
			handleSocket();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(ss != null) try {
			ss.close();
		} catch (IOException e) { }
	}
	
	private void handleSocket() {
		try(DataInputStream is = new DataInputStream(currentSocket.getInputStream())) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buf = new byte[65536];
			while(true) {
				int size = is.readInt(), len;
				if(size == 0) break;
				bos.reset();
				while(size > 0 && (len = is.read(buf)) > 0) {
					bos.write(buf, 0, len);
					size -= len;
				}
				
				BufferedImage im = ImageIO.read(new ByteArrayInputStream(bos.toByteArray()));
				
				if(im == null) {
					System.out.println("Null at Image.");
					break;
				} else {
					System.out.println("New pic in.(" + im.getWidth() + "," + im.getHeight() + ")");
				}
				EventQueue.invokeLater(()->{
					ic.setImage(im);
					fr.validate();
					fr.repaint();
					fr.pack();
				});
			}
		}catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				currentSocket.close();
			} catch (IOException e) { }
		}
		
		System.out.println("Connection closed.");
	}
	
	public static void main(String[] args) {
		new PicReceiver();
	}
}
