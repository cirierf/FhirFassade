package com.example.demo.controller;

public class BadRequestException extends Exception {
	private final String errorMessage;

	public BadRequestException(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
