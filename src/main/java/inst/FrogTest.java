package inst;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.flyingff.bsbridge.ADBCommander;
import net.flyingff.ui.PicFrame;

public class FrogTest {
	private static final String ACTIVITY_NAME = "net.gree.unitywebview.CUnityPlayerActivity";
	private static final String PACKAGE_NAME = "jp.co.hit_point.tabikaeru";
	private static final int FACTOR = 4;
	
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
		
		FrogHacker fh = new FrogHacker(ac);

		while(true) {
			System.out.println("Start game activity...");
			ac.launch(PACKAGE_NAME, ACTIVITY_NAME);
			
			if(!fh.handle(pf::setPic)) {
				System.out.println("Game ended by user, exit...");
				break;
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
		
		System.exit(0);
	}
	private static enum Stage {
		OPENED,				// 刚打开
		HANDLE_MESSAGES,	// 处理通知消息
		HANDLE_PICTURE,		// 处理新写真
		DO_GRASS,			// 摘草
		FEED_ANIMALS,		// 喂食小动物
		VIEW_MAIL, 			// 收信
		PREPARE_PACKAGE,	// 收拾背包
		DONE				// 能做的都做完了，该关系统了
	}
	private static class FrogHacker {
		private BufferedImage img;
		private ADBCommander ac;
		public FrogHacker(ADBCommander c) {
			ac = Objects.requireNonNull(c);
		}
		private boolean isGameForeground() {
			return ac.topActivity()[0].equals(PACKAGE_NAME);
		}
		private int colorAt(int x, int y) {
			return img.getRGB(x, y) & 0xFFFFFF;
		}
		private boolean hasNoDialog() {
			return (img.getRGB(30, 40) & 0xFFFFFF) == 0xE3DBC2;
		}
		private boolean hasMoreDialog() {
			return (img.getRGB(30, 40) & 0xFFFFFF) == 0x9F9A88;
		}
		private void swipe(int x0, int y0, int x1, int y1, int tm) {
			ac.swipe(x0 * FACTOR, y0 * FACTOR, x1 * FACTOR, y1 * FACTOR, tm);
		}
		private void tap(int x, int y) {
			ac.tap(x * FACTOR, y * FACTOR);
		}
		private void delay(long tm) {
			try { Thread.sleep(tm); } catch (InterruptedException e) { e.printStackTrace(); }
		}
		private boolean areaCheck(int x, int y, int r, int color) {
			if(r < 0) r = -r;
			color = color & 0xFFFFFF;
			for(int dx = -r + 1; dx < r; dx++) {
				for(int dy = -r + 1; dy < r; dy++) {
					if((img.getRGB(x + dx, y + dy) & 0xFFFFFF) != color) {
						return false;
					}
				}
			}
			return true;
		}
		
		
		public void doGrass() {
			// slide to left
			swipe(5, 100, 225, 100, 500);
			delay(1000);
			// do grass
			swipe(18, 315, 230, 315, 1000);
			delay(1500);
			swipe(18, 345, 230, 345, 1000);
			delay(1500);
			// slide to right
			swipe(225, 100, 5, 100, 500);
			delay(500);
		}

		
		public boolean handle(Consumer<BufferedImage> captureListener) {
			Runnable updateImage = () -> {
				img = ac.capture2(FACTOR);
				if(captureListener != null) {
					try {
						EventQueue.invokeAndWait(()->{
							captureListener.accept(img);
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			Stage stage = Stage.OPENED;
			boolean hasAnimal = false;
			// do only when game end
			while(isGameForeground()) {
				updateImage.run();
				switch(stage) {
				case OPENED:
					if(hasMoreDialog()) {
						stage = Stage.HANDLE_MESSAGES;
					} else if (hasNoDialog()) {
						stage = Stage.DO_GRASS;
					}
					break;
				case HANDLE_MESSAGES:
					if(hasNoDialog()) {
						stage = hasAnimal ? Stage.FEED_ANIMALS : Stage.DO_GRASS;
					} else {
						int msg = handleMessages();
						if(msg == 0) {
							System.out.println("有新的写真到来，请注意查收！");
							stage = Stage.HANDLE_PICTURE;
						} else if (msg == 1) {
							hasAnimal = true;
						}
					}
					break;
				case HANDLE_PICTURE:
					// wait until no dialog present
					if(hasNoDialog()) {
						System.out.println("完成写真保存。");
						stage = Stage.HANDLE_MESSAGES;
					}
					break;
				case FEED_ANIMALS:
					System.out.println("要给来访的小动物喂食了");
					// open feed window
					tap(170, 330);
					delay(1000);
					// randomly choose a food...
					int tryTimes = 20;
					while(tryTimes --> 0) {
						int x = ((int) (Math.random() * 4)) * 50 + 60,
								y  = ((int) (Math.random() * 5)) * 50 + 210;
						
						if(colorAt(x, y) == 0xe5dcc0) {
							continue;
						}
						tap(x, y);
						break;
					}
					delay(1000);
					if(tryTimes > 0) {
						// close dialog of feed
						tap(40, 40);
						delay(1000);
					}
					stage = Stage.DO_GRASS;
					break;
				case DO_GRASS:
					delay(200);
					doGrass();
					stage = Stage.VIEW_MAIL;
					break;
				case VIEW_MAIL:
					tap(250, 290);
					delay(1000);
					updateImage.run();
					if(colorAt(210, 300) == 0xFFFFFF) {
						System.out.println("收到了新的来信哦");
						tap(210, 300);
						delay(1500);
					} else {
						System.out.println("木有新的来信");
					}
					// close mail box
					tap(40, 40);
					delay(1500);
					stage = Stage.PREPARE_PACKAGE;
					break;
				case PREPARE_PACKAGE:
					if(colorAt(150, 225) == 0x89959e) {
						System.out.println("青蛙不在家，无需收拾行李~");
					} else {
						System.out.print("进屋...");
						tap(250, 500);
						delay(2000);
						System.out.println("买吃的...");
						buyFood();
						
						System.out.print("打开背包...");
						tap(50, 500);
						delay(1000);
						
						fillItem(updateImage, 87, 238, "食物");
						fillItem(updateImage, 180, 240, "护身符");
						fillItem(updateImage, 105, 320, "装备1");
						fillItem(updateImage, 160, 320, "装备2");
						System.out.println("收拾好了！");
						// confirm
						if(colorAt(130, 365) == 0xFFFFFF) {
							tap(130, 365);
							delay(1000);
						}
						tap(232, 160);
						delay(1000);
					}
					stage = Stage.DONE;
					break;
				case DONE:
					System.out.println("干完活了，可以退出了~");
					ac.back();
					delay(1000);
					tap(100, 310);
					while(isGameForeground()) {
						delay(500);
					}
					return true;
				default:
					System.out.println("Unknown stage: " + stage);
					return false;
				}
				
				delay(100);
			}
			// game ended due to user action
			return false;
		}
		
		/** return true means a new picture comes. */
		protected int handleMessages() {
			int color;
			switch(color = colorAt(20, 270)) {
			case 0xB2DDEB:
				// 带了特产回来
				System.out.println("青蛙带来了特产哦");
				tap(40, 40);
				tap(130, 400);
				delay(1000);
				return 0;
			case 0xCBEBA6:
				// 带了写真回来
				tap(40, 40);
				return 1;
			case 0xf7d179:
				System.out.println("有小伙伴来访啦");
				tap(40, 40);
				return 2;
			default: 
				System.out.printf("Unknown color: %x\n", color);
				return -1;
			}
		}
		
		private static final String[] FOOD_NAME = {
				"野葡萄烤饼 (10 草)",
				"巨石三明治 (20 草)",
				"南瓜百吉饼 (50 草)",
				"乳蛋饼     (80 草)",
				"艾蒿小麦饼 (100草)",
				"辣葱饼     (100草)"
		};
		private static final int[][] FOOD_POSITION = {
				{70, 185}, {200, 185},
				{70, 320}, {200, 320}
		};
		protected void buyFood() {
			int foodId = (int) (Math.random() * 6);
			delay(500);
			System.out.printf("买一个%s带路上吃吧~\n", FOOD_NAME[foodId]);
			// 点进商店
			tap(250, 470);
			delay(2000);
			if(foodId >= 4) {
				tap(250, 260);
				delay(1000);
				foodId -= 4;
			}
			int[] pt = FOOD_POSITION[foodId];
			tap(pt[0], pt[1]);
			delay(500);
			tap(pt[0], pt[1]);
			delay(1000);
			tap(93, 321);
			delay(1000);
			
			// back to house
			ac.back();
			delay(2000);
		}
		protected void fillItem(Runnable updateImage, int x, int y, String name) {
			updateImage.run();
			if(!areaCheck(x, y, 5, 0x9BD6EE)) {
				System.out.print("带上" + name + ".");
				tap(x, y);
				delay(1000);
				
				// swipe for a random distance
				System.out.print(".");
				double r = Math.random();
				if(r > 0.1) {
					swipe(140, 425, 140, 425 - (int) (r * 400), (int)(1000 * r));
				}
				delay(2000);
				
				// randomly choose an item, if present
				System.out.print(".");
				updateImage.run();
				List<Integer> pts = IntStream.range(0, 12)
						.mapToObj(Integer::valueOf)
						.collect(Collectors.toList());
				Collections.shuffle(pts);
				while(!pts.isEmpty()) {
					int posY = 185 + pts.get(0) * 20;
					if(colorAt(220, posY) == 0xFFE09E) {
						tap(220, posY);
						break;
					}
					pts.remove(0);
				}
				// no any item found? just close.
				if(pts.isEmpty()) {
					tap(236, 126);
				}
				
				delay(1000);
			}
		}
	}
	
}
