package org.binas.station.ws.it;

import org.binas.station.ws.NoSlotAvail_Exception;

import org.junit.Assert;
import org.junit.Test;

/**
 * Class that tests returnBina operation
 */
public class returnBinaIT extends BaseIT {

	@Test
	public void success() {
		try {

			Assert.assertEquals(client.getInfo().getFreeDocks(), 0);
			client.getBina();

			Assert.assertEquals(client.getInfo().getFreeDocks(), 1);
			client.returnBina();

			Assert.assertEquals(client.getInfo().getFreeDocks(), 0);

		} catch ( Exception e ) {
			System.out.println( e.getMessage() );
		}
	}

	@Test(expected = NoSlotAvail_Exception.class)
	public void returnOneBinaOnFullStation() throws NoSlotAvail_Exception {
		client.returnBina();
	}

}
