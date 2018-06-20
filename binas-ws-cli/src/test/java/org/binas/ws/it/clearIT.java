package org.binas.ws.it;

import org.junit.Test;

/**
 * Test suite
 */
public class clearIT extends BaseIT {

	@Test
	public void pingTest() {
		client.testClear();
	}
}
