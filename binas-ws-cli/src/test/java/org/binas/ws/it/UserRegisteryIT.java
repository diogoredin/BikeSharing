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
public class UserRegisteryIT extends BaseIT {

	static UserView user;
	static String email = "alice@T09.binas.org";
	static Integer defaultCredit = 10;
	static boolean defaultHasBina = false;

	@BeforeClass
	public static void setUp() {

		try {
			user = client.activateUser(email);

		} catch ( InvalidEmail_Exception e) {
			System.err.println(e.getMessage());

		} catch ( EmailExists_Exception e) {
			System.err.println(e.getMessage());

		}

	}

	@Test
	public void success() {
		assertEquals(email, user.getEmail());
	}

	@AfterClass
	public static void cleanup() {
		client.testClear();
	}

	@Test(expected = InvalidEmail_Exception.class)
	public void emailNotWellFormattedTwoAts() throws InvalidEmail_Exception {

		try {
			String email = "@@";
			client.activateUser(email);

		} catch ( EmailExists_Exception e) {
			System.err.println(e.getMessage());

		}

	}

	@Test(expected = InvalidEmail_Exception.class)
	public void emailNotWellFormattedNoSuffix() throws InvalidEmail_Exception {

		try {
			String email = "user@localhost.";
			client.activateUser(email);

		} catch (EmailExists_Exception e) {
			System.err.println(e.getMessage());

		}

	}

	@Test(expected = InvalidEmail_Exception.class)
	public void emailNotWellFormattedSpecialCharacters() throws InvalidEmail_Exception {

		try {

			String email = "test%&/7@gmail.com";
			client.activateUser(email);

		} catch (EmailExists_Exception e) {
			System.err.println(e.getMessage());

		}

	}

	@Test(expected = InvalidEmail_Exception.class)
	public void emailNotWellFormattedEmpty() throws InvalidEmail_Exception {

		try {
			String email = "";
			client.activateUser(email);

		} catch (EmailExists_Exception e) {
			System.err.println(e.getMessage());

		}

	}

	@Test(expected = InvalidEmail_Exception.class)
	public void emailNotWellFormattedNoDomain() throws InvalidEmail_Exception {

		try {
			String email = "user@localhost";
			client.activateUser(email);

		} catch (EmailExists_Exception e) {
			System.err.println(e.getMessage());

		}

	}

	@Test(expected = InvalidEmail_Exception.class)
	public void emailNotWellFormattedNoAt() throws InvalidEmail_Exception {

		try {
			String email = "test.com";
			client.activateUser(email);

		} catch (EmailExists_Exception e) {
			System.err.println(e.getMessage());

		}

	}
	

	@Test(expected = EmailExists_Exception.class)
	public void emailExists() throws EmailExists_Exception {

		try {
			client.activateUser(email);

		} catch (InvalidEmail_Exception e) {
			System.err.println(e.getMessage());

		}

	}

}
