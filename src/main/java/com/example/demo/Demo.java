package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // Kennzeichnet die Hauptklasse einer Spring Boot-Anwendung
public class Demo {

	// Hauptmethode - der Einstiegspunkt der Spring Boot-Anwendung
	public static void main(String[] args) {
		 // Startet die Spring Boot-Anwendung
		SpringApplication.run(Demo.class, args);
	}

}
