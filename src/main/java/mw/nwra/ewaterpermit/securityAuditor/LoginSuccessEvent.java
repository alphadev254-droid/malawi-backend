package mw.nwra.ewaterpermit.securityAuditor;

import org.springframework.context.ApplicationEvent;

public class LoginSuccessEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1L;

	/**
	 * Create a new ApplicationEvent.
	 */
	public LoginSuccessEvent(Object source) {
		super(source);
	}
}
