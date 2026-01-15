package mw.nwra.ewaterpermit.securityAuditor;

import org.springframework.context.ApplicationEvent;

public class LoginFailureEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1L;

	/**
	 * Create a new ApplicationEvent.
	 */
	public LoginFailureEvent(Object source) {
		super(source);
	}
}
