package com.example.demo.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.config.FhirFacadeProperties;

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service // Kennzeichnet diese Klasse als Spring Service-Komponente
public class ProprietaryApiService {

	private static final Logger logger = Logger.getLogger(ProprietaryApiService.class.getName());

	// Erzeugt ein RestTemplate-Objekt für HTTP-Anfragen
	private final RestTemplate restTemplate = new RestTemplate();

	private final String proprietaryBaseUrl;

	public ProprietaryApiService(FhirFacadeProperties fhirFacadeProperties) {
		proprietaryBaseUrl = fhirFacadeProperties.getProprietaryBaseUrl();
	}

	/**
	 * Sendet Patientendaten an eine proprietäre API.
	 * 
	 * @param firstName Der Vorname des Patienten
	 * @param lastName  Der Nachname des Patienten
	 * @param birthDate Das Geburtsdatum des Patienten
	 * @return true, wenn die API-Anfrage erfolgreich war; false, wenn ein Fehler
	 *         aufgetreten ist
	 */

	public boolean sendPatientData(String firstName, String lastName, String birthDate) {
		try {
			// URL der proprietären API
			String url = proprietaryBaseUrl + "/Person";
			// Erstellen des Anfragekörpers mit den Patientendaten
			String requestBody = String.format("{\"firstName\":\"%s\",\"lastName\":\"%s\",\"birthDate\":\"%s\"}",
					firstName, lastName, birthDate);

			return sendRequest(url, requestBody);
		} catch (Exception e) {
			// Loggt eine Ausnahme, falls eine auftritt
			logger.log(Level.SEVERE, "Exception occurred while sending patient data", e);
			return false;
		}
	}

	/**
	 * Sendet Dokumenten an eine proprietäre API.
	 * 
	 * @param kdlCode               Der Kode der Klinische Dokumentenklassen-Liste
	 *                              (KDL)
	 * @param patientenId           Der Id es Patienten
	 * @param abrechnungsfallNummer Die Abrechnungsfallnummer
	 * @param creationDate          Das Erstelldatum
	 * @param data                  die eigentliche Binary
	 * @return true, wenn die API-Anfrage erfolgreich war; false, wenn ein Fehler
	 *         aufgetreten ist
	 */

	public boolean sendDocumentData(String kdlCode, Integer patientenId, Integer abrechnungsfallNummer,
			Date creationDate, byte[] data) {
		try {
			// URL der proprietären API
			String url = proprietaryBaseUrl + "/Document";
			Encoder encoder = Base64.getEncoder();
			String contentB64 = encoder.encodeToString(data);
			// Erstellen des Anfragekörpers mit den Patientendaten
			String requestBody = String.format(
					"{\"kdlCode\":\"%s\",\"patientId\":%d,\"visitNumber\":%d,\"dateCreated\":\"%s\",\"contentB64\":\"%s\"}",
					kdlCode, patientenId, abrechnungsfallNummer, creationDate, contentB64);

			return sendRequest(url, requestBody);
		} catch (Exception e) {
			// Loggt eine Ausnahme, falls eine auftritt
			logger.log(Level.SEVERE, "Exception occurred while sending patient data", e);
			return false;
		}
	}

	private boolean sendRequest(String url, String requestBody) {
		// Loggt die URL und den Anfragekörper
		logger.info("Sending request to proprietary API: " + url);
		logger.info("Request body: " + requestBody);

		// Sendet eine POST-Anfrage an die proprietäre API
		ResponseEntity<String> response = restTemplate.postForEntity(url, requestBody, String.class);

		// Loggt den Statuscode der Antwort
		logger.info("Response from proprietary API: " + response.getStatusCode());
		HttpStatus statusCode = response.getStatusCode();

		// Akzeptiere sowohl 200 (OK) als auch 201 (Created) als erfolgreichen Status
		if (statusCode == HttpStatus.OK || statusCode == HttpStatus.CREATED) {
			return true;
		} else {
			// Loggt einen Fehler, wenn der Statuscode nicht 200 oder 201 ist
			logger.severe("Proprietary API returned an error: " + response.getBody());
			return false;
		}
	}
}