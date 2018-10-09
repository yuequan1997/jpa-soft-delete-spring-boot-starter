package org.yuequan.jpa.soft.delete;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.yuequan.jpa.soft.delete.repository.EnableJpaSoftDeleteRepositories;
import org.yuequan.jpa.soft.delete.repository.support.JpaSoftDeleteRepository;

@SpringBootApplication
@EnableJpaSoftDeleteRepositories
public class JpaSoftDeleteSpringBootStarterApplication {

	public static void main(String[] args) {
		SpringApplication.run(JpaSoftDeleteSpringBootStarterApplication.class, args);
	}
}
