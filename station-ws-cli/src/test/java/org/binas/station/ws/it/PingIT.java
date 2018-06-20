package org.binas.station.ws.it;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Class that tests Ping operation
 */
public class PingIT extends BaseIT {

	@Test
	public void pingEmptyTest() {
		assertNotNull(client.testPing("I'm running."));
	}

}
