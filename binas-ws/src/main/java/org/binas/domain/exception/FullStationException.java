package org.binas.domain.exception;

/** Exception used to signal a problem when user already has a Bina. */
public class FullStationException extends Exception {
	private static final long serialVersionUID = 1L;

	public FullStationException() {
	}

	public FullStationException(String message) {
		super(message);
	}
}
