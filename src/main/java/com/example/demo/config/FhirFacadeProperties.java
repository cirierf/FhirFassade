package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
@Configuration
@ConfigurationProperties(prefix = "fhir-facade", ignoreUnknownFields = false)
public class FhirFacadeProperties {
	private String proprietaryBaseUrl;

	public String getProprietaryBaseUrl() {		
		return proprietaryBaseUrl;
	}

	public void setProprietaryBaseUrl(String baseUrl) {
		this.proprietaryBaseUrl = baseUrl;
	}
}
