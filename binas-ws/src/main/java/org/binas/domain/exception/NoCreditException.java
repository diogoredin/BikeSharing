package org.binas.domain.exception;

/** Exception used to signal a problem a with user credit. */
public class NoCreditException extends Exception {
	private static final long serialVersionUID = 1L;

	public NoCreditException() {
	}

	public NoCreditException(String message) {
		super(message);
	}
}
