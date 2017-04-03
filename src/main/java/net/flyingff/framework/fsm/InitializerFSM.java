package net.flyingff.framework.fsm;

import net.flyingff.framework.IAppContext;
import net.flyingff.framework.service.AnnouncementService;
import net.flyingff.framework.service.IAnnouncementService;

public class InitializerFSM extends AbstractFinateStateMachine {

	public InitializerFSM(IAppContext context) {
		super(context);
	}

	@Override
	protected String createStates(IAppContext context) {
		registState("start", hint->{
			context.registryService(IAnnouncementService.class, new AnnouncementService());
			System.out.println("Actived.");
			hint.next("2");
		});
		
		registState("2", hint->{
			hint.swap("next");
			System.out.println("pop on 2");
		});
		return "start";
	}

}
