package com.ticketpurchasingsystem.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class TicketApplication {

	public static void main(String[] args) {
		try {
			SpringApplication.run(TicketApplication.class, args);
		}catch (Exception e){
			System.exit(1);
		}
	}

}
