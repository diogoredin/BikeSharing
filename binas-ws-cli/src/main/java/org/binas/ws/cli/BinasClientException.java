package org.binas.ws.cli;


public class BinasClientException extends Exception {

	private static final long serialVersionUID = 1L;

	public BinasClientException() {
    }

    public BinasClientException(String message) {
        super(message);
    }

    public BinasClientException(Throwable cause) {
        super(cause);
    }

    public BinasClientException(String message, Throwable cause) {
        super(message, cause);
    }

}