package org.binas.ws;

import javax.jws.WebService;

import org.binas.station.ws.*;
import org.binas.station.ws.cli.*;

import org.binas.domain.BinasManager;
import org.binas.domain.User;

import org.binas.domain.exception.EmailExistsException;
import org.binas.domain.exception.InvalidEmailException;
import org.binas.domain.exception.UserNotExistsException;
import org.binas.domain.exception.NoCreditException;
import org.binas.domain.exception.NoSlotAvailException;
import org.binas.domain.exception.InvalidStationException;
import org.binas.domain.exception.NoBinaAvailException;
import org.binas.domain.exception.BadInitException;
import org.binas.domain.exception.AlreadyHasBinaException;
import org.binas.domain.exception.FullStationException;
import org.binas.domain.exception.NoBinaRentedException;

import pt.ulisboa.tecnico.sdis.ws.uddi.UDDINaming;
import pt.ulisboa.tecnico.sdis.ws.uddi.UDDIRecord;

import pt.ulisboa.tecnico.sdis.ws.uddi.UDDINamingException;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import javax.jws.HandlerChain;
import javax.jws.WebService;

/**
 * This class implements the Web Service port type (interface). The annotations
 * below "map" the Java class to the WSDL definitions.
 */

@WebService(
	endpointInterface = "org.binas.ws.BinasPortType",
	wsdlLocation = "binas.1_0.wsdl",
	name = "BinasWebService",
	portName = "BinasPort",
	targetNamespace = "http://ws.binas.org/",
	serviceName = "BinasService"
)

@HandlerChain(file = "/binas-ws_handler-chain.xml")

public class BinasPortImpl implements BinasPortType {

	/**
	 * The Endpoint manager controls the Web Service instance during its whole
	 * lifecycle.
	 */
	private BinasEndpointManager endpointManager;
	public String stationsWSName;

	/** Constructor receives a reference to the endpoint manager. */
	public BinasPortImpl(BinasEndpointManager endpointManager) {
		this.endpointManager = endpointManager;
		this.stationsWSName = "T09_Station%";
	}

	// Main operations -------------------------------------------------------
	
	/** Register a user. */
	@Override
	public UserView activateUser(String email) throws InvalidEmail_Exception, EmailExists_Exception {

		UserView userView = new UserView();

		try {
			userView = buildUserView( BinasManager.getInstance().activateUser(email, stationsWSName, endpointManager) );
	
		} catch (EmailExistsException e) {
			throwEmailExists(e.getMessage());

		} catch (InvalidEmailException e) {
			throwInvalidEmail(e.getMessage());

		}

		return userView;
	}

	/** Returns the Bina the user has. */
	@Override
	public void returnBina(String stationId, String email) throws FullStation_Exception, InvalidStation_Exception, NoBinaRented_Exception, UserNotExists_Exception {
		int updatedCredit = 0;

		try {
			StationClient station = BinasManager.getInstance().getStation(stationId, stationsWSName, endpointManager);		
			int bonus = station.returnBina();
			
			BinasManager.getInstance().getUser(email).parkBina(); /* This data isnt replicated */
			updatedCredit = BinasManager.getInstance().getCredit(email, stationsWSName, endpointManager) + bonus;
			BinasManager.getInstance().setCredit(email, updatedCredit, stationsWSName, endpointManager);
		
		} catch (InvalidStationException e) {
			throwInvalidStation(e.getMessage());

		} catch (NoBinaRentedException e) {
			throwNoBinaRented(e.getMessage());

		} catch (UserNotExistsException e) {
			throwUserNotExists(e.getMessage());
		
		} catch (NoSlotAvail_Exception e) {
			throwFullStation(e.getMessage());

		} catch (UDDINamingException e) {
			System.out.printf("Caught exception when listing UDDI Records: %s%n", e);

		} catch (StationClientException e) {
			System.out.printf("Something is wrong with the client %s%n", e);
		}
	}
	
	@Override
	public void rentBina(String station, String email) throws AlreadyHasBina_Exception, InvalidStation_Exception, NoBinaAvail_Exception, NoCredit_Exception, UserNotExists_Exception {
		int updatedCredit = 0;

		try {
			int credit = BinasManager.getInstance().getCredit(email, stationsWSName, endpointManager);

			if (credit > 0) {

				BinasManager.getInstance().getStation(station, stationsWSName, endpointManager).getBina();
				BinasManager.getInstance().getUser(email).takeBina(); /* This data isnt replicated */
				BinasManager.getInstance().setCredit(email, credit - 1, stationsWSName, endpointManager);

			} else {
				throwNoCredit("No credit available.");
			}

		} catch (AlreadyHasBinaException e) {
			throwAlreadyHasBina(e.getMessage());
		
		} catch (InvalidStationException e) {
			throwInvalidStation(e.getMessage());	
		
		} catch (org.binas.station.ws.NoBinaAvail_Exception e) {
			throwNoBinaAvail(e.getMessage());

		} catch (UserNotExistsException e) {
			throwUserNotExists(e.getMessage());
		
		} catch (StationClientException e) {
			System.out.printf("Something is wrong with the client %s%n", e);

		} catch (UDDINamingException e) {
			System.out.printf("Caught exception when listing UDDI Records: %s%n", e);
		}

	}

	/* Gets the credit of a given user */
	@Override
	public int getCredit(String email) throws UserNotExists_Exception {

		int credit = 0;

		try {
			credit = BinasManager.getInstance().getCredit(email, stationsWSName, endpointManager);

		} catch (UserNotExistsException e) {
			throwUserNotExists(e.getMessage());
		}
		
		return credit;
	} 

	/* Gets the station view of the requested station. */
	@Override
	public StationView getInfoStation(String id) throws InvalidStation_Exception {

		StationView station = new StationView();

		try {
			station = buildStationView(BinasManager.getInstance().getStation(id, stationsWSName, endpointManager).getInfo());

		} catch(InvalidStationException e) {
			throwInvalidStation(e.getMessage());

		} catch (Exception e) {
			System.out.printf("Caught exception when listing UDDI Records: %s%n", e);
		}

		return station;
	}

	/* Gets the closest k stations to the coordinates given. */
	@Override
	public List<StationView> listStations(Integer k, CoordinatesView coordinates) {

		List<StationView> list = new ArrayList<StationView>();
		List<Integer> distances = new ArrayList<Integer>();

		/* For each Station Record */
		try {
			List<StationClient> stations = BinasManager.getInstance().getStations(stationsWSName, endpointManager);

			/* Connect to each station  */
			for (StationClient s : stations) {
				distances.add(Math.abs(coordinates.getX() - s.getInfo().getCoordinate().getX()) + Math.abs(coordinates.getY() - s.getInfo().getCoordinate().getY()));
			}

			int minIndex = 0;
			int minValue = distances.get(minIndex).intValue();

		 	for (int i = 0; i < k; i++) {

				for (int j = 0; j < distances.size(); j++) {

					if (distances.get(j).intValue() < minValue || distances.get(j).intValue() == minValue) {
						minValue = distances.get(j).intValue();
						minIndex = j;
					}

				}
				
				StationView station = buildStationView(stations.get(minIndex).getInfo());

				list.add(station);
		 		stations.remove(minIndex);
				distances.remove(minIndex);

		 	}

		} catch (Exception e) {
			System.out.printf("Caught exception when listing UDDI Records: %s%n", e);
		}
		
		return list;
	}

	// View helpers ----------------------------------------------------------

	/** Helper to convert a domain user to a view. */
	private UserView buildUserView(User user) {
		UserView view = new UserView();
		view.setEmail(user.getEmail());
		view.setHasBina(user.hasBina());
		view.setCredit(user.getCredit());
		return view;
	}

	/** Helper to convert a coordinate view between packages. */
	private org.binas.ws.CoordinatesView buildCoordinatesView(org.binas.station.ws.CoordinatesView coordinate) {
		org.binas.ws.CoordinatesView view = new org.binas.ws.CoordinatesView();
		view.setX(coordinate.getX());
		view.setY(coordinate.getY());
		return view;
	}

	/** Helper to convert a station view between packages. */
	private org.binas.ws.StationView buildStationView(org.binas.station.ws.StationView station) {
		org.binas.ws.StationView view = new org.binas.ws.StationView();
		view.setId(station.getId());
		view.setCoordinate(buildCoordinatesView(station.getCoordinate()));
		view.setTotalGets(station.getTotalGets());
		view.setTotalReturns(station.getTotalReturns());
		view.setFreeDocks(station.getFreeDocks());
		view.setAvailableBinas(station.getAvailableBinas());
		return view;
	}

	// Test Control operations -----------------------------------------------

	/** Diagnostic operation to check if service is running. */
	@Override
	public String testPing(String inputMessage) {

		/* Default message */
		if (inputMessage == null || inputMessage.trim().length() == 0) {
			inputMessage = "Non specified.";
		}

		/* Build a string with a message to return to Binas Client */
		StringBuilder builder = new StringBuilder();

		/* Build answer with status of Binas and Stations */
		builder.append("\n BINAS STATUS \n");
		builder.append("Binas has " + BinasManager.getInstance().getNumberOfUsers() + " registered users.");

		/* Get all Station Records */
		builder.append("\n STATIONS STATUS \n");
		try {
			for (StationClient station : BinasManager.getInstance().getStations(stationsWSName, endpointManager)) {
				builder.append(station.testPing(inputMessage) + "\n");
			}

		} catch (Exception e) {
			System.out.printf("Caught exception when listing UDDI Records: %s%n", e);
		}

		/* Returned to Binas Client */
		return builder.toString();
	}

	/** Cleans up the state of Binas and stations. */
	@Override
	public void testClear() {

		/* Delete all users from Binas */
		BinasManager.getInstance().reset();

		/* Reset all stations */
		try {
			List<StationClient> stations = BinasManager.getInstance().getStations(stationsWSName, endpointManager);
			for ( StationClient station : stations ) {
				station.testClear();
			}
		} catch (Exception e) {
			System.out.printf("Caught exception when listing UDDI Records: %s%n", e);
		}

	}

	/* Sets the Binas server with all users with the specified credit. */
	@Override
	public void testInit(int userInitialPoints) throws BadInit_Exception {

		try {
			BinasManager.getInstance().init(userInitialPoints);
		} catch (BadInitException e) {
			throwBadInit("Invalid user initialization values!");
		}

	}

	/** Sets all stations with the specificed values. */
	@Override
	public void testInitStation(String stationId, int x, int y, int capacity, int returnPrize) throws BadInit_Exception {

		/* Check if it is well initiated */
		try {
			StationClient station = BinasManager.getInstance().getStation(stationId, stationsWSName, endpointManager);
			station.testInit(x, y, capacity, returnPrize);
		} catch (Exception e) {
			throwBadInit("Invalid station initialization values!");
		}

	}

	// Exception helpers -----------------------------------------------------

	/** Helper to throw a new EmailExists exception. */
	private void throwEmailExists(final String message) throws EmailExists_Exception {
		EmailExists faultInfo = new EmailExists();
		faultInfo.message = message;
		throw new EmailExists_Exception(message, faultInfo);
	}
	
	/** Helper to throw a new InvalidEmail exception. */
	private void throwInvalidEmail(final String message) throws InvalidEmail_Exception {
		InvalidEmail faultInfo = new InvalidEmail();
		faultInfo.message = message;
		throw new InvalidEmail_Exception(message, faultInfo);
	}

	/** Helper to throw a new NoUserNotExists exception. */
	private void throwUserNotExists(final String message) throws UserNotExists_Exception {
		UserNotExists faultInfo = new UserNotExists();
		faultInfo.message = message;
		throw new UserNotExists_Exception(message, faultInfo);
	}

	/** Helper to throw a new NoUserNotExists exception. */
	private void throwNoCredit(final String message) throws NoCredit_Exception {
		NoCredit faultInfo = new NoCredit();
		faultInfo.message = message;
		throw new NoCredit_Exception(message, faultInfo);
	}

	/** Helper to throw a new InvalidStation exception. */
	private void throwInvalidStation(final String message) throws InvalidStation_Exception {
		InvalidStation faultInfo = new InvalidStation();
		faultInfo.message = message;
		throw new InvalidStation_Exception(message, faultInfo);
	}

	/** Helper to throw a new BadInit exception. */
	private void throwBadInit(final String message) throws BadInit_Exception {
		BadInit faultInfo = new BadInit();
		faultInfo.message = message;
		throw new BadInit_Exception(message, faultInfo);
	}

	/** Helper to throw a new NoBinaAvail exception. */
	private void throwNoBinaAvail(final String message) throws NoBinaAvail_Exception {
		NoBinaAvail faultInfo = new NoBinaAvail();
		faultInfo.message = message;
		throw new NoBinaAvail_Exception(message, faultInfo);
	}

	/** Helper to throw a new AlreadyHasBina exception. */
	private void throwAlreadyHasBina(final String message) throws AlreadyHasBina_Exception {
		AlreadyHasBina faultInfo = new AlreadyHasBina();
		faultInfo.message = message;
		throw new AlreadyHasBina_Exception(message, faultInfo);
	}

	/** Helper to throw a new FullStation exception. */
	private void throwFullStation(final String message) throws FullStation_Exception {
		FullStation faultInfo = new FullStation();
		faultInfo.message = message;
		throw new FullStation_Exception(message, faultInfo);
	}

	/** Helper to throw a new NoBinaRented exception. */
	private void throwNoBinaRented(final String message) throws NoBinaRented_Exception {
		NoBinaRented faultInfo = new NoBinaRented();
		faultInfo.message = message;
		throw new NoBinaRented_Exception(message, faultInfo);
	}
}
