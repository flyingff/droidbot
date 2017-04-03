package net.flyingff.framework.fsm;

import java.util.function.Consumer;

import net.flyingff.framework.fsm.AbstractFinateStateMachine.TransHint;

public interface IState extends Consumer<TransHint> {

}
