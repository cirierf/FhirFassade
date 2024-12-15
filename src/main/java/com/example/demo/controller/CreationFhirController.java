package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hl7.fhir.r4.model.DomainResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class CreationFhirController<ResourceT extends DomainResource> {

	private Function<ResourceT, Boolean> callProprietaryApiService;
	private BiConsumer<ResourceT, List<String>> checkInputResource;
	private Class<ResourceT> resourceClass;

	CreationFhirController(Class<ResourceT> resourceClass, BiConsumer<ResourceT, List<String>> checkInputResource,
			Function<ResourceT, Boolean> callProprietaryApiService) {
		this.resourceClass = resourceClass;
		this.callProprietaryApiService = callProprietaryApiService;
		this.checkInputResource = checkInputResource;
	}

	// Erzeugt ein FhirContext-Objekt für FHIR R4
	private final FhirContext fhirContext = FhirContext.forR4();

	ResponseEntity<String> createResource(String patientResource) throws BadRequestException {
		// Erzeugt einen JSON-Parser für FHIR
		IParser parser = fhirContext.newJsonParser();
		// Parsen des Ressource-Strings in ein Fhir-Reosurce
		ResourceT resource = parser.parseResource(resourceClass, patientResource);
		List<String> issues = new ArrayList<String>();
		checkInputResource.accept(resource, issues);
		// Falls Fehler aufgetreten sind, sammeln wir alle Fehlermeldungen in die
		// Ausnahme
		if (!issues.isEmpty()) {
			throw new BadRequestException(issues);
		}
		// Sendet die resource an die proprietäre API
		boolean apiSuccess = callProprietaryApiService.apply(resource);

		if (apiSuccess) {
			return ResponseEntity.status(HttpStatus.CREATED).body(resourceClass.getSimpleName() + " created successfully.");
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error.");
		}
	}

}
