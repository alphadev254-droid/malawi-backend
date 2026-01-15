package mw.nwra.ewaterpermit.securityAuditor;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoginAspect {

	@Autowired
	private LoginFailureEventPublisher publisher;
	@Autowired
	private LoginSuccessEventPublisher successEventPublisher;

	@Pointcut("execution(* org.springframework.security.authentication.AuthenticationProvider.authenticate(..))")
	public void doAuthenticate() {

	}

	@Before("mw.nwra.ewaterpermit.securityAuditor.LoginAspect.doAuthenticate() && args(authentication)")
	public void logBefore(Authentication authentication) {

//		System.out.println("This is before the Authenticate Method: authentication: " + authentication.isAuthenticated());
	}

	@AfterReturning(value = "mw.nwra.ewaterpermit.securityAuditor.LoginAspect.doAuthenticate()", returning = "authentication")
	public void logAfterAuthenticate(Authentication authentication) {
//		System.out.println("This is after the Authenticate Method authentication: " + authentication.isAuthenticated());
		successEventPublisher.publishEvent(new LoginSuccessEvent(authentication));
	}

	@AfterThrowing("mw.nwra.ewaterpermit.securityAuditor.LoginAspect.doAuthenticate() && args(authentication)")
	public void logAuthenicationException(Authentication authentication) {
		String userDetails = (String) authentication.getPrincipal();
		System.out.println("Login failed for user: " + userDetails);

		publisher.publish(new LoginFailureEvent(authentication));

	}
}
