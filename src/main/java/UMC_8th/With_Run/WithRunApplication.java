package UMC_8th.With_Run;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class WithRunApplication {

	public static void main(String[] args) {
		System.out.println("JDBC URL = " +
				System.getenv("DB_HOST") + ":" +
				System.getenv("DB_PORT") + "/" +
				System.getenv("DB_NAME"));
		SpringApplication.run(WithRunApplication.class, args);
	}


}
