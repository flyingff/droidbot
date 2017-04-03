package net.flyingff.framework.service;

import java.util.Iterator;

public interface IAnnouncementService {
	public Iterator<Activity> getCurrentTasks();
	public void pushActivity(Activity a);
	
	public static class Activity {
		public String description;
		public float percentage;
		public boolean indetermined, finished;
		public void finish() { finished = true; }
		public Activity(String desc) {
			this.description = desc;
			indetermined = true;
		}
		public Activity(String desc, float percent) {
			this.description = desc;
			indetermined = false;
			this.percentage = percent;
		}
	}
}
