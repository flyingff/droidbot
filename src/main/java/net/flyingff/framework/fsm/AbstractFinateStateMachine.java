package net.flyingff.framework.fsm;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.flyingff.framework.AppContext;
import net.flyingff.framework.IAppContext;

public abstract class AbstractFinateStateMachine {
	private final AppContext context;
	private Map<String, IState> states = new HashMap<>();
	private IState startState, current;
	
	public AbstractFinateStateMachine(IAppContext ctx) {
		AppContext context = (AppContext) ctx;
		this.context = Objects.requireNonNull(context, "Context cannot be null.");
		
		String starter = createStates(context);
		startState = getState(starter);
		current = startState;
	}
	private final IState getState(String name) {
		return Objects.requireNonNull(states.get(name), "State not found: " + name);
	}
	protected final void registState (String name, IState state) {
		name = Objects.requireNonNull(name, "Name cannot be null");
		if(states.containsKey(name)) {
			throw new RuntimeException("Duplicate state name: " + name);
		}
		states.put(name, state);
	}
	
	private final TransHint hint = new TransHint();
	/**
	 * Active this FSM once.
	 * @return true if not wait, else false when need wait
	 */
	public boolean active() {
		hint.nextState = null;
		hint.nextStateHint = TransHint.NONE;
		hint.FSMName = null;
		current.accept(hint);
		// change next state
		if(hint.nextState != null) {
			current = getState(hint.nextState);
		}
		// execute special command
		switch (hint.nextStateHint) {
		case TransHint.POP:
			context.pop(this);
			break;
		case TransHint.PUSH:
			context.push(hint.FSMName, this);
			break;
		case TransHint.SWAP:
			context.swap(hint.FSMName, this);
			break;
		}
		
		// decide whether need to sleep
		return hint.nextStateHint != TransHint.WAIT;
	}
	
	protected abstract String createStates(final IAppContext context);

	/*public abstract class State {
		private final String name;
		private Consumer<>
		public State(String name) {
			this.name = Objects.requireNonNull(name, "State must have name.");
			registState(this);
		}
		/**
		 * do initialize in this method
		 * @return the name of this State
		 *
		protected void init(AppContext context) {}
		/**
		 * called when it's active
		 *
		public abstract void active(AppContext context, TransHint hint);
	}*/
	
	public static class TransHint {
		private static final int NONE = 0, PUSH = 1, POP = 2, WAIT = 3, SWAP = 4;
		private String nextState = null;
		private int nextStateHint = NONE;
		private String FSMName = null;
		public void push(String fsmName) {
			nextStateHint = PUSH;
			FSMName = fsmName;
		}
		public void pop() {
			nextStateHint = POP;
		}
		public void doWait() {
			nextStateHint = WAIT;
		}
		public void swap(String fsmName) {
			nextStateHint = SWAP;
			FSMName = fsmName;
		}
		public void next(String str) {
			nextState = str;
		}
	}
}
