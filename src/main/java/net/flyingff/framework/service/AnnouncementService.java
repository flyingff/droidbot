package net.flyingff.framework.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class AnnouncementService implements IAnnouncementService {
	private Deque<Activity> activities = new ArrayDeque<>();
	@Override
	public void pushActivity(Activity a) {
		activities.push(a);
	}

	@Override
	public Iterator<Activity> getCurrentTasks() {
		Iterator<Activity> it = activities.descendingIterator();
		while(it.hasNext()) {
			if(it.next().finished) {
				it.remove();
			} else {
				break;
			}
		}
		
		// return a copy of current tasks
		return new ArrayDeque<>(activities).iterator();
	}

}
