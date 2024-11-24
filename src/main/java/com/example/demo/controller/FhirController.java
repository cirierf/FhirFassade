package com.example.demo.controller;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Identifier;
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

	private static final String DOCUMENT_REFERENCE = "DocumentReference";
	private static final Logger logger = Logger.getLogger(FhirController.class.getName());
	private final ProprietaryApiService proprietaryApiService;

	public FhirController(ProprietaryApiService proprietaryApiService) {
		this.proprietaryApiService = proprietaryApiService;
	}

	// Erzeugt ein FhirContext-Objekt für FHIR R4
	private final FhirContext fhirContext = FhirContext.forR4();

	@PostMapping("/Patient") // Mapped HTTP POST-Anfragen auf diesen Endpunkt
	public ResponseEntity<String> createPatient(@RequestBody String patientResource) {
		try {
			// Loggt die erhaltene Anfrage
			logger.info("Received request to create patient: " + patientResource);

			// Erzeugt einen JSON-Parser für FHIR
			IParser parser = fhirContext.newJsonParser();
			// Parsen des Patient-Ressource-Strings in ein Patient-Objekt
			Patient patient = parser.parseResource(Patient.class, patientResource);

			// Extrahieren des Vornamens aus der Patient-Ressource
			String firstName = patient.getName().get(0).getGiven().stream().map(IPrimitiveType::getValue)
					.collect(Collectors.joining(" "));
			// Extrahieren des Nachnamens aus der Patient-Ressource
			String lastName = patient.getName().get(0).getFamily();
			// Extrahieren des Geburtsdatums aus der Patient-Ressource
			String birthDate = patient.getBirthDateElement().getValueAsString();

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
		} catch (Exception e) {
			// Loggt und gibt eine Fehlerantwort zurück, wenn eine Ausnahme auftritt
			logger.log(Level.SEVERE, "Exception occurred while creating patient", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error.");
		}
	}

	private static final String KDL_SYSTEM = "http://dvmd.de/fhir/CodeSystem/kdl";
	public static final String ABRECHNUNGSNUMMER = "AN";

	@PostMapping("/" + DOCUMENT_REFERENCE)
	public ResponseEntity<String> createDocumentReference(@RequestBody String documentReferenceResource) {
		try {
			// Loggt die erhaltene Anfrage
			logger.info("Received request to create document reference: " + documentReferenceResource);

			// Erzeugt einen JSON-Parser für FHIR
			IParser parser = fhirContext.newJsonParser();
			// Parsen des Patient-Ressource-Strings in ein Patient-Objekt
			DocumentReference documentReference = parser.parseResource(DocumentReference.class,
					documentReferenceResource);

			// Extrahieren des KDL-Code aus der DocumentReference-Ressource
			String kdlCode = documentReference.getType().getCoding().stream()
					.filter(coding -> KDL_SYSTEM.equals(coding.getSystem())).findFirst().get().getCode();
			// Extrahieren des Patienten-ID aus der DocumentReference-Ressource
			String patientenId = documentReference.getSubject().getIdentifier().getValue();
			// Extrahieren des Abrechnungsfall-Nummer aus der DocumentReference-Ressource
			String abrechnungsfallNummer = documentReference.getContext().getEncounter().stream().filter(reference -> {
				if (reference.getIdentifier() == null)
					return false;
				Identifier identifier = reference.getIdentifier();
				return ABRECHNUNGSNUMMER.equals(identifier.getSystem());
			}).findFirst().get().getIdentifier().getValue();
			// Extrahieren des Erstelldatum aus der DocumentReference-Ressource
			Date creationDate = documentReference.getContent().get(0).getAttachment().getCreation();
			// Extrahieren der Dokument data
			byte[] data = documentReference.getContent().get(0).getAttachment().getData();

			// Loggt die extrahierten und konvertierten Patientendaten
			logger.info("Parsed document data: KDL code: " + kdlCode + ", patienten Id: " + patientenId
					+ ", abrechnungsfallNummer: " + abrechnungsfallNummer + ", creationDate: " + creationDate
					+ ", data:" + data);

			int patientenIdInt = Integer.parseInt(patientenId);
			int abrechnungsfallNummerInt = Integer.parseInt(abrechnungsfallNummer);
			// Sendet die Dokumentdaten an die proprietäre API
			boolean apiSuccess = proprietaryApiService.sendDocumentData(kdlCode, patientenIdInt,
					abrechnungsfallNummerInt, creationDate, data);

			if (apiSuccess) {
				// Loggt und gibt eine Erfolgsantwort zurück, wenn die API-Anfrage erfolgreich
				// war
				logger.info("document data sent successfully.");
				return ResponseEntity.status(HttpStatus.CREATED).body("Document created successfully.");
			} else {
				// Loggt und gibt eine Fehlerantwort zurück, wenn die API-Anfrage fehlschlägt
				logger.severe("Failed to send document data to proprietary API.");
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error.");
			}
		} catch (Exception e) {
			// Loggt und gibt eine Fehlerantwort zurück, wenn eine Ausnahme auftritt
			logger.log(Level.SEVERE, "Exception occurred while creating document", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error.");
		}
	}

	private String convertDate(String birthDate) {
		// Konvertierung des Geburtsdatums von YYYY-MM-DD zu DD.MM.YYYY
		String[] parts = birthDate.split("-");
		return parts[2] + "." + parts[1] + "." + parts[0];
	}
}