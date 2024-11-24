package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.service.ProprietaryApiService;

@WebMvcTest(FhirController.class)
class FhirControllerTest {

	private static final String BEISPIEL_FHIR_RESSOURCE_PATIENT_JSON = "Beispiel-FHIR-Ressource-Patient";
	private static final String BEISPIEL_FHIR_RESSOURCE_PATIENT_JSON_OHNE_NAMEN = "Beispiel-FHIR-Ressource-Patient-Ohne-Namen";
	private static final String BEISPIEL_FHIR_RESSOURCE_PATIENT_JSON_OHNE_GEBURTSDATUM = "Beispiel-FHIR-Ressource-Patient-Ohne-Geburtsdatum";
	private static final String BEISPIEL_FHIR_RESSOURCE_DOCUMENT_REFERENCE = "Beispiel-FHIR-Ressource-DocumentReference";
	private static final String BEISPIEL_FHIR_RESSOURCE_DOCUMENT_REFERENCE_WO_KDL = "Beispiel-FHIR-Ressource-DocumentReferenceOhneKdl";
	private static final String BEISPIEL_FHIR_RESSOURCE_DOCUMENT_REFERENCE_WO_BILLING_NUMMER = "Beispiel-FHIR-Ressource-DocumentReferenceOhneAN";

	private String getJsonString(String jsonFile) throws IOException {
		Resource examplePatientJson = new ClassPathResource(jsonFile + ".json");
		byte[] allBytes = examplePatientJson.getInputStream().readAllBytes();
		return new String(allBytes);
	}

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ProprietaryApiService proprietaryApiService;

	@Test
	void sentPatientToServiceShouldBeCreated() throws Exception {
		String patientJsonString = getJsonString(BEISPIEL_FHIR_RESSOURCE_PATIENT_JSON);
		when(proprietaryApiService.sendPatientData(anyString(), anyString(), anyString())).thenReturn(true);
		this.mockMvc.perform(post("/Patient").content(patientJsonString).contentType(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
	}

	@Test
	void serverErroredPatientToServiceShouldBeServerErrored() throws Exception {
		String patientJsonString = getJsonString(BEISPIEL_FHIR_RESSOURCE_PATIENT_JSON);
		when(proprietaryApiService.sendPatientData(anyString(), anyString(), anyString())).thenReturn(false);
		this.mockMvc.perform(post("/Patient").content(patientJsonString).contentType(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().is5xxServerError());
	}

	@Test
	void sentPatientWoNameShouldBeUnprocessableEntityErrored() throws Exception {
		String patientJsonString = getJsonString(BEISPIEL_FHIR_RESSOURCE_PATIENT_JSON_OHNE_NAMEN);
		when(proprietaryApiService.sendPatientData(anyString(), anyString(), anyString())).thenReturn(true);
		this.mockMvc.perform(post("/Patient").content(patientJsonString).contentType(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isUnprocessableEntity());
	}

	@Test
	void sentPatientWoBirthdateShouldBeUnprocessableEntityErrored() throws Exception {
		String patientJsonString = getJsonString(BEISPIEL_FHIR_RESSOURCE_PATIENT_JSON_OHNE_GEBURTSDATUM);
		when(proprietaryApiService.sendPatientData(anyString(), anyString(), anyString())).thenReturn(true);
		this.mockMvc.perform(post("/Patient").content(patientJsonString).contentType(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isUnprocessableEntity());
	}

	@Test
	void sentDocumentToServiceShouldBeCreated() throws Exception {
		String documentJsonString = getJsonString(BEISPIEL_FHIR_RESSOURCE_DOCUMENT_REFERENCE);
		when(proprietaryApiService.sendDocumentData(anyString(), any(), any(), any(), any())).thenReturn(true);
		this.mockMvc
				.perform(post("/DocumentReference").content(documentJsonString).contentType(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
	}

	@Test
	void serverErroredDocumentToServiceShouldBeServerErrored() throws Exception {
		String documentJsonString = getJsonString(BEISPIEL_FHIR_RESSOURCE_DOCUMENT_REFERENCE);
		when(proprietaryApiService.sendDocumentData(anyString(), any(), any(), any(), any())).thenReturn(false);
		this.mockMvc.perform(post("/DocumentReference").content(documentJsonString).contentType(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().is5xxServerError());
	}

	@Test
	void sentDocumentWoKdlShouldBeUnprocessableEntityErrored() throws Exception {
		String patientJsonString = getJsonString(BEISPIEL_FHIR_RESSOURCE_DOCUMENT_REFERENCE_WO_KDL);
		when(proprietaryApiService.sendPatientData(anyString(), anyString(), anyString())).thenReturn(true);
		this.mockMvc.perform(post("/DocumentReference").content(patientJsonString).contentType(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isUnprocessableEntity());
	}

	@Test
	void sentDocumentWoBillingNumberShouldBeUnprocessableEntityErrored() throws Exception {
		String patientJsonString = getJsonString(BEISPIEL_FHIR_RESSOURCE_DOCUMENT_REFERENCE_WO_BILLING_NUMMER);
		when(proprietaryApiService.sendPatientData(anyString(), anyString(), anyString())).thenReturn(true);
		this.mockMvc.perform(post("/DocumentReference").content(patientJsonString).contentType(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isUnprocessableEntity());
	}
}
