package com.certificate.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClientApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(ClientApplication.class);
        // Disable the web environment (tomcat will not start)
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
	}

}
