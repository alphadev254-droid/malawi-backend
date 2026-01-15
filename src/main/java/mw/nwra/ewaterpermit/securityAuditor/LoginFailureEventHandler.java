package mw.nwra.ewaterpermit.securityAuditor;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import mw.nwra.ewaterpermit.constant.SysAccountStatusValue;
import mw.nwra.ewaterpermit.model.SysConfig;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.SysAccountStatusService;
import mw.nwra.ewaterpermit.service.SysConfigService;
import mw.nwra.ewaterpermit.service.SysUserAccountService;

@Component
public class LoginFailureEventHandler implements ApplicationListener<LoginFailureEvent> {

	@Autowired
	private SysUserAccountService userAccountService;

	@Autowired
	SysConfigService sysConfigService;

	@Autowired
	private SysAccountStatusService accountStatusService;

	@Override
	public void onApplicationEvent(LoginFailureEvent event) {

		Authentication authentication = (Authentication) event.getSource();
		System.out.println("LoginEvent Failure for: " + authentication.getPrincipal());
		updateUserAccount(authentication);
	}

	public void updateUserAccount(Authentication authentication) {
		SysUserAccount user = this.userAccountService
				.getSysUserAccountByUsername((String) authentication.getPrincipal());

		if (user != null) { // user found
			// Ensure date_created is preserved
			if (user.getDateCreated() == null) {
				user.setDateCreated(new Timestamp(new Date().getTime()));
			}
			SysConfig sysConfig = this.sysConfigService.getAllSysConfigurations().stream()
					.filter(config -> config.getDateDeactivated() == null).collect(Collectors.toList()).get(0);
			Integer currentAttempts = user.getPasswordAttemptCount();
		user.setPasswordAttemptCount((currentAttempts != null ? currentAttempts : 0) + 1);
			user.setLastPasswordAttempt(new Timestamp(new Date().getTime()));
			if (user.getPasswordAttemptCount() > sysConfig.getLockUserMaximumAttempts()) {
				System.out.println("3 Valid User name, updating failed attempts for " + user.getUsername());
				// if failures exceeds 2X LockUserMaximumAttempts, disable/deactivate account
				if (user.getPasswordAttemptCount() > (2 * sysConfig.getLockUserMaximumAttempts())) {
					// lock
					user.setSysAccountStatus(this.accountStatusService
							.getSysAccountStatusByName(SysAccountStatusValue.DEACTIVATED.toString()));
				} else {
					// else disable temporarily
					user.setSysAccountStatus(this.accountStatusService
							.getSysAccountStatusByName(SysAccountStatusValue.TEMPORARILY_DISABLED.toString()));

					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(new Date().getTime());
					cal.add(Calendar.SECOND, sysConfig.getLockUserTime());
					user.setCanLoginAfter(new Timestamp(cal.getTime().getTime()));
				}
			}
			user.setLastPasswordAttempt(new Timestamp(new Date().getTime()));
			user.setDateUpdated(new Timestamp(new Date().getTime()));
			this.userAccountService.updateSysUserAccount(user);
		}
	}
}
