package org.binas.station.ws.it;

import org.binas.station.ws.NoBinaAvail_Exception;

import org.junit.Test;

/**
 * Class that tests getBina operation
 */
public class getBinaIT extends BaseIT {

	@Test
	public void success() {
		try {
			client.getBina();
		} catch ( Exception e ) {
			System.out.println( e.getMessage() );
		}
	}

	@Test(expected = NoBinaAvail_Exception.class)
	public void getMaximumNumberOfBinas() throws NoBinaAvail_Exception {

		for ( int i = 0; i < client.getInfo().getCapacity(); i++ ) {
			client.getBina();
		}

		client.getBina();
	}

}
