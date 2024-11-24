package com.example.demo.config;

import java.util.List;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.demo.controller.BadRequestException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ControllerAdvice
@Component
public class ExceptionAspect {
	private final FhirContext fhirContext = FhirContext.forR4();
	IParser parser = fhirContext.newJsonParser();

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

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<Object> handleBadRquestException(BadRequestException exception) {
		Logger logger = LoggerFactory.getLogger(ExceptionAspect.class);
		logger.error("Handling: ", exception);

		// Loggt und gibt eine Fehlerantwort zurück, wenn eine Ausnahme auftritt
		logger.error("Exception occurred while creating document", exception);
		OperationOutcome operationOutcome = createOperationOutcome(exception.getErrorMessages());

		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON)
				.body(parser.encodeResourceToString(operationOutcome));

	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleException(Exception exception) {
		Logger logger = LoggerFactory.getLogger(ExceptionAspect.class);
		logger.error("Handling: ", exception);

		// Loggt und gibt eine Fehlerantwort zurück, wenn eine Ausnahme auftritt
		logger.error("Exception occurred while creating resource", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error.");
	}

}