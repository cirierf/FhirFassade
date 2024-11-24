package com.example.demo.controller;

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

	private String getPatientJsonString() throws IOException {
		Resource examplePatientJson = new ClassPathResource("Beispiel-FHIR-Ressource-Patient.json");
		byte[] allBytes = examplePatientJson.getInputStream().readAllBytes();
		return new String(allBytes);
	}

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ProprietaryApiService proprietaryApiService;

	@Test
	void sentPatientToServiceShouldBeCreated() throws Exception {
		String patientJsonString = getPatientJsonString();
		when(proprietaryApiService.sendPatientData(anyString(), anyString(), anyString())).thenReturn(true);
		this.mockMvc.perform(post("/Patient").content(patientJsonString).contentType(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
	}

	@Test
	void serverErroredPatientToServiceShouldBeServerErrored() throws Exception {
		String patientJsonString = getPatientJsonString();
		when(proprietaryApiService.sendPatientData(anyString(), anyString(), anyString())).thenReturn(false);
		this.mockMvc.perform(post("/Patient").content(patientJsonString).contentType(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().is5xxServerError());
	}

}
