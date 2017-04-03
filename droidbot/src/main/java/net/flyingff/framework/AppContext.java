package net.flyingff.framework;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import com.sun.jna.Platform;

import net.flyingff.framework.fsm.AbstractFinateStateMachine;

public class AppContext implements IAppContext {
	private Map<String, Class<? extends AbstractFinateStateMachine>> fsmClasses = new HashMap<>();
	private Map<Class<?>, Object> servicesByType = new HashMap<>();
	private Map<String, Object> servicesByName = new HashMap<>();
	private Deque<AbstractFinateStateMachine> fsmStack = new ArrayDeque<>();
	
	private IPulseSupplier supplier;
	private String supplierName;
	
	private Object pulse;

	public AppContext() {
		Properties pFSM = new Properties();
		String starter = null;
		try {
			String fsmPropName;
			if(Platform.isWindows()){
				fsmPropName = PROPNAME_WIN32;
			} else if(Platform.isMac()){
				fsmPropName = PROPNAME_MACOS;
			} else {
				throw new RuntimeException("Platform not supported: " + Platform.getOSType() + "(platform code in JNA)");
			}
			pFSM.load(AppContext.class.getClassLoader().getResourceAsStream(fsmPropName));
			for(Object o : pFSM.keySet()) {
				String name = (String) o;
				if(STARTER.equals(name)) {
					starter = pFSM.getProperty(name);
					continue;
				} else if (PULSESUPPLIER.equals(name)) {
					supplierName = pFSM.getProperty(name);
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
		if(supplierName == null) {
			throw new RuntimeException("Need a pulse supplier");
		}
		fsmStack.push(newFSM(starter));
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getServices(Class<? super T> clazz) {
		return (T)servicesByType.get(clazz);
	}
	@SuppressWarnings("unchecked")
	public <T> T getServices(String name) {
		return (T)servicesByName.get(name);
	}
	public <T> void registryService(Class<? super T> clazz, T obj) {
		servicesByType.put(clazz, obj);
	}
	public <T> void registryService(String name, T obj) {
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
					Object toInj;
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
	
	public void start() {
		if(supplier != null) {
			throw new IllegalStateException();
		}
		// wait for window
		try {
			supplier = (IPulseSupplier) Class.forName(supplierName).newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Failed to load pulse supplier", e);
		}
		// start capture
		supplier.getAtDesireRate(40, p->{
			pulse = p;
			active();
		});
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getPulse() {
		return (T) pulse;
	}
	private void active() {
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
		supplier.stop();
	}
	
	public static void main(String[] args) {
		AppContext context = new AppContext();
		context.start();
	}
}
