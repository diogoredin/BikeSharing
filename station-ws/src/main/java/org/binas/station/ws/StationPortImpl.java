package org.binas.station.ws;

import java.util.concurrent.Future;

import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import org.binas.station.domain.Station;
import org.binas.station.domain.Coordinates;
import org.binas.station.domain.User;
import org.binas.station.domain.exception.EmailExistsException;
import org.binas.station.domain.exception.InvalidEmailException;
import org.binas.station.domain.exception.UserNotExistsException;
import org.binas.station.domain.exception.BadInitException;
import org.binas.station.domain.exception.NoSlotAvailException;
import org.binas.station.domain.exception.NoBinaAvailException;

import java.util.List;
import java.util.ArrayList;

/**
 * This class implements the Web Service port type (interface). The annotations
 * below "map" the Java class to the WSDL definitions.
 */

@WebService(
	endpointInterface = "org.binas.station.ws.StationPortType",
	wsdlLocation = "station.2_0.wsdl",
	name = "StationWebService",
	portName = "StationPort",
	targetNamespace = "http://ws.station.binas.org/",
	serviceName = "StationService"
)

public class StationPortImpl implements StationPortType {

	/**
	 * The Endpoint manager controls the Web Service instance during its whole
	 * lifecycle.
	 */
	private StationEndpointManager endpointManager;

	/** Constructor receives a reference to the endpoint manager. */
	public StationPortImpl(StationEndpointManager endpointManager) {
		this.endpointManager = endpointManager;
	}

	// Main operations -------------------------------------------------------
	
	@Override
	public StationView getInfo() {
		return buildStationView(Station.getInstance());
	}

	@Override
	public synchronized UserView activateUser(String email, int tag) throws InvalidEmail_Exception, EmailExists_Exception {

		UserView userView = new UserView();

		try {
			userView = buildUserView( Station.getInstance().activateUser(email, tag) );
	
		} catch (EmailExistsException e) {
			throwEmailExists(e.getMessage());

		} catch (InvalidEmailException e) {
			throwInvalidEmail(e.getMessage());

		}

		return userView;
	}

	@Override
	public synchronized void setBalance(String email, int credit, int tag) throws UserNotExists_Exception {

		try {
			Station.getInstance().getUser(email).setCredit(credit, tag);

		} catch (UserNotExistsException e) {
			throwUserNotExists(e.getMessage());

		}
	}

	/* Used to fetch balances of all users and update their data on Binas if needed */
	@Override
	public List<UserView> listUsers() {

		List<UserView> result = new ArrayList<UserView>();

		for ( User user : Station.getInstance().getUsers() ) {
			result.add(buildUserView(user));
		}

		return result;
	}

	@Override
	public synchronized void getBina() throws NoBinaAvail_Exception {
		try {
			Station.getInstance().getBina();
		} catch(NoBinaAvailException e) {
			throwNoBinaAvail(e.getMessage());
		}
	}

	@Override
	public synchronized int returnBina() throws NoSlotAvail_Exception {
		int binas = 0;
		try {
			binas = Station.getInstance().returnBina();
		} catch(NoSlotAvailException e) {
			throwNoSlotAvail(e.getMessage());
		}
		return binas;
	}

	// Test Control operations -----------------------------------------------

	/** Diagnostic operation to check if service is running. */
	@Override
	public String testPing(String inputMessage) {

		/* If no input is received, return a default name. */
		if (inputMessage == null || inputMessage.trim().length() == 0) {
			inputMessage = "Non specified.";
		}
		
		/* If the station does not have a name, return a default. */
		String wsName = endpointManager.getWsName();
		if (wsName == null || wsName.trim().length() == 0) {
			wsName = "unknown";
		}
		
		/* Build a string with a message to return. */
		StringBuilder builder = new StringBuilder();

		/* Station status. */
		Station station = Station.getInstance();
		station.getId();
		int bonus = station.getBonus();
		int x = station.getCoordinates().getX();
		int y = station.getCoordinates().getY();
		int free = station.getFreeDocks();
		int max = station.getMaxCapacity();
		int totalGets = station.getTotalGets();
		int totalReturns = station.getTotalReturns();

		/* Build the string. */
		builder.append("Station ").append(wsName);
		builder.append(" @ ").append(x);
		builder.append(", ").append(y);
		builder.append(" has Bonus: ").append(bonus);
		builder.append(" | Free Docks: ").append(free);
		builder.append(" | Max Capacity: ").append(max);
		builder.append(" | Binas Rented: ").append(totalGets);
		builder.append(" | Binas Returned: ").append(totalReturns);
		return builder.toString();
	}

	/** Return all station variables to default values. */
	@Override
	public void testClear() {
		Station.getInstance().reset();
	}

	/** Set station variables with specific values. */
	@Override
	public void testInit(int x, int y, int capacity, int returnPrize) throws BadInit_Exception {
		try {
			Station.getInstance().init(x, y, capacity, returnPrize);
		} catch (BadInitException e) {
			throwBadInit("Invalid initialization values!");
		}
	}

	// View helpers ----------------------------------------------------------

	/** Helper to convert a domain user to a view. */
	private UserView buildUserView(User user) {
		UserView view = new UserView();
		view.setEmail(user.getEmail());
		view.setCredit(user.getCredit());
		view.setTag(user.getTag());
		return view;
	}	

	/** Helper to convert a domain station to a view. */
	private StationView buildStationView(Station station) {
		StationView view = new StationView();
		view.setId(station.getId());
		view.setCoordinate(buildCoordinatesView(station.getCoordinates()));
		view.setCapacity(station.getMaxCapacity());
		view.setTotalGets(station.getTotalGets());
		view.setTotalReturns(station.getTotalReturns());
		view.setFreeDocks(station.getFreeDocks());
		view.setAvailableBinas(station.getAvailableBinas());
		return view;
	}

	/** Helper to convert a domain coordinates to a view. */
	private CoordinatesView buildCoordinatesView(Coordinates coordinates) {
		CoordinatesView view = new CoordinatesView();
		view.setX(coordinates.getX());
		view.setY(coordinates.getY());
		return view;
	}

	// Exception helpers -----------------------------------------------------

	/** Helper to throw a new NoBinaAvail exception. */
	private void throwNoBinaAvail(final String message) throws NoBinaAvail_Exception {
		NoBinaAvail faultInfo = new NoBinaAvail();
		faultInfo.message = message;
		throw new NoBinaAvail_Exception(message, faultInfo);
	}

	/** Helper to throw a new NoSlotAvail exception. */
	private void throwNoSlotAvail(final String message) throws NoSlotAvail_Exception {
		NoSlotAvail faultInfo = new NoSlotAvail();
		faultInfo.message = message;
		throw new NoSlotAvail_Exception(message, faultInfo);
	}

	/** Helper to throw a new BadInit exception. */
	private void throwBadInit(final String message) throws BadInit_Exception {
		BadInit faultInfo = new BadInit();
		faultInfo.message = message;
		throw new BadInit_Exception(message, faultInfo);
	}

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

}
