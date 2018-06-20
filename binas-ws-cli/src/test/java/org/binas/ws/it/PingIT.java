package org.binas.ws.it;

import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Test suite
 */
public class PingIT extends BaseIT {

	@Test
	public void pingTest() {
		assertNotNull(client.testPing("I'm running."));
	}
}
