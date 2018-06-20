package org.binas.ws.it;

import org.junit.After;
import org.junit.Before;

import org.junit.BeforeClass;
import org.junit.AfterClass;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;
import java.util.ArrayList;

import org.binas.ws.*;
import org.binas.station.ws.cli.*;

/**
 * Class that tests listing stations
 */
public class RentBinaIT extends BaseIT {

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
			client.rentBina(stationName1, email1);
			assertEquals(client.getCredit(email1), 10-1); // 10 Initial - 1 Cost   
			assertEquals(client.getInfoStation(stationName1).getTotalGets(), 1); // 1 Get
			assertEquals(client.getInfoStation(stationName1).getFreeDocks(), 1); // 6 Capacity
		
			client.rentBina(stationName2, email2);
			assertEquals(client.getCredit(email1), 10-1); // 10 Initial - 1 Cost   
			assertEquals(client.getInfoStation(stationName1).getTotalGets(), 1); // 1 Get
			assertEquals(client.getInfoStation(stationName1).getFreeDocks(), 1); // 12 Capacity

			client.rentBina(stationName3, email3);
			assertEquals(client.getCredit(email3), 10-1); // 10 Initial - 1 Cost   
			assertEquals(client.getInfoStation(stationName1).getTotalGets(), 1); // 1 Get
			assertEquals(client.getInfoStation(stationName1).getFreeDocks(), 1); // 20 Capacity

			client.rentBina(stationName3, email1);
			assertEquals(client.getCredit(email1), 9-1); // 9 Initial - 1 Cost   
			assertEquals(client.getInfoStation(stationName3).getTotalGets(), 2); // 1 Get
			assertEquals(client.getInfoStation(stationName1).getFreeDocks(), 1); // 19 Capacity

		} catch (AlreadyHasBina_Exception e) {
			System.err.println(e.getMessage());

		} catch (InvalidStation_Exception e) {
			System.err.println(e.getMessage());	

		} catch (NoBinaAvail_Exception e) {
			System.err.println(e.getMessage());

		} catch (NoCredit_Exception e) {
			System.err.println(e.getMessage());
		
		} catch (UserNotExists_Exception e) {
			System.err.println(e.getMessage());
			
		}
	}

	@Test(expected = AlreadyHasBina_Exception.class)
	public void AlreadyHasBinaTest() throws AlreadyHasBina_Exception {

		try {
			client.rentBina(stationName1, email1);
			client.rentBina(stationName2, email1);

		} catch (InvalidStation_Exception e) {
			System.err.println(e.getMessage());	

		} catch (NoBinaAvail_Exception e) {
			System.err.println(e.getMessage());

		} catch (NoCredit_Exception e) {
			System.err.println(e.getMessage());
		
		}  catch (UserNotExists_Exception e) {
			System.err.println(e.getMessage());
		}
	}

	@Test(expected = InvalidStation_Exception.class)
	public void InvalidStationTest() throws InvalidStation_Exception {

		try {
			client.rentBina("TestStation", email1);

		} catch (AlreadyHasBina_Exception e) {
			System.err.println(e.getMessage());
		
		} catch (NoBinaAvail_Exception e) {
			System.err.println(e.getMessage());

		} catch (NoCredit_Exception e) {
			System.err.println(e.getMessage());
		
		} catch (UserNotExists_Exception e) {
			System.err.println(e.getMessage());
			
		}

	}

	@Test(expected = NoBinaAvail_Exception.class)
	public void NoBinaAvailTest() throws NoBinaAvail_Exception {
		
		try {
			client.testInitStation(stationName1, 22, 7, 0, 2);
			client.rentBina(stationName1, email1);

		} catch (AlreadyHasBina_Exception e) {
			System.err.println(e.getMessage());
		
		} catch (InvalidStation_Exception e) {
			System.err.println(e.getMessage());	

		} catch (NoCredit_Exception e) {
			System.err.println(e.getMessage());
		
		} catch (UserNotExists_Exception e) {
			System.err.println(e.getMessage());
			
		} catch (BadInit_Exception e) {
			System.err.println(e.getMessage());
        }

	}

	@Test(expected = NoCredit_Exception.class)
	public void NoCreditTest() throws NoCredit_Exception {

		try {

			client.testInit(0);
			client.rentBina(stationName1, email1);

		} catch (AlreadyHasBina_Exception e) {
			System.err.println(e.getMessage());
		
		} catch (InvalidStation_Exception e) {
			System.err.println(e.getMessage());	

		} catch (NoBinaAvail_Exception e) {
			System.err.println(e.getMessage());

		} catch (UserNotExists_Exception e) {
			System.err.println(e.getMessage());
			
		} catch (BadInit_Exception e) {
			System.err.println(e.getMessage());

        }

	}

	@Test(expected = UserNotExists_Exception.class)
	public void UserNotExistsTest() throws UserNotExists_Exception {

		try {
			client.rentBina(stationName1, "random@test.com");

		} catch (AlreadyHasBina_Exception e) {
			System.err.println(e.getMessage());
		
		} catch (InvalidStation_Exception e) {
			System.err.println(e.getMessage());

		} catch (NoBinaAvail_Exception e) {
			System.err.println(e.getMessage());

		} catch (NoCredit_Exception e) {
			System.err.println(e.getMessage());
		
		}

	}

}
