package org.binas.station.ws;

import org.binas.station.domain.Station;

import java.util.Scanner;

/**
 * The application is where the service starts running. The program arguments
 * are processed here. Other configurations can also be done here.
 */
public class StationApp {

	public static void main(String[] args) throws Exception {

		/* Check arguments */
		if (args.length < 2) {
			System.err.println("Argument(s) missing!");
			System.err.println("Usage: java " + StationApp.class.getName() + "wsName wsURL OR wsName wsURL uddiURL");
			return;
		}

		String wsName = args[0];
		String wsURL = args[1];
		String uddiURL = args[2];
		
		/* Create connection */
		StationEndpointManager endpoint = new StationEndpointManager(uddiURL, wsName, wsURL);
		Station.getInstance().setId(wsName);

		System.out.println(StationApp.class.getSimpleName() + " running");

		/* Start web service */
		try {
			endpoint.start();

			/********** FAULT TOLERANCE TESTING ZONE **********/

			int num = 1;
			Scanner scanner = new Scanner(System.in);
			System.out.println("\nFAULT TOLERANCE TESTING");
			System.out.println("Press (0) to shutdown.");
			System.out.println("Press (1) to get number of registered users.");
			System.out.println("Press (2) to clear everything on the station.");
	
			System.out.println("\nEnter Command:");
	
			while ((num = scanner.nextInt()) > 0) {
				
				if (num == 1) {

					try {
						System.out.println(Station.getInstance().getUsers().size() + " users.");
		
					} catch (Exception e) {
						System.out.println("Something went wrong!");

					}

				} else if (num == 2) {

					try {
						Station.getInstance().reset();
						System.out.println("Station cleared!");
		
					} catch (Exception e) {
						System.out.println("Something went wrong!");
						
					}
				}
			}

		} finally {
			endpoint.stop();

		}

	}

}