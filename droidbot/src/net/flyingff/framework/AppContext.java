package net.flyingff.framework;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import net.flyingff.bsbridge.WindowCapturer;
import net.flyingff.framework.fsm.AbstractFinateStateMachine;

public class AppContext implements IAppContext {
	public static final String STARTER = "STARTER";
	private Map<String, Class<? extends AbstractFinateStateMachine>> fsmClasses = new HashMap<>();
	private Map<Class<?>, Object> servicesByType = new HashMap<>();
	private Map<String, Object> servicesByName = new HashMap<>();
	private Deque<AbstractFinateStateMachine> fsmStack = new ArrayDeque<>();
	
	private WindowCapturer capturer;
	
	public AppContext() {
		Properties pFSM = new Properties();
		String starter = null;
		try {
			pFSM.load(AppContext.class.getResourceAsStream("fsm.properties"));
			for(Object o : pFSM.keySet()) {
				String name = (String) o;
				if(STARTER.equals(name)) {
					starter = pFSM.getProperty(name);
					continue;
				}
				String className = pFSM.getProperty(name);
				@SuppressWarnings("unchecked")
				Class<? extends AbstractFinateStateMachine> clazz = (Class<? extends AbstractFinateStateMachine>) Class.forName(className);
				if (!AbstractFinateStateMachine.class.isAssignableFrom(clazz)){
					throw new RuntimeException("Class must be child of AbstractFinateStateMachine: " + className);
				}
				fsmClasses.put(name, clazz);
			}
			
		} catch (Exception e) { throw new RuntimeException(e); }
	
		if(starter == null) {
			throw new RuntimeException("Need a starter");
		}
		
		fsmStack.push(newFSM(starter));
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getServices(Class<? super T> clazz) {
		return (T)servicesByType.get(clazz);
	}
	@SuppressWarnings("unchecked")
	public <T> T getServices(String name) {
		return (T)servicesByType.get(name);
	}
	public <T> void registService(Class<? super T> clazz, T obj) {
		servicesByType.put(clazz, obj);
	}
	public <T> void registService(String name, T obj) {
		servicesByName.put(name, obj);
	}
	
	private <T> void injectObject(Class<?> clazz, T object) {
		if(!clazz.isInstance(object)) {
			throw new RuntimeException(object + " is not a instance of " + clazz);
		}
		try {
			for(Field field : clazz.getDeclaredFields()) {
				if(field.getAnnotation(Inject.class) != null) {
					field.setAccessible(true);
					Named named = field.getAnnotation(Named.class);
					Object toInj = null;
					if(named == null) {
						toInj = servicesByType.get(field.getType());
					} else {
						toInj = servicesByName.get(named.value());
					}
					if(toInj == null && field.getAnnotation(Optional.class) == null) {
						throw new RuntimeException("Cannot find inject object for " + field);
					}
					field.set(object, toInj);
				}
			}
		} catch(Exception e) {
			throw new RuntimeException("Failed to inject object of " + clazz.getName(), e);
		}
	}
	private AbstractFinateStateMachine newFSM(String name) {
		try {
			Class<? extends AbstractFinateStateMachine> clazz = Objects.requireNonNull(fsmClasses.get(name), "No such FSM:" + name);
			AbstractFinateStateMachine afsm = clazz.getConstructor(new Class[]{IAppContext.class}).newInstance(this);
			injectObject(clazz, afsm);
			return afsm;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private BufferedImage screen;
	public void start() {
		if(capturer != null) {
			throw new IllegalStateException();
		}
		// wait for window
		WindowCapturer.waitForWindow();
		
		// start capture
		capturer = new WindowCapturer();
		capturer.intervalCap(40, img->{
			screen = img;
			active();
		});
	}
	public BufferedImage getScreen() {
		return screen;
	}
	public void active() {
		boolean running = true;
		while(running) {
			AbstractFinateStateMachine fsm = fsmStack.peek();
			running = fsm.active();
			// empty stack, if pop just executed
			if(fsmStack.isEmpty()) {
				terminate();
				break;
			}
		}
	}
	public void push(String next, AbstractFinateStateMachine executor) {
		if(fsmStack.isEmpty()){
			throw new IllegalStateException("Cannot push when stack is Empty; use 'swap' instead.");
		} else if (fsmStack.peek() != executor) {
			throw new IllegalStateException("Non top-level FSM cannot perform push.");
		} else {
			fsmStack.push(newFSM(next));
		}
	}
	public void pop(AbstractFinateStateMachine executor) {
		if(fsmStack.isEmpty()) {
			throw new IllegalStateException("Cannot pop empty stack.");
		} else if (fsmStack.peek() != executor) {
			throw new IllegalStateException("Cannot pop other FSM.");
		} else {
			fsmStack.pop();
		}
	}
	public void swap(String next, AbstractFinateStateMachine executor) {
		if(fsmStack.isEmpty()) {
			throw new IllegalStateException("Cannot swap when empty stack.");
		} else if (fsmStack.peek() != executor) {
			throw new IllegalStateException("Non top-level FSM cannot perform swap.");
		} else {
			AbstractFinateStateMachine fsm = newFSM(next);
			fsmStack.pop();
			fsmStack.push(fsm);
		}
	}
	
	private void terminate() {
		System.out.println("All stacks poped. Program will exit");
		capturer.stop();
	}
	
	public static void main(String[] args) {
		AppContext context = new AppContext();
		System.out.println("Waiting for window...");
		context.start();
		System.out.println("Message loop started.");
	}
}
