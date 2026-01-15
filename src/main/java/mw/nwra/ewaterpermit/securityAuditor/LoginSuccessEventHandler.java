package mw.nwra.ewaterpermit.securityAuditor;

import java.sql.Timestamp;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import mw.nwra.ewaterpermit.constant.SysAccountStatusValue;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.service.SysAccountStatusService;
import mw.nwra.ewaterpermit.service.SysConfigService;
import mw.nwra.ewaterpermit.service.SysUserAccountService;

@Component
public class LoginSuccessEventHandler implements ApplicationListener<LoginSuccessEvent> {
	@Autowired
	private SysUserAccountService userAccountService;

	@Autowired
	SysConfigService sysConfigService;

	@Autowired
	private SysAccountStatusService accountStatusService;

	@Override
	public void onApplicationEvent(LoginSuccessEvent event) {
		Authentication authentication = (Authentication) event.getSource();
//		System.out.println("LoginEvent Success for: " + authentication.getPrincipal());
		updateUserAccount(authentication);
	}

	public void updateUserAccount(Authentication authentication) {
		UserDetails userDetails = (UserDetails) authentication.getPrincipal();
		SysUserAccount user = this.userAccountService.getSysUserAccountByUsername(userDetails.getUsername());

		if (user != null) { // user found
			// Ensure date_created is preserved
			if (user.getDateCreated() == null) {
				user.setDateCreated(new Timestamp(new Date().getTime()));
			}
			user.setLastLogin(new Timestamp(new Date().getTime()));
			user.setCanLoginAfter(null);
			user.setLastPasswordAttempt(new Timestamp(new Date().getTime()));
			user.setPasswordAttemptCount(0);
			user.setDateUpdated(new Timestamp(new Date().getTime()));
			user.setSysAccountStatus(
					this.accountStatusService.getSysAccountStatusByName(SysAccountStatusValue.ACTIVE.toString()));
			this.userAccountService.updateSysUserAccount(user);
		}
	}
}
