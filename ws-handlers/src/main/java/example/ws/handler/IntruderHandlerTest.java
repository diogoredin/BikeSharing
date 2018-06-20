package example.ws.handler;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

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
public class IntruderHandlerTest implements SOAPHandler<SOAPMessageContext> {

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
		if (verbose) { System.out.println("Message intercepted."); }

		try {

			/* OUTBOUND - Intercept sensitive message */
			if (outbound) {

				if (verbose) { System.out.print("Outbound message to Binas with sensitive information intercepted."); }

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

					/* Only capture sensitive messages */
					if (name.equals("ns2:rentBina") || name.equals("ns2:returnBina") || name.equals("ns2:getCredit") ) {
						
						/* Get childs of the request */
						NodeList argumentsChildren = argument.getChildNodes();

						/* Tries to find the email of the function invoked. */
						for (int j=0; j < argumentsChildren.getLength(); j++) {
							Node argumentIn = argumentsChildren.item(j);
							String nameIn = argumentIn.getNodeName();

							/* Replace the email with another Kerberos user. */
							if (nameIn.equals("email") ) {
								argumentIn.setTextContent("charlie@T09.binas.org");
							}
						}

					}
				}

				/* Finally save the changes */
				message.saveChanges();

			}

		} catch (Exception e) {
			System.out.print("Caught exception in handleMessage: ");
			System.out.println(e);
			System.out.println("Normal processing continued...");
		}

        if (verbose) { logSOAPMessage(smc, System.out); }
        return true;
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
