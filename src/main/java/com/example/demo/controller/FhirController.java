package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.ProprietaryApiService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@RestController // Kennzeichnet diese Klasse als Spring REST Controller
@RequestMapping // Basis-URL für alle Endpunkte in dieser Klasse
public class FhirController {

	private final ProprietaryApiService proprietaryApiService;

	public FhirController(ProprietaryApiService proprietaryApiService) {
		this.proprietaryApiService = proprietaryApiService;
	}

	// Erzeugt ein FhirContext-Objekt für FHIR R4
	private final FhirContext fhirContext = FhirContext.forR4();

	@PostMapping("/Patient") // Mapped HTTP POST-Anfragen auf diesen Endpunkt
	public ResponseEntity<String> createPatient(@RequestBody String patientResource) throws BadRequestException {
		// Erzeugt einen JSON-Parser für FHIR
		IParser parser = fhirContext.newJsonParser();
		// Parsen des Patient-Ressource-Strings in ein Patient-Objekt
		Patient patient = parser.parseResource(Patient.class, patientResource);
		List<String> issues = new ArrayList<>();
		List<HumanName> name = patient.getName();
		if (name.isEmpty()) {
			issues.add("Missing patient name (Patient.name)");
		}
		// Extrahieren des Geburtsdatums aus der Patient-Ressource
		String birthDate = patient.getBirthDateElement().getValueAsString();
		if (birthDate == null) {
			issues.add("Missing birthdate (Patient.birthdate)");
		}
		if (!issues.isEmpty()) {
			throw new BadRequestException(issues);
		}

		// Extrahieren des Vornamens aus der Patient-Ressource
		String firstName = name.get(0).getGiven().stream().map(IPrimitiveType::getValue)
				.collect(Collectors.joining(" "));
		// Extrahieren des Nachnamens aus der Patient-Ressource
		String lastName = name.get(0).getFamily();

		// Konvertierung des Geburtsdatums in das gewünschte Format
		birthDate = convertDate(birthDate);

		// Sendet die Patientendaten an die proprietäre API
		boolean apiSuccess = proprietaryApiService.sendPatientData(firstName, lastName, birthDate);

		if (apiSuccess) {
			return ResponseEntity.status(HttpStatus.CREATED).body("Patient created successfully.");
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error.");
		}
	}

	private String convertDate(String birthDate) {
		// Konvertierung des Geburtsdatums von YYYY-MM-DD zu DD.MM.YYYY
		String[] parts = birthDate.split("-");
		// Das Geburtsdatum kann mit YYYY oder YYYY-MM angegeben werden, in diesemFall
		// wir verlieren die Info...
		if (parts.length < 3) {
			return null;
		}
		return parts[2] + "." + parts[1] + "." + parts[0];

	}
}