package org.binas.domain.exception;

/** Exception used to signal a problem while initializing a user. */
public class EmailExistsException extends Exception {
	private static final long serialVersionUID = 1L;

	public EmailExistsException() {
	}

	public EmailExistsException(String message) {
		super(message);
	}
}
