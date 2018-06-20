package org.binas.station.ws.it;

import static org.junit.Assert.*;
import org.junit.Test;
import org.binas.station.ws.*;
import org.binas.station.ws.cli.*;

/**
 * Class that tests getBina operation
 */
public class getInfoIT extends BaseIT {

	@Test
	public void getInfoTest() {

		try {
			assertNotNull(client.getInfo());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

	}

}
