package org.binas.ws.it;

import org.junit.Test;

/**
 * Test suite
 */
public class initStationIT extends BaseIT {

	@Test
	public void initStationTest() {
		try {
			client.testInitStation("T09_Station1", 22, 7, 6, 2);
		} catch ( Exception e) {
			System.err.println(e.getMessage());
		}
	}
}
