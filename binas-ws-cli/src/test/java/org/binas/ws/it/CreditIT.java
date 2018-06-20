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
public class CreditIT extends BaseIT {

	static UserView user;
	static String email = "test@abc.com";
	static Integer defaultCredit = 10;

	@BeforeClass
	public static void setUp() {

		try {
			user = client.activateUser(email);

		} catch (InvalidEmail_Exception e) {
			System.err.println(e.getMessage());

		} catch (EmailExists_Exception e) {
			System.err.println(e.getMessage());

		}

	}

	@After
	public void unregisterUsers() {
		client.testClear();
	}

	@Test
	public void success() {
		assertEquals(defaultCredit.intValue(), user.getCredit().intValue());
	}

	@AfterClass
	public static void cleanup() {
		client.testClear();
	}

}