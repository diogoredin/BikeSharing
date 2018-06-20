package org.binas.ws.it;

import org.binas.ws.*;

import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Class that tests register operation
 */
public class getStationIT extends BaseIT {

	static String email = "test@abc.com";
	static Integer defaultCredit = 10;
	
	static String stationName1 = "T09_Station1";
	static String stationName2 = "T09_Station2";
	static String stationName3 = "T09_Station3";

	@BeforeClass
	public static void setUp() {

		try {
			client.testInitStation(stationName1, 22, 7, 6, 2);
			client.testInitStation(stationName2, 80, 20, 12, 1);
			client.testInitStation(stationName2, 50, 50, 20, 0);

		} catch ( Exception e) {
			System.err.println(e.getMessage());
		}

	}

	@After
	public void unregisterUsers() {
		client.testClear();
	}

	@Test
	public void success() {

		try {
			assertEquals(stationName1, client.getInfoStation(stationName1).getId());
			assertEquals(stationName2, client.getInfoStation(stationName2).getId());
			assertEquals(stationName3, client.getInfoStation(stationName3).getId());

		} catch ( InvalidStation_Exception e) {
			System.err.println(e.getMessage());
		}

	}

	@AfterClass
	public static void cleanup() {
		client.testClear();
	}

}
