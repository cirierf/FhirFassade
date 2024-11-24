package com.example.demo.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.HumanName;
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

	private static final String KDL_SYSTEM = "http://dvmd.de/fhir/CodeSystem/kdl";
	public static final String ABRECHNUNGSNUMMER = "AN";

	@PostMapping("/" + DOCUMENT_REFERENCE)
	public ResponseEntity<String> createDocumentReference(@RequestBody String documentReferenceResource) {
		try {
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
				return ABRECHNUNGSNUMMER.equals(identifier.getType().getCoding().get(0).getCode());
			}).findFirst().get().getIdentifier().getValue();
			// Extrahieren des Erstelldatum aus der DocumentReference-Ressource
			Date creationDate = documentReference.getContent().get(0).getAttachment().getCreation();
			// Extrahieren der Dokument data
			byte[] data = documentReference.getContent().get(0).getAttachment().getData();

			int patientenIdInt = Integer.parseInt(patientenId);
			int abrechnungsfallNummerInt = Integer.parseInt(abrechnungsfallNummer);
			// Sendet die Dokumentdaten an die proprietäre API
			boolean apiSuccess = proprietaryApiService.sendDocumentData(kdlCode, patientenIdInt,
					abrechnungsfallNummerInt, creationDate, data);

			if (apiSuccess) {
				// Loggt und gibt eine Erfolgsantwort zurück, wenn die API-Anfrage erfolgreich
				// war
				return ResponseEntity.status(HttpStatus.CREATED).body("Document created successfully.");
			} else {
				// Loggt und gibt eine Fehlerantwort zurück, wenn die API-Anfrage fehlschlägt
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error.");
			}
		} catch (Exception e) {
			// Loggt und gibt eine Fehlerantwort zurück, wenn eine Ausnahme auftritt
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