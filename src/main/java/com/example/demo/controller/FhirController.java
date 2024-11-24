package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

	private static final Logger logger = Logger.getLogger(FhirController.class.getName());
	private final ProprietaryApiService proprietaryApiService;

	public FhirController(ProprietaryApiService proprietaryApiService) {
		this.proprietaryApiService = proprietaryApiService;
	}

	// Erzeugt ein FhirContext-Objekt für FHIR R4
	private final FhirContext fhirContext = FhirContext.forR4();

	@PostMapping("/Patient") // Mapped HTTP POST-Anfragen auf diesen Endpunkt
	public ResponseEntity<String> createPatient(@RequestBody String patientResource) {
		// Loggt die erhaltene Anfrage
		logger.info("Received request to create patient: " + patientResource);

		// Erzeugt einen JSON-Parser für FHIR
		IParser parser = fhirContext.newJsonParser();
		// Parsen des Patient-Ressource-Strings in ein Patient-Objekt
		Patient patient = parser.parseResource(Patient.class, patientResource);
		try {
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

			// Loggt die extrahierten und konvertierten Patientendaten
			logger.info("Parsed patient data: " + firstName + " " + lastName + ", Birthdate: " + birthDate);
			// Sendet die Patientendaten an die proprietäre API
			boolean apiSuccess = proprietaryApiService.sendPatientData(firstName, lastName, birthDate);

			if (apiSuccess) {
				// Loggt und gibt eine Erfolgsantwort zurück, wenn die API-Anfrage erfolgreich
				// war
				logger.info("Patient data sent successfully.");
				return ResponseEntity.status(HttpStatus.CREATED).body("Patient created successfully.");
			} else {
				// Loggt und gibt eine Fehlerantwort zurück, wenn die API-Anfrage fehlschlägt
				logger.severe("Failed to send patient data to proprietary API.");
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error.");
			}
		} catch (BadRequestException e) {
			// Loggt und gibt eine Fehlerantwort zurück, wenn eine Ausnahme auftritt
			logger.log(Level.SEVERE, "Exception occurred while creating document", e);
			OperationOutcome operationOutcome = createOperationOutcome(e.getErrorMessages());

			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON)
					.body(parser.encodeResourceToString(operationOutcome));
		} catch (Exception e) {
			// Loggt und gibt eine Fehlerantwort zurück, wenn eine Ausnahme auftritt
			logger.log(Level.SEVERE, "Exception occurred while creating patient", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error.");
		}
	}

	private OperationOutcome createOperationOutcome(List<String> errorMessages) {
		OperationOutcome operationOutcome = new OperationOutcome();
		errorMessages.stream().forEach(errorMessage -> {
			OperationOutcomeIssueComponent issue = new OperationOutcomeIssueComponent();
			IssueSeverity severity = IssueSeverity.FATAL;
			issue.setSeverity(severity);
			IssueType code = IssueType.REQUIRED;
			issue.setCode(code);
			issue.setDiagnostics(errorMessage);
			operationOutcome.addIssue(issue);
		});
		return operationOutcome;
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