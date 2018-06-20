package org.binas.ws;

import org.binas.domain.BinasManager;

public class BinasApp {

	public static void main(String[] args) throws Exception {
		
		if (args.length < 2) {
			System.err.println("Argument(s) missing!");
			System.err.println("Usage: java " + BinasApp.class.getName() + "wsName wsURL OR wsName wsURL uddiURL");
			return;
		}

		String uddiURL = args[0];
		String wsName = args[1];
		String wsURL = args[2];

		BinasEndpointManager endpoint = new BinasEndpointManager(uddiURL, wsName, wsURL);

		System.out.println(BinasApp.class.getSimpleName() + " running");

		try {
			endpoint.start();
			endpoint.awaitConnections();
		} finally {
			endpoint.stop();
		}

	}

}