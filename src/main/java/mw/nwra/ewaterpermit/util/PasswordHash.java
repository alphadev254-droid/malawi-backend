package mw.nwra.ewaterpermit.util;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class PasswordHash {
	public PasswordHash() {

	}

	public static String hashPassword(String plainTextPassword) {
		return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
	}

	public static boolean checkPassword(String plainPassword, String hashedPassword) {
		boolean passwordMatch = false;
		try {
			passwordMatch = BCrypt.checkpw(plainPassword, hashedPassword);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return passwordMatch;
	}

}
