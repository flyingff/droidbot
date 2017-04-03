package net.flyingff.ui;

import java.awt.BorderLayout;
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
	private BufferedImage im = new BufferedImage(800, 450, BufferedImage.TYPE_3BYTE_BGR);
	private Graphics g = im.getGraphics();
	public PicFrame(Consumer<MouseEvent> onMouseDown, Consumer<MouseEvent> onMouseUp, Consumer<MouseEvent> onMouseDrag) {
		super("Pic frame");
		setResizable(false);
		JLabel lb = new JLabel(new ImageIcon(im));
		lb.setPreferredSize(new Dimension(800, 450));
		add(lb, BorderLayout.CENTER);
		setLocationRelativeTo(null);
		pack();
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
		if(im != null) {
			g.drawImage(im, 0, 0, 800, 450, null);
			repaint();
		}
	}
}