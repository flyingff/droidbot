package net.flyingff.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class PicFrame extends JFrame{
	private static final long serialVersionUID = 1L;
	private final ImageIcon ic;
	private final BufferedImage imgBlack;
	public PicFrame(int w, int h, Consumer<MouseEvent> onMouseDown, Consumer<MouseEvent> onMouseUp, Consumer<MouseEvent> onMouseDrag) {
		super("Screen");
		imgBlack = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		Graphics g = imgBlack.getGraphics();
		g.setColor(Color.black);
		g.fillRect(0, 0, w, h);
		g.setColor(Color.WHITE);
		g.drawString("No Image", 10, 10);
		
		setResizable(false);
		JLabel lb = new JLabel(ic = new ImageIcon(imgBlack));
		lb.setPreferredSize(new Dimension(w, h));
		add(lb, BorderLayout.CENTER);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		MouseAdapter ma = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				onMouseDown.accept(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				onMouseUp.accept(e);
			}
			@Override
			public void mouseDragged(MouseEvent e) {
				onMouseDrag.accept(e);
			}
		};
		lb.addMouseListener(ma);
		lb.addMouseMotionListener(ma);
		
	}
	public void setPic(BufferedImage im) {
		if(im == null) {
			ic.setImage(imgBlack);
		} else {
			ic.setImage(im);
		}
		repaint();
	}
}