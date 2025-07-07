package yeonjae.snapguide;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SnapguideApplication {

	public static void main(String[] args) {
		SpringApplication.run(SnapguideApplication.class, args);
	}
}
