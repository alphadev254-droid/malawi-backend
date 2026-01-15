package mw.nwra.ewaterpermit.service;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import mw.nwra.ewaterpermit.model.SysUserAccount;

@Service(value = "customUserDetailsService")
public class CustomUserDetailsService implements UserDetailsService {

	@Autowired
	private SysUserAccountService sysUserAccountService;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// TODO Auto-generated method stub
		SysUserAccount sysUserAccount = this.sysUserAccountService.getSysUserAccountByUsername(username);

		if (sysUserAccount != null) {
			return new User(sysUserAccount.getUsername(), sysUserAccount.getPassword(), new ArrayList<>());
		}
		return null;
	}

}
