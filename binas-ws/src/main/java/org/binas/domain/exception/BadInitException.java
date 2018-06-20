package org.binas.domain.exception;

/** Exception used to signal a problem while initializing a station. */
public class BadInitException extends Exception {
	private static final long serialVersionUID = 1L;

	public BadInitException() {
	}

	public BadInitException(String message) {
		super(message);
	}
}
