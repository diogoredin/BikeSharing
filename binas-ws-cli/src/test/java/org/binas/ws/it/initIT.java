package org.binas.ws.it;

import org.junit.Test;

/**
 * Test suite
 */
public class initIT extends BaseIT {

	@Test
	public void initTest() {
		try {
			client.testInit(10);
		} catch ( Exception e) {
			System.err.println(e.getMessage());
		}
	}

}
