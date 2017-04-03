package net.flyingff.framework.fsm;

import javax.inject.Inject;

import net.flyingff.framework.IAppContext;
import net.flyingff.framework.service.IAnnouncementService;

public class NextFSM extends AbstractFinateStateMachine {

	public NextFSM(IAppContext ctx) {
		super(ctx);
	}
	@Inject
	private IAnnouncementService as;
	@Override
	protected String createStates(IAppContext context) {
		registState("s", h->{
			System.out.println("as=" + as);
			h.pop();
		});
		return "s";
	}

}
