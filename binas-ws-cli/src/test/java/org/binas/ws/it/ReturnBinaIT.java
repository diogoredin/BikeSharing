package org.binas.ws.it;

import org.junit.After;
import org.junit.Before;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.AfterClass;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;
import java.util.ArrayList;

import org.binas.ws.*;
import org.apache.ws.scout.model.uddi.v2.Email;
import org.binas.station.ws.cli.*;

/**
 * Class that tests listing stations
 */
public class ReturnBinaIT extends BaseIT {

	static String email1 = "alice@T09.binas.org";
	static String email2 = "charlie@T09.binas.org";
	static String email3 = "eve@T09.binas.org";

    static String stationName1 = "T09_Station1";
	static String stationName2 = "T09_Station2";
	static String stationName3 = "T09_Station3";

	@Before
	public void setUp() {

        try {
			client.testInitStation(stationName1, 22, 7, 6, 2);
			client.testInitStation(stationName2, 80, 20, 12, 1);
			client.testInitStation(stationName3, 50, 50, 20, 0);

			client.activateUser(email1);
			client.activateUser(email2);
			client.activateUser(email3);

			client.rentBina(stationName1, email1);
			client.rentBina(stationName2, email2);
			client.rentBina(stationName3, email3);

		} catch (Exception e) {
			System.err.println(e.getMessage());
        }

	}

	@After
	public void clear() {
		client.testClear();
	}

	@Test
	public void success() {

		try {
			client.returnBina(stationName2, email1);
			assertEquals(client.getCredit(email1), 10-1+1);
			assertEquals(client.getInfoStation(stationName2).getTotalReturns(), 1);
			assertEquals(client.getInfoStation(stationName2).getFreeDocks(), 0);

			client.returnBina(stationName1, email2);
			assertEquals(client.getCredit(email2), 10-1+2);
			assertEquals(client.getInfoStation(stationName1).getTotalReturns(), 1);
			assertEquals(client.getInfoStation(stationName1).getFreeDocks(), 0);

			client.returnBina(stationName3, email3);
			assertEquals(client.getCredit(email3), 10-1+0);
			assertEquals(client.getInfoStation(stationName3).getTotalReturns(), 1);
			assertEquals(client.getInfoStation(stationName3).getFreeDocks(), 0);

		} catch (NoBinaRented_Exception e) {
			System.err.println(e.getMessage());

		} catch (UserNotExists_Exception e) {
			System.err.println(e.getMessage());	

		} catch (InvalidStation_Exception e) {
			System.err.println(e.getMessage());

		} catch (FullStation_Exception e) {
			System.err.println(e.getMessage());
		
		}
	}

	@Test(expected = UserNotExists_Exception.class)
	public void UserNotExistsTest() throws UserNotExists_Exception {

		try {
			client.returnBina(stationName1, "random@random.com");

		} catch (NoBinaRented_Exception e) {
			System.err.println(e.getMessage());

		} catch (InvalidStation_Exception e) {
			System.err.println(e.getMessage());

		} catch (FullStation_Exception e) {
			System.err.println(e.getMessage());
		
		}

	}

	@Test(expected = InvalidStation_Exception.class)
	public void InvalidStationTest() throws InvalidStation_Exception {
		
		try {
			client.returnBina("RandomStation", email1);

		} catch (NoBinaRented_Exception e) {
			System.err.println(e.getMessage());

		} catch (UserNotExists_Exception e) {
			System.err.println(e.getMessage());	

		} catch (FullStation_Exception e) {
			System.err.println(e.getMessage());
		
		}

	}

	@Test(expected = FullStation_Exception.class)
	public void FullStationTest() throws FullStation_Exception {

		try {
			client.returnBina(stationName1, email1);
			client.returnBina(stationName1, email2);

		} catch (NoBinaRented_Exception e) {
			System.err.println(e.getMessage());

		} catch (UserNotExists_Exception e) {
			System.err.println(e.getMessage());	

		} catch (InvalidStation_Exception e) {
			System.err.println(e.getMessage());

		}

	}

}
