package inst;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.util.Date;

import net.flyingff.bsbridge.ADBCommander;
import net.flyingff.ui.PicFrame;

public class FrogTest {
	private static final String ACTIVITY_NAME = "net.gree.unitywebview.CUnityPlayerActivity";
	private static final String PACKAGE_NAME = "jp.co.hit_point.tabikaeru";
	
	private static PicFrame pf;
	public static void main(String[] args) throws Exception {
		ADBCommander ac = new ADBCommander();
		pf = new PicFrame(270, 540, ev->{
			System.out.printf("(%d, %d) -> #%x\n", ev.getX(), 
					ev.getY(), pf.getPic().getRGB(ev.getX(), ev.getY()) & 0xFFFFFF);
		}, null, ev->{
			System.out.printf("(%d, %d) -> #%x\n", ev.getX(), 
					ev.getY(), pf.getPic().getRGB(ev.getX(), ev.getY()) & 0xFFFFFF);
		});
		
		FrogHacker fh = new FrogHacker();
		boolean doGrassed = false;
		while(true) {
			System.out.println("Start game activity...");
			ac.launch(PACKAGE_NAME, ACTIVITY_NAME);
			
			doGrassed = false;
			// wait for game activity end
			while(ac.topActivity()[0].equals(PACKAGE_NAME)) {
				BufferedImage im = ac.capture2(4);
				fh.setImg(im);
				if(doGrassed) {
					System.out.println("Grass has already done.");
				} else {
					if(fh.isOkay()) {
						Thread.sleep(500);
						// slide to left
						fh.doGrass(ac);
						doGrassed = true;
					} else {
						// not 
						System.out.println("....");
					}
				}
				
				EventQueue.invokeAndWait(()->{
					pf.setPic(im);
				});
				Thread.sleep(500);
			}
			System.out.println("Game exit detected, modify time...");
			Thread.sleep(2000);
			ac.kill(PACKAGE_NAME);
			Date d = ac.readDate();
			ac.setDate(new Date(d.getTime() + 3600_000L * 3));
			Thread.sleep(500);
		}
		//System.out.println(ac.whoAmI());
		//System.out.println(ac.readDate());
		/*PicFrame pf = new PicFrame(270, 540, null, ev->{
			
		}, null);
		ac.captureViaVideo(270, 540, frame->{
			EventQueue.invokeLater(()->{
				pf.setPic(frame);
			});
		});
		*/
		
		// System.exit(0);
	}
	
}

class FrogHacker {
	private BufferedImage img;
	public void setImg(BufferedImage img) {
		this.img = img;
	}
	
	public boolean isOkay() {
		return (img.getRGB(30, 40) & 0xFFFFFF) == 0xE3DBC2;
	}
	public void doGrass(ADBCommander ac) {
		// slide to left
		ac.swipe(20, 400, 900, 400, 500);
		delay(1000);
		// do grass
		ac.swipe(18 * 4, 315 * 4, 230 * 4, 315 * 4, 1000);
		delay(1500);
		ac.swipe(18 * 4, 345 * 4, 230 * 4, 345 * 4, 1000);
		delay(1500);
		// slide to right
		ac.swipe(900, 400, 20, 400, 500);
		delay(500);
	}
	
	private void delay(long tm) {
		try { Thread.sleep(tm); } catch (InterruptedException e) { e.printStackTrace(); }
	}
}