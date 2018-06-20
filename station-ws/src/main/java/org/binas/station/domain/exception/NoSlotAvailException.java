package org.binas.station.domain.exception;

/** Exception used to signal that no slots are are currently available in a station. */
public class NoSlotAvailException extends Exception {
	private static final long serialVersionUID = 1L;

	public NoSlotAvailException() {
	}

	public NoSlotAvailException(String message) {
		super(message);
	}
}
