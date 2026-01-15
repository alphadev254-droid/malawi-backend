package mw.nwra.ewaterpermit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
public class EwaterpermitApplication {

	public static void main(String[] args) {
		SpringApplication.run(EwaterpermitApplication.class, args);
	}

}
