package com.example.demo.controller;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.ProprietaryApiService;

@RestController // Kennzeichnet diese Klasse als Spring REST Controller
@RequestMapping // Basis-URL für alle Endpunkte in dieser Klasse
public class FhirController {

	private static final String DOCUMENT_REFERENCE = "DocumentReference";
	private final ProprietaryApiService proprietaryApiService;

	public FhirController(ProprietaryApiService proprietaryApiService) {
		this.proprietaryApiService = proprietaryApiService;
	}

	void checkInputResourcePatient(Patient patient, List<String> issues) {
		List<HumanName> name = patient.getName();
		if (name.isEmpty()) {
			issues.add("Missing patient name (Patient.name)");
		}
		// Extrahieren des Geburtsdatums aus der Patient-Ressource
		String birthDate = patient.getBirthDateElement().getValueAsString();
		if (birthDate == null) {
			issues.add("Missing birthdate (Patient.birthdate)");
		}
	}

	Boolean callProprietaryApiServicePatient(Patient patient) {
		List<HumanName> name = patient.getName();
		String birthDate = patient.getBirthDateElement().getValueAsString();

		// Extrahieren des Vornamens aus der Patient-Ressource
		String firstName = name.get(0).getGiven().stream().map(IPrimitiveType::getValue)
				.collect(Collectors.joining(" "));
		// Extrahieren des Nachnamens aus der Patient-Ressource
		String lastName = name.get(0).getFamily();

		// Konvertierung des Geburtsdatums in das gewünschte Format
		birthDate = convertDate(birthDate);

		// Sendet die Patientendaten an die proprietäre API
		boolean apiSuccess = proprietaryApiService.sendPatientData(firstName, lastName, birthDate);
		return apiSuccess;
	}

	@PostMapping("/Patient") // Mapped HTTP POST-Anfragen auf diesen Endpunkt
	public ResponseEntity<String> createPatient(@RequestBody String patientResource) throws BadRequestException {

		CreationFhirController<Patient> creationFhirController = new CreationFhirController<Patient>(Patient.class,
				this::checkInputResourcePatient, this::callProprietaryApiServicePatient);
		return creationFhirController.createResource(patientResource);

	}

	private static final String KDL_SYSTEM = "http://dvmd.de/fhir/CodeSystem/kdl";
	public static final String ABRECHNUNGSNUMMER = "AN";

	void checkInputResourceDocumentReference(DocumentReference documentReference, List<String> issues) {
		// Extrahieren des KDL-Code aus der DocumentReference-Ressource
		Optional<Coding> maybeKdlCoding = extractKdlCoding(documentReference);
		if (!maybeKdlCoding.isPresent()) {
			issues.add("Missing document type from KDL terminology (DocumentReference.type.coding:KDL)");
		}

		Optional<Reference> maybeBillingNumber = extractBillingnumber(documentReference);
		if (!maybeBillingNumber.isPresent()) {
			issues.add(
					"Missing billing number (DocumentReference.context.encounter.identifier:Abrechnungsnummer.type.coding:AN)");
		}
	}

	private Optional<Coding> extractKdlCoding(DocumentReference documentReference) {
		return documentReference.getType().getCoding().stream().filter(coding -> KDL_SYSTEM.equals(coding.getSystem()))
				.findFirst();
	}

	private Optional<Reference> extractBillingnumber(DocumentReference documentReference) {
		return documentReference.getContext().getEncounter().stream().filter(reference -> {
			if (reference.getIdentifier() == null)
				return false;
			Identifier identifier = reference.getIdentifier();
			return ABRECHNUNGSNUMMER.equals(identifier.getType().getCoding().get(0).getCode());
		}).findFirst();
	}

	Boolean callProprietaryApiServiceDocumentReference(DocumentReference documentReference) {
		Optional<Coding> maybeKdlCoding = documentReference.getType().getCoding().stream()
				.filter(coding -> KDL_SYSTEM.equals(coding.getSystem())).findFirst();
		String kdlCode = maybeKdlCoding.get().getCode();
		// Extrahieren des Patienten-ID aus der DocumentReference-Ressource
		String patientenId = documentReference.getSubject().getIdentifier().getValue();
		Optional<Reference> maybeBillingNumber = extractBillingnumber(documentReference);
		// Extrahieren des Abrechnungsfall-Nummer aus der DocumentReference-Ressource
		String abrechnungsfallNummer = maybeBillingNumber.get().getIdentifier().getValue();
		// Extrahieren des Erstelldatum aus der DocumentReference-Ressource
		Date creationDate = documentReference.getContent().get(0).getAttachment().getCreation();
		// Extrahieren der Dokument data
		byte[] data = documentReference.getContent().get(0).getAttachment().getData();

		int patientenIdInt = Integer.parseInt(patientenId);
		int abrechnungsfallNummerInt = Integer.parseInt(abrechnungsfallNummer);
		// Sendet die Dokumentdaten an die proprietäre API
		return proprietaryApiService.sendDocumentData(kdlCode, patientenIdInt, abrechnungsfallNummerInt, creationDate,
				data);
	}

	@PostMapping("/" + DOCUMENT_REFERENCE)
	public ResponseEntity<String> createDocumentReference(@RequestBody String documentReferenceResource)
			throws BadRequestException {
		CreationFhirController<DocumentReference> creationFhirController = new CreationFhirController<>(
				DocumentReference.class, this::checkInputResourceDocumentReference,
				this::callProprietaryApiServiceDocumentReference);
		return creationFhirController.createResource(documentReferenceResource);
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