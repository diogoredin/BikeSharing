package org.binas.station.ws.cli;

import org.binas.station.ws.*;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Response;

import static javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY;
import java.util.Map;
import java.util.List;
import java.util.concurrent.Future;

import pt.ulisboa.tecnico.sdis.ws.uddi.UDDINaming;

/**
 * Client port wrapper.
 *
 * Adds easier end point address configuration to the Port generated by
 * wsimport.
 */

public class StationClient implements StationPortType {

	/** WS service */
	StationService service = null;

	/** WS port (port type is the interface, port is the implementation) */
	StationPortType port = null;

	/** UDDI server URL */
	private String uddiURL = null;

	/** WS name */
	private String wsName = null;

	/** WS end point address */
	private String wsURL = null;

	public String getWsURL() {
		return wsURL;
	}

	/** output option **/
	private boolean verbose = false;

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	/** constructor with provided web service URL */
	public StationClient(String wsURL) throws StationClientException {
		if (wsURL == null) {
			throw new NullPointerException("wsURL can't be null!");
		}
		this.wsURL = wsURL;
		createStub();
	}

	/** constructor with provided UDDI location and name */
	public StationClient(String uddiURL, String wsName) throws StationClientException {
		if (wsName == null || uddiURL == null) {
			throw new NullPointerException("wsName and uddiURL can't be null!");
		}
		this.uddiURL = uddiURL;
		this.wsName = wsName;
		uddiLookup();
		createStub();
	}

	/** UDDI lookup */
	private void uddiLookup() throws StationClientException {
		
		try {

			System.out.printf("Contacting UDDI at %s%n", uddiURL);
			UDDINaming uddiNaming = new UDDINaming(uddiURL);

			System.out.printf("Looking for '%s'%n", wsName);
			String endpointAddress = uddiNaming.lookup(wsName);

			
			if (endpointAddress == null) {
				System.out.println("Not found!");
				return;
			} else {
				System.out.printf("Found %s%n", endpointAddress);
			}

		} catch( Exception notFoundError ) {
			String message = String.format("Failed looking for '%s'%n at %s%n.", wsName, uddiURL);
		}

	}

	/** Stub creation and configuration */
	private void createStub() {
		if (verbose) { System.out.println("Creating stub..."); }

		service = new StationService();
		port = service.getStationPort();

		if (wsURL != null) {
			if (verbose)
				System.out.println("Setting endpoint address...");
				BindingProvider bindingProvider = (BindingProvider) port;
				Map<String, Object> requestContext = bindingProvider.getRequestContext();
				requestContext.put(ENDPOINT_ADDRESS_PROPERTY, wsURL);
		}
	}

	// remote invocation methods ----------------------------------------------
	@Override
	public StationView getInfo() {
		return port.getInfo();
	}

	@Override
	public void getBina() throws NoBinaAvail_Exception {
		port.getBina();
	}

	@Override
	public int returnBina() throws NoSlotAvail_Exception {
		return port.returnBina();
	}

	@Override
	public UserView activateUser(String email, int tag) throws EmailExists_Exception, InvalidEmail_Exception {
		return port.activateUser(email, tag);
	}

	@Override
	public void setBalance(String email, int balance, int tag) throws UserNotExists_Exception {
		port.setBalance(email, balance, tag);
	}

	@Override
	public List<UserView> listUsers() {
		return port.listUsers();
	}

	// test control operations ------------------------------------------------
	@Override
	public String testPing(String inputMessage) {
		return port.testPing(inputMessage);
	}

	@Override
	public void testClear() {
		port.testClear();
	}

	@Override
	public void testInit(int x, int y, int capacity, int returnPrize) throws BadInit_Exception {
		port.testInit(x, y, capacity, returnPrize);
	}

	// asynchronous calls -----------------------------------------------------
	@Override
	public Response<GetInfoResponse> getInfoAsync() {
		return port.getInfoAsync();
	}

	@Override
	public Future<?> getInfoAsync(AsyncHandler<GetInfoResponse> asyncHandler) {
		return port.getInfoAsync(asyncHandler);
	}

	@Override
	public Response<GetBinaResponse> getBinaAsync() {
		return port.getBinaAsync();
	}

	@Override
	public Future<?> getBinaAsync(AsyncHandler<GetBinaResponse> asyncHandler) {
		return port.getBinaAsync(asyncHandler);
	}

	@Override
	public Response<ReturnBinaResponse> returnBinaAsync() {
		return port.returnBinaAsync();
	}

	@Override
	public Future<?> returnBinaAsync(AsyncHandler<ReturnBinaResponse> asyncHandler) {
		return port.returnBinaAsync(asyncHandler);
	}

	@Override
	public Response<ActivateUserResponse> activateUserAsync(String email, int tag) {
		return port.activateUserAsync(email, tag);
	}

	@Override
	public Future<?> activateUserAsync(String email, int tag, AsyncHandler<ActivateUserResponse> asyncHandler) {
		return port.activateUserAsync(email, tag, asyncHandler);
	}

	@Override
	public Response<SetBalanceResponse> setBalanceAsync(String email, int balance, int tag) {
		return port.setBalanceAsync(email, balance, tag);
	}

	@Override
	public Future<?> setBalanceAsync(String email, int balance, int tag,
			AsyncHandler<SetBalanceResponse> asyncHandler) {
		return port.setBalanceAsync(email, balance, tag, asyncHandler);
	}

	@Override
	public Response<ListUsersResponse> listUsersAsync() {
		return port.listUsersAsync();
	}

	@Override
	public Future<?> listUsersAsync(AsyncHandler<ListUsersResponse> asyncHandler) {
		return port.listUsersAsync(asyncHandler);
	}

	@Override
	public Response<TestPingResponse> testPingAsync(String inputMessage) {
		return port.testPingAsync(inputMessage);
	}

	@Override
	public Future<?> testPingAsync(String inputMessage, AsyncHandler<TestPingResponse> asyncHandler) {
		return port.testPingAsync(inputMessage, asyncHandler);
	}

	@Override
	public Response<TestClearResponse> testClearAsync() {
		return port.testClearAsync();
	}

	@Override
	public Future<?> testClearAsync(AsyncHandler<TestClearResponse> asyncHandler) {
		return port.testClearAsync(asyncHandler);
	}

	@Override
	public Response<TestInitResponse> testInitAsync(int x, int y, int capacity, int returnPrize) {
		return port.testInitAsync(x, y, capacity, returnPrize);
	}

	@Override
	public Future<?> testInitAsync(int x, int y, int capacity, int returnPrize,
			AsyncHandler<TestInitResponse> asyncHandler) {
		return port.testInitAsync(x, y, capacity, returnPrize, asyncHandler);
	}
}