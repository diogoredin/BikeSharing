package org.binas.station.ws.cli;

/** Exception to be thrown when something is wrong with the client. */
public class StationClientException extends Exception {

	private static final long serialVersionUID = 1L;

	public StationClientException() {
		super();
	}

	public StationClientException(String message) {
		super(message);
	}

	public StationClientException(Throwable cause) {
		super(cause);
	}

	public StationClientException(String message, Throwable cause) {
		super(message, cause);
	}

}
