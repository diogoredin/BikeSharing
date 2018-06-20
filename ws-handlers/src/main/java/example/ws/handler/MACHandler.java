package example.ws.handler;

import java.io.IOException;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.RuntimeException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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

import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.soap.SOAPFaultException;

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
import javax.crypto.Mac;
import javax.crypto.SecretKey;

import static javax.xml.bind.DatatypeConverter.printHexBinary;

import pt.ulisboa.tecnico.sdis.kerby.*;
import pt.ulisboa.tecnico.sdis.kerby.cli.KerbyClient;

/**
 * This SOAPHandler outputs the contents of inbound and outbound messages.
 */
public class MACHandler implements SOAPHandler<SOAPMessageContext> {

	/* Exchanged between outbound and inbound and throughout execution. */
	public static final String KEY_PROPERTY = "key.property";

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
	public boolean handleMessage(SOAPMessageContext smc) throws SOAPFaultException {

		Boolean outbound = ( (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY) );
		Boolean inbound = !outbound;

		if (verbose) { System.out.println("Message intercepted."); }

		try {

			/* OUTBOUND - Add MAC to the message (on Client). */
			if (outbound && shouldBeAuthenticatedRequest(smc)) {

				if (verbose) { System.out.print("Outbound message intercepted found by MacHandler."); }

				/* Extract SOAPBody from message. */
				String SOAPBody = extractSOAPBody(smc);

				/* Extract ticket from message. */
				Ticket ticket = extractTicket(smc);
				Key clientServerKey = ticket.getKeyXY();

				/* Calculate the MAC */
				byte[] originalMAC = calculateMAC(clientServerKey, SOAPBody);

				/* Parse the SOAP response */
				SOAPMessage message = smc.getMessage();
				SOAPPart part = message.getSOAPPart();
				SOAPEnvelope envelope = part.getEnvelope();
				SOAPHeader header = envelope.getHeader();

				/* Add MAC */
				QName qMac = new QName("http://ws.binas.org/", "Mac");
				SOAPHeaderElement mac = header.addHeaderElement(qMac);
				mac.addTextNode(printHexBinary(originalMAC));

				header.getFirstChild().appendChild(mac);

				/* Save Key in shared KEY_PROPERTY to be used for outbound messages  */
				smc.put(KEY_PROPERTY, clientServerKey);

				/* Set property scope so that we can access it */
				smc.setScope(KEY_PROPERTY, Scope.APPLICATION);

				/* Finally save the changes */
				message.saveChanges();

			}

			/* OUTBOUND - Add MAC to the message (on Server). */
			else if (outbound && shouldBeAuthenticatedResponse(smc)) {

				if (verbose) { System.out.print("Outbound message intercepted found by MacHandler."); }

				/* Extract SOAPBody from message. */
				String SOAPBody = extractSOAPBody(smc);

				/* Get key saved on inbound. */
				Key clientServerKey = (Key) smc.get(KEY_PROPERTY);

				/* Calculate the MAC */
				byte[] originalMAC = calculateMAC(clientServerKey, SOAPBody);

				/* Parse the SOAP response */
				SOAPMessage message = smc.getMessage();
				SOAPPart part = message.getSOAPPart();
				SOAPEnvelope envelope = part.getEnvelope();
				SOAPHeader header = envelope.getHeader();

				/* Add MAC */
				QName qMac = new QName("http://ws.binas.org/", "Mac");
				SOAPHeaderElement mac = header.addHeaderElement(qMac);
				mac.addTextNode(printHexBinary(originalMAC));

				header.getFirstChild().appendChild(mac);

				/* Finally save the changes */
				message.saveChanges();

			}

			/* INBOUND - Verifiy MAC of the message (on Server). */
			else if (inbound && shouldBeAuthenticatedRequest(smc)) {

				if (verbose) { System.out.print("Inbound message intercepted found by MacHandler."); }

				/* Extract SOAPBody from message. */
				String SOAPBody = extractSOAPBody(smc);

				/* Extract Mac from message. */
				String extractedMAC = extractMAC(smc);

				/* Extract ticket from message. */
				Ticket ticket = extractTicket(smc);
				Key clientServerKey = ticket.getKeyXY();

				/* Calculate the MAC */
				byte[] originalMAC = calculateMAC(clientServerKey, SOAPBody);

				/* Check if MAC valid - SOAPBody wasn't changed and timestamp is still valid (same time to the seconds) */
				if (!extractedMAC.equals(printHexBinary(originalMAC))) {
					SOAPFactory soapFactory = SOAPFactory.newInstance();
					SOAPFault soapFault = soapFactory.createFault("Your request was adultered.", new QName("http://ws.binas.org/", "MacHandler")); 
					throw new SOAPFaultException(soapFault);
				}

				/* Save Key in shared KEY_PROPERTY to be used for outbound messages  */
				smc.put(KEY_PROPERTY, clientServerKey);

				/* Set property scope so that we can access it */
				smc.setScope(KEY_PROPERTY, Scope.APPLICATION);

			}

			/* INBOUND - Verifiy MAC of the message (on Client). */
			else if (inbound && shouldBeAuthenticatedResponse(smc)) {

				if (verbose) { System.out.print("Inbound message intercepted found by MacHandler."); }
			
				/* Get key saved on inbound. */
				Key clientServerKey = (Key) smc.get(KEY_PROPERTY);

				/* Extract soapBODY from message. */
				String SOAPBody = extractSOAPBody(smc);

				/* Extract Mac from message. */
				String extractedMAC = extractMAC(smc);

				/* Calculate the MAC */
				byte[] originalMAC = calculateMAC(clientServerKey, SOAPBody);

				/* Check if MAC valid - SOAPBody wasn't changed and timestamp is still valid (same time to the seconds) */
				if (!extractedMAC.equals(printHexBinary(originalMAC))) {
					SOAPFactory soapFactory = SOAPFactory.newInstance();
					SOAPFault soapFault = soapFactory.createFault("Your request was adultered.", new QName("http://ws.binas.org/", "MacHandler")); 
					throw new SOAPFaultException(soapFault);
				}

			}
		
		} catch (SOAPFaultException exception) {
			throw exception;

		} catch (Exception exception) {
			System.out.print("Caught exception in MACHandler: ");
			System.out.println(exception);

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
	 * Extract Ticket.
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
	 * Extract Auth.
	 */
	private Auth extractAuth(SOAPMessageContext smc) throws Exception {

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
				Auth auth = new Auth(argument.getFirstChild());

				return auth;
			}
		}

		return null;
	}

	/**
	 * Extract SOAPBODY as String.
	 */
	private String extractSOAPBody(SOAPMessageContext smc) throws Exception {

		/* Gets the body of the message and the specific request. */
		SOAPMessage message = smc.getMessage();
		SOAPPart part = message.getSOAPPart();
		SOAPEnvelope envelope = part.getEnvelope();
		SOAPBody body = envelope.getBody();

		/* Converts the XML to a String. */
		DOMSource dom = new DOMSource(body);
		StringWriter result = new StringWriter();
		TransformerFactory.newInstance().newTransformer().transform(dom, new StreamResult(result));

		return result.toString();
	}

	/**
	 * Extract Mac.
	 */
	private String extractMAC(SOAPMessageContext smc) throws Exception {

		/* Gets the body of the message and the specific request. */
		SOAPMessage message = smc.getMessage();
		SOAPPart part = message.getSOAPPart();
		SOAPEnvelope envelope = part.getEnvelope();
		SOAPHeader header = envelope.getHeader();
		Node firstChild = header.getFirstChild();
		NodeList children = firstChild.getChildNodes();

		/* Tries to find the mac of the function invoked. */
		for (int i = 0; i < children.getLength(); i++) {
			Node argument = children.item(i);
			String name = argument.getNodeName();

			if (name.equals("Mac")) {
				return argument.getFirstChild().getTextContent();
			}
		}

		return null;
	}

	/**
	 * Calculate the Message Authentication Code.
	 */
	private byte[] calculateMAC(Key clientServerKey, String SOAPBody) throws Exception {

		/* Instantiates the MAC cipher. */
		Mac cipher = Mac.getInstance("HmacSHA256");

		/* Initializes the cipher with the MAC Key. */
		cipher.init(clientServerKey);

		/* The MAC must be from the same day, hour and minutes (ignore seconds). */
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
		String timestamp = dateFormat.format(now);

		/* Concatenate it to the SOAPBody. */
		String MACtoCipher = SOAPBody + timestamp;

		/* Generates the digest of the plain bytes. */
		byte[] cipherDigest = cipher.doFinal(MACtoCipher.getBytes());

		return cipherDigest;
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
