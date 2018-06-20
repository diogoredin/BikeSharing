package example.ws.handler;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.lang.RuntimeException;

import javax.xml.namespace.QName;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.MessageContext.Scope;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

import static javax.xml.bind.DatatypeConverter.printHexBinary;

import pt.ulisboa.tecnico.sdis.kerby.*;
import pt.ulisboa.tecnico.sdis.kerby.cli.KerbyClient;

/**
 * This SOAPHandler outputs the contents of inbound and outbound messages.
 */
public class KerberosClientHandler implements SOAPHandler<SOAPMessageContext> {

	/* Exchanged between outbound and inbound and throughout execution. */
	public static final String REQUEST_DATE_1_PROPERTY = "request_date_1.property";
	public static final String REQUEST_DATE_2_PROPERTY = "request_date_2.property";
	public static final String KEY_PROPERTY = "key.property";
	public static ArrayList<Long> NOUNCES = new ArrayList<Long>();

	/** Date formatter used for outputting time stamp in ISO 8601 format. */
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	private boolean verbose = false;

	//
	// Handler interface implementation
	//

	/**
	 * Gets the header blocks that can be processed by this Handler instance. If
	 * null, processes all.
	 */
	@Override
	public Set<QName> getHeaders() {
		return null;
	}

	/**
	 * Kerberos login results.
	 */

	// Server
	private String validServerName = "binas@T09.binas.org";

	// Ticket, Auth and Nounce
	private CipheredView ticket;
	private CipheredView auth;
	private long nounce;

	/**
	 * The handleMessage method is invoked for normal processing of inbound and
	 * outbound messages.
	 */
	@Override
	public boolean handleMessage(SOAPMessageContext smc) {

		Boolean outbound = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
		Boolean inbound = !outbound;

		if (verbose) { System.out.println("Message intercepted."); }

		try {

			/* OUTBOUND - Add AUTH, TICKET */
			if (outbound && shouldBeAuthenticatedRequest(smc)) {

				if (verbose) { System.out.print("Outbound message to be authenticated intercepted."); }

				/* Extract user email to login to Kerberos. */
				String email = extractEmailOfUser(smc);
				String password = null;

				/* TESTING PURPOSES ONLY: Get email from properties. */
				switch(email) {

					case "alice@T09.binas.org" : password = "WD5zra6C";
					break;

					case "charlie@T09.binas.org" : password = "MVRXHy8";
					break;

					case "eve@T09.binas.org" : password = "AJyz4Bw";
					break;

				}

				/* Login to Kerberos. */
				Key clientServerKey = loginToKerberos(email, password, 30);

				/* Parse the SOAP response */
				SOAPMessage message = smc.getMessage();
				SOAPPart part = message.getSOAPPart();
				SOAPEnvelope envelope = part.getEnvelope();
				SOAPHeader header = envelope.getHeader();

				/* Adds one if empty */
				if ( header == null ) { header = envelope.addHeader(); }

				/* Create an Header 'Parent' Element */
				QName qDetails = new QName("http://ws.binas.org/", "Details");
				SOAPHeaderElement userDetails = header.addHeaderElement(qDetails);

				/* Add AUTH */
				CipherClerk cipherClerk = new CipherClerk();
				Node authNode = cipherClerk.cipherToXMLNode(auth, "Auth");
				authNode = header.getOwnerDocument().importNode(authNode.getFirstChild(), true);

				userDetails.appendChild(authNode);

				/* Add TICKET */
				Node ticketNode = cipherClerk.cipherToXMLNode(ticket, "Ticket");
				ticketNode = header.getOwnerDocument().importNode(ticketNode.getFirstChild(), true);

				userDetails.appendChild(ticketNode);

				/* Add NOUNCE */
				QName qNounce = new QName("http://ws.binas.org/", "Nounce");
				SOAPHeaderElement nounceNode = header.addHeaderElement(qNounce);
				nounceNode.addTextNode(Long.toString(nounce));

				userDetails.appendChild(nounceNode);

				/* Save the dates of the request sent to the server to compare upon response. */
				Date currDate = new Date();
				Date validUntilDate = new Date(currDate.getTime() + (30 * 60000));

				/* Save current date and the time when the ticket expires. */
				smc.put(REQUEST_DATE_1_PROPERTY, currDate);
				smc.setScope(REQUEST_DATE_1_PROPERTY, Scope.APPLICATION);

				smc.put(REQUEST_DATE_2_PROPERTY, validUntilDate);
				smc.setScope(REQUEST_DATE_2_PROPERTY, Scope.APPLICATION);

				/* Save Key in shared KEY_PROPERTY to be used for outbound messages  */
				smc.put(KEY_PROPERTY, clientServerKey);
				smc.setScope(KEY_PROPERTY, Scope.APPLICATION);

				/* Finally save the changes */
				message.saveChanges();

			}

			if (inbound && shouldBeAuthenticatedResponse(smc)) {

				/* Get key saved on inbound. */
				Key clientServerKey = (Key) smc.get(KEY_PROPERTY);

				/* Get the request time unciphered from the message. */
				RequestTime requestTime = extractRequestTime(smc, clientServerKey);

				/* Extract Nounce from message. */
				Long localNounce = extractNounce(smc);

				/* Check if we haven't seen this request already. */
				if ( !NOUNCES.contains(localNounce) ) {
					NOUNCES.add(localNounce);
				} else {
					throw new RuntimeException();
				}

				/* Get the date of the ticket (valid between 1 and 2). */
				Date currDate = new Date();
				Date ticketDate1 = (Date) smc.get(REQUEST_DATE_1_PROPERTY);
				Date ticketDate2 = (Date) smc.get(REQUEST_DATE_2_PROPERTY);

				if (verbose) {
					System.out.println("Current = " + currDate.getTime() + " Ticket 1st =" + ticketDate1.getTime() + " Ticket 2nd =" + ticketDate2.getTime());
				}

				/* The current date should be in the interval of validity of the ticket. */
				/* TimeRequest should equal the time the request was sent. */
				/* We could have > 0 if the watches were synced, but they arent so we add a tolerance of 10 minutes. */
				if ( !( (currDate.getTime() - ticketDate1.getTime() > -36000) && (ticketDate2.getTime() - currDate.getTime() > -36000) &&
						(requestTime.getTimeRequest().getTime() - ticketDate1.getTime() > -36000) )) {
					throw new RuntimeException();
				}

			}

		} catch (Exception e) {
			System.out.print("Caught exception in ClientHandler: ");
			System.out.println(e);
		}

        if (verbose) { logSOAPMessage(smc, System.out); }
        return true;
	}

	//
	// Auxiliary functions
	//

	/**
	 * Given a certain intercepted message indicates whether it should be encrypted.
	 * Messages to be Encripted include: rentBina(id, email), returnBina(station,email), getCredit(email)
	 */
	private Boolean shouldBeAuthenticatedRequest(SOAPMessageContext smc) throws Exception {

		/* Gets the body of the message and the specific request. */
		SOAPMessage message = smc.getMessage();
		SOAPPart part = message.getSOAPPart();
		SOAPEnvelope envelope = part.getEnvelope();
		SOAPBody body = envelope.getBody();
		NodeList children = body.getChildNodes();
		
		/* Tries to find the name of the function invoked. */
		for (int i=0; i < children.getLength(); i++) {
			Node argument = children.item(i);
			String name = argument.getNodeName();

			if (name.equals("ns2:rentBina") || name.equals("ns2:returnBina") || name.equals("ns2:getCredit") ) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Given a certain intercepted message indicates whether it should be encrypted.
	 * Messages to be Encripted include: rentBina(id, email), returnBina(station,email), getCredit(email)
	 */
	private Boolean shouldBeAuthenticatedResponse(SOAPMessageContext smc) throws Exception {

		/* Gets the body of the message and the specific request. */
		SOAPMessage message = smc.getMessage();
		SOAPPart part = message.getSOAPPart();
		SOAPEnvelope envelope = part.getEnvelope();
		SOAPBody body = envelope.getBody();
		NodeList children = body.getChildNodes();
		
		/* Tries to find the name of the function invoked. */
		for (int i=0; i < children.getLength(); i++) {
			Node argument = children.item(i);
			String name = argument.getNodeName();

			if (name.equals("ns2:rentBinaResponse") || name.equals("ns2:returnBinaResponse") || name.equals("ns2:getCreditResponse") ) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Login to Kerberos.
	 */
	private Key loginToKerberos(String validClientName, String validClientPassword, int validDuration) throws Exception {

		/* Connect to Kerberos */
		KerbyClient kerby = new KerbyClient("http://sec.sd.rnl.tecnico.ulisboa.pt:8888/kerby");
		if (verbose) { System.out.println("KerbyClient created at http://sec.sd.rnl.tecnico.ulisboa.pt:8888/kerby"); }

		/* Get a client key */
		Key clientKey = SecurityHelper.generateKeyFromPassword(validClientPassword);

		/* Generate a nounce */
		SecureRandom randomGenerator = new SecureRandom();
		nounce = randomGenerator.nextLong();

		/* Request a ticket */
		SessionKeyAndTicketView result = kerby.requestTicket(validClientName, validServerName, nounce, validDuration);
		if (verbose) {  System.out.println("Session Key and Ticket View received."); }

		/* Open the session key with the client key */
		CipheredView cipheredSessionKey = result.getSessionKey();
		SessionKey sessionKey = new SessionKey(cipheredSessionKey, clientKey);
		Key clientServerKey = sessionKey.getKeyXY();

		/* Save the already ciphered ticket */
		ticket = result.getTicket();

		/* Create an authenticator */
		Date currDate = new Date();
		Auth plainAuth = new Auth(validClientName, currDate);
		if (verbose) { System.out.println("Auth contains: " + plainAuth.toString()); }

		/* Cipher the auth */
		auth = plainAuth.cipher(clientServerKey);

		return clientServerKey;
	}

	/**
	 * Given a certain message / invoked function we extract the users's email so we can login the user
	 * to the Kerberos system.
	 */
	private String extractEmailOfUser(SOAPMessageContext smc) throws Exception {

		/* Gets the body of the message and the specific request. */
		SOAPMessage message = smc.getMessage();
		SOAPPart part = message.getSOAPPart();
		SOAPEnvelope envelope = part.getEnvelope();
		SOAPBody body = envelope.getBody();
		Node firstChild = body.getFirstChild();
		NodeList children = firstChild.getChildNodes();
		
		/* Tries to find the email of the function invoked. */
		for (int i=0; i < children.getLength(); i++) {
			Node argument = children.item(i);
			String name = argument.getNodeName();

			if (name.equals("email") ) {
				return argument.getFirstChild().getTextContent();
			}
		}

		return null;
	}

	/**
	 * Extract TimeRequest from server response.
	 */
	private RequestTime extractRequestTime(SOAPMessageContext smc, Key clientServerKey) throws Exception {

		/* Gets the body of the message and the specific request. */
		SOAPMessage message = smc.getMessage();
		SOAPPart part = message.getSOAPPart();
		SOAPEnvelope envelope = part.getEnvelope();
		SOAPHeader header = envelope.getHeader();
		Node firstChild = header.getFirstChild();
		NodeList children = firstChild.getChildNodes();

		/* Tries to find the ticket of the function invoked. */
		for (int i = 0; i < children.getLength(); i++) {
			Node argument = children.item(i);
			String name = argument.getNodeName();

			if (name.equals("RequestTime")) {

				/* CipherView. */
				CipherClerk cipherClerk = new CipherClerk();
				CipheredView cipherView = cipherClerk.cipherFromXMLNode(argument);

				/* Convert CipherView to TimeRequest. */
				RequestTime requestTime = new RequestTime(cipherView, clientServerKey);

				return requestTime;
			}
		}

		return null;
	}

	/**
	 * Extract Nounce.
	 */
	private Long extractNounce(SOAPMessageContext smc) throws Exception {

		/* Gets the body of the message and the specific request. */
		SOAPMessage message = smc.getMessage();
		SOAPPart part = message.getSOAPPart();
		SOAPEnvelope envelope = part.getEnvelope();
		SOAPHeader header = envelope.getHeader();
		Node firstChild = header.getFirstChild();
		NodeList children = firstChild.getChildNodes();

		/* Tries to find the nounce of the function invoked. */
		for (int i = 0; i < children.getLength(); i++) {
			Node argument = children.item(i);
			String name = argument.getNodeName();

			if (name.equals("Nounce")) {
				String nounceAsString = argument.getFirstChild().getTextContent();
				long nounceAsPrimitive = Long.valueOf(nounceAsString).longValue();

				return new Long(nounceAsPrimitive);
			}
		}

		return null;
	}

	/** The handleFault method is invoked for fault message processing. */
	@Override
	public boolean handleFault(SOAPMessageContext smc) {
		logSOAPMessage(smc, System.out);
		return true;
	}

	/**
	 * Called at the conclusion of a message exchange pattern just prior to the
	 * JAX-WS runtime dispatching a message, fault or exception.
	 */
	@Override
	public void close(MessageContext messageContext) {
		// nothing to clean up
	}

	/**
	 * Check the MESSAGE_OUTBOUND_PROPERTY in the context to see if this is an
	 * outgoing or incoming message. Write the SOAP message to the print stream. The
	 * writeTo() method can throw SOAPException or IOException.
	 */
	private void logSOAPMessage(SOAPMessageContext smc, PrintStream out) {
		Boolean outbound = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

		// print current time stamp
		Date now = new Date();
		out.print(dateFormat.format(now));

		// print SOAP message direction
		out.println(" " + (outbound ? "OUT" : "IN") + "bound SOAP message:");

		// print SOAP message contents
		SOAPMessage message = smc.getMessage();
		try {
			message.writeTo(out);
			// print a newline after message
			out.println();

		} catch (SOAPException se) {
			out.print("Ignoring SOAPException in handler: ");
			out.println(se);
		} catch (IOException ioe) {
			out.print("Ignoring IOException in handler: ");
			out.println(ioe);
		}
	}

}
