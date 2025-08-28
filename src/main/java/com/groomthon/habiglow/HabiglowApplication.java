package com.groomthon.habiglow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class HabiglowApplication {

	public static void main(String[] args) {
		SpringApplication.run(HabiglowApplication.class, args);
	}

}
