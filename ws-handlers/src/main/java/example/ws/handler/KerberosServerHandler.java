package example.ws.handler;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import javax.xml.ws.handler.MessageContext.Scope;
import javax.xml.ws.handler.soap.SOAPHandler;
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
import javax.crypto.SecretKey;

import javax.xml.bind.DatatypeConverter;

import pt.ulisboa.tecnico.sdis.kerby.*;
import pt.ulisboa.tecnico.sdis.kerby.cli.KerbyClient;

/**
 * This SOAPHandler outputs the contents of inbound and outbound messages.
 */
public class KerberosServerHandler implements SOAPHandler<SOAPMessageContext> {

	/* Exchanged between outbound and inbound and throughout execution. */
	public static final String TREQ_PROPERTY = "treq.property";
	public static final String NOUNCE_PROPERTY = "nounce.property";
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
	 * Kerberos authentication data.
	 */
	private String validServerName = "binas@T09.binas.org";

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

			/* OUTBOUND - Add TimeRequest tag to the Details header. */
			if (outbound && shouldBeAuthenticated(smc)) {

				if (verbose) { System.out.print("Outbound message intercepted found by KerberosServerHandler."); }

				/* Parse the SOAP response. */
				SOAPMessage message = smc.getMessage();
				SOAPPart part = message.getSOAPPart();
				SOAPEnvelope envelope = part.getEnvelope();
				SOAPHeader header = envelope.getHeader();

				/* Add a header if we don't find one. */
				if (header == null) { header = envelope.addHeader(); }

				/* Create 'Parent' element in our header. */
				QName qDetails = new QName("http://ws.binas.org/", "Details");
				SOAPHeaderElement userDetails = header.addHeaderElement(qDetails);

				/* Get RequestTime from shared TREQ_PROPERTY. */
				CipherClerk cipherClerk = new CipherClerk();
				CipheredView requestTime = (CipheredView) smc.get(TREQ_PROPERTY);

				/* Add REQUEST TIME */
				Node requestTimeNode = cipherClerk.cipherToXMLNode(requestTime, "RequestTime");
				requestTimeNode = header.getOwnerDocument().importNode(requestTimeNode.getFirstChild(), true);

				userDetails.appendChild(requestTimeNode);

				/* Add NOUNCE */
				QName qNounce = new QName("http://ws.binas.org/", "Nounce");
				SOAPHeaderElement nounceNode = header.addHeaderElement(qNounce);
				nounceNode.addTextNode((String) smc.get(NOUNCE_PROPERTY));

				userDetails.appendChild(nounceNode);

				/* Finally save changes. */
				message.saveChanges();
			}

			/* INBOUND - Validate both the ticket and the authenticator. */
			if (inbound && shouldBeAuthenticated(smc)) {

				if (verbose) { System.out.print("Inbound message intercepted found by KerberosServerHandler."); }

				/* Extract email from message. */
				String email = extractEmailFromBody(smc);

				/* Extract ticket from message. */
				Ticket ticket = extractTicket(smc);
				Key clientServerKey = ticket.getKeyXY();

				/* Extract ticket from message. */
				Auth auth = extractAuth(smc, clientServerKey);

				/* Extract Nounce from message. */
				Long nounce = extractNounce(smc);
				
				/* Check if we haven't seen this request already. */
				if ( !NOUNCES.contains(nounce) ) {
					NOUNCES.add(nounce);
				} else {
					throw new RuntimeException();
				}

				/* Get the date of creation of auth and ticket (valid between 1 and 2). */
				Date currDate = new Date();
				Date authDate = auth.getTimeRequest();
				Date ticketDate1 = ticket.getTime1();
				Date ticketDate2 = ticket.getTime2();

				/* The current date should be after the authentication date. */
				/* We could have > 0 if the watches were synced, but they arent so we add a tolerance of 10 minutes. */
				if ( !(currDate.getTime() - authDate.getTime() > -36000) ) {
					throw new RuntimeException();
				}

				/* The current date should be in the interval of validity of the ticket. */
				/* We could have > 0 if the watches were synced, but they arent so we add a tolerance of 10 minutes. */
				if ( !( (currDate.getTime() - ticketDate1.getTime() > -36000) && (ticketDate2.getTime() - currDate.getTime() > -36000) ) ) {
					throw new RuntimeException();
				}

				/* Cipher it with ClientServer key. */
				RequestTime requestTime = new RequestTime(authDate);
				CipheredView cipheredRequestTime = requestTime.cipher(clientServerKey);

				/* Save cipheredRequestTime in shared TREQ_PROPERTY to be used for outbound messages. */
				smc.put(TREQ_PROPERTY, cipheredRequestTime);

				/* Set property scope so that we can access it. */
				smc.setScope(TREQ_PROPERTY, Scope.APPLICATION);

				/* Save NOUNCE in shared NOUNCE_PROPERTY to be used for outbound messages. */
				smc.put(NOUNCE_PROPERTY, Long.toString(nounce));

				/* Set property scope so that we can access it. */
				smc.setScope(NOUNCE_PROPERTY, Scope.APPLICATION);
			}

		} catch (Exception e) {
			System.out.print("Caught exception in ServerHandler: ");
			System.out.println(e.getClass());

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
	private Boolean shouldBeAuthenticated(SOAPMessageContext smc) throws Exception {

		/* Gets the body of the message and the specific request. */
		SOAPMessage message = smc.getMessage();
		SOAPPart part = message.getSOAPPart();
		SOAPEnvelope envelope = part.getEnvelope();
		SOAPBody body = envelope.getBody();
		NodeList children = body.getChildNodes();

		/* Tries to find the name of the function invoked. */
		for (int i = 0; i < children.getLength(); i++) {
			Node argument = children.item(i);
			String name = argument.getNodeName();

			if (name.equals("ns2:rentBina") || name.equals("ns2:returnBina") || name.equals("ns2:getCredit") || 
				name.equals("ns2:rentBinaResponse") || name.equals("ns2:returnBinaResponse") || name.equals("ns2:getCreditResponse" ) ) {
				return true;
			}
		}

		return false;
	}
	
	/**
	 * Extract email from the message body.
	 */
	private String extractEmailFromBody(SOAPMessageContext smc) throws Exception {

		/* Gets the body of the message and the specific request. */
		SOAPMessage message = smc.getMessage();
		SOAPPart part = message.getSOAPPart();
		SOAPEnvelope envelope = part.getEnvelope();
		SOAPBody body = envelope.getBody();
		Node firstChild = body.getFirstChild();
		NodeList children = firstChild.getChildNodes();

		/* Tries to find the email of the function invoked. */
		for (int i = 0; i < children.getLength(); i++) {
			Node argument = children.item(i);
			String name = argument.getNodeName();

			if (name.equals("email") ) {
				return argument.getFirstChild().getTextContent();
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

	/**
	 * Extract ticket from the message header.
	 */
	private Ticket extractTicket(SOAPMessageContext smc) throws Exception {

		/* Login to Server. */
		String validServerPassword = "VOL6yuFj";
		Key serverKey = SecurityHelper.generateKeyFromPassword(validServerPassword);

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

			if (name.equals("Ticket")) {

				/* CipherView. */
				CipherClerk cipherClerk = new CipherClerk();
				CipheredView cipherView = cipherClerk.cipherFromXMLNode(argument);

				/* Convert CipherView to Ticket. */
				Ticket ticket = new Ticket(cipherView, serverKey);

				return ticket;
			}
		}

		return null;
	}

	/**
	 * Extract Auth from the message header.
	 */
	private Auth extractAuth(SOAPMessageContext smc, Key clientServerKey) throws Exception {

		/* Gets the body of the message and the specific request. */
		SOAPMessage message = smc.getMessage();
		SOAPPart part = message.getSOAPPart();
		SOAPEnvelope envelope = part.getEnvelope();
		SOAPHeader header = envelope.getHeader();
		Node firstChild = header.getFirstChild();
		NodeList children = firstChild.getChildNodes();
		
		/* Tries to find the email of the function invoked. */
		for (int i = 0; i < children.getLength(); i++) {
			Node argument = children.item(i);
			String name = argument.getNodeName();

			if (name.equals("Auth")) {
				
				/* CipherView. */
				CipherClerk cipherClerk = new CipherClerk();
				CipheredView cipheredView = cipherClerk.cipherFromXMLNode(argument);

				/* Convert CipherView to Auth. */
				Auth auth = new Auth(cipheredView, clientServerKey);

				return auth;
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
