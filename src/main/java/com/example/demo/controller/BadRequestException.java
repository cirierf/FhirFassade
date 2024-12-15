package com.example.demo.controller;

import java.util.List;

@SuppressWarnings("serial")
public class BadRequestException extends Exception {
	private final List<String> errorMessages;

	public BadRequestException(List<String> errorMessages) {
		this.errorMessages = errorMessages;
	}

	public List<String> getErrorMessages() {
		return errorMessages;
	}
}
