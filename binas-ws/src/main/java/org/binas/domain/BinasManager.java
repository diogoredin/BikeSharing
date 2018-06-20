package org.binas.domain;

import org.binas.station.ws.*;
import org.binas.station.ws.cli.*;
import org.binas.ws.BinasEndpointManager;

import org.binas.domain.User;
import org.binas.domain.exception.InvalidEmailException;
import org.binas.domain.exception.UserNotExistsException;
import org.binas.domain.exception.EmailExistsException;
import org.binas.domain.exception.InvalidStationException;
import org.binas.domain.exception.BadInitException;

import pt.ulisboa.tecnico.sdis.ws.uddi.UDDINaming;
import pt.ulisboa.tecnico.sdis.ws.uddi.UDDINamingException;
import pt.ulisboa.tecnico.sdis.ws.uddi.UDDIRecord;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Collections;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.ExecutionException;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

public class BinasManager {

	public ArrayList<User> users;
	public int defaultCredit = 10;
	
	/** Global with current number of transactions. Uses lock-free thread-safe single variable. */
	private AtomicInteger tag = new AtomicInteger(0);
	
	/** output option **/
    private boolean verbose = false;

	// Singleton -------------------------------------------------------------

	private BinasManager() {
		users = new ArrayList<User>();
	}

	/**
	 * SingletonHolder is loaded on the first execution of Singleton.getInstance()
	 * or the first access to SingletonHolder.INSTANCE, not before.
	 */
	private static class SingletonHolder {
		private static final BinasManager INSTANCE = new BinasManager();
	}

	public static BinasManager getInstance() {
		return SingletonHolder.INSTANCE;
	}

	/* Specifies the credit for each user. */
	public void init(int credit) throws BadInitException {

		try {
			this.defaultCredit = credit;
			for ( User user : users ) {
				user.setCredit(credit);
			}
		} catch(Exception e) {
			throw new BadInitException(e.getMessage());
		}

	}

	/* Cleans up the users records. */
	public void reset() {
		users.clear();
	}

	/* Gets the number of existing users. */
	public int getNumberOfUsers() {
		return users.size();
	}

	/* Registered email. */
	public boolean emailExists(String email) {

		for (User user : users) {
			if (user.getEmail().equals(email)) {
				return true;
			}
		}

		return false;
	}

	/* Return a given user the specified email registered. */
	public User getUser(String email) throws UserNotExistsException {

		for (User user : users) {
			if (user.getEmail().equals(email)) {
				return user;
			}
		}

		throw new UserNotExistsException("User with email " + email + " not found.");
	}

	// Writes -------------------------------------------------------------

	/* Registers a user with the given email on Binas. */
	public synchronized User activateUser(String email, String stationsWSName, BinasEndpointManager endpointManager) throws EmailExistsException, InvalidEmailException {

		/* If the most updated tag of Binas is 0 his data is not the most updated and we can't use it */
		if (tag.get() == 0) { updateBinas(stationsWSName, endpointManager); }

		/* Check if user exists */
		if (emailExists(email)) {
			throw new EmailExistsException(email);
		}

		User user = new User(email, this.defaultCredit);
		users.add(user);

		/* Make sure all stations receive the new data */
		updateStations(email, tag.incrementAndGet(), stationsWSName, endpointManager, true);

		return user;
	}

	/* Registers a user with the given email on Binas. */
	public synchronized void setCredit(String email, int credit, String stationsWSName, BinasEndpointManager endpointManager) throws UserNotExistsException {

		/* If the most updated tag of Binas is 0 his data is not the most updated and we can't use it */
		if (tag.get() == 0) { updateBinas(stationsWSName, endpointManager); }		

		/* Check if user exists */
		if (!emailExists(email)) {
			throw new UserNotExistsException(email);
		}

		getUser(email).setCredit(credit);

		/* Make sure all stations receive the new data */
		updateStations(email, tag.incrementAndGet(), stationsWSName, endpointManager, false);
	}

	// Reads ----------------------------------------------------------------

	/* Registers a user with the given email on Binas. */
	public synchronized int getCredit(String email, String stationsWSName, BinasEndpointManager endpointManager) throws UserNotExistsException {

		/* If the most updated tag of Binas is 0 his data is not the most updated and we can't use it */
		if (tag.get() == 0) { updateBinas(stationsWSName, endpointManager); }

		/* Check if user exists */
		if (!emailExists(email)) {
			throw new UserNotExistsException(email);
		}

		return getUser(email).getCredit();
	}

	// Updaters -------------------------------------------------------------

	/* This function consults a majority of stations and updates the users records with the most updated data */
	public synchronized void updateBinas(String stationsWSName, BinasEndpointManager endpointManager) {

		try {

			/* Get a list of stations. */
			List<StationClient> stations = getStations(stationsWSName, endpointManager);
			ArrayList<List<UserView>> results = new ArrayList<List<UserView>>();

			/* Majority needed */
			int majority = (int) Math.floor(stations.size()/2) + 1;

			/* Get the tags for all stations */
			for (int i = 0; i < stations.size(); i++) {

				/* If not registered, register. */
				try {

					/* Async Call */
					stations.get(i).listUsersAsync(new AsyncHandler<ListUsersResponse>() {

						/* For each station answer we add the user's list to a result array */
						@Override
						public void handleResponse(Response<ListUsersResponse> response) {
							try {

								if ( verbose ) {
									System.out.print("Asynchronous call result arrived:\n");
									System.out.println("Number of Users reported: " + response.get().getUsers().size());
								}
								
								results.add(response.get().getUsers());

							} catch (InterruptedException e) {
								System.out.println("Caught interrupted exception.");
								System.out.print("Cause: ");
								System.out.println(e.getCause());

							} catch (ExecutionException e) {
								System.out.println("Caught execution exception.");
								System.out.print("Cause: ");
								System.out.println(e.getCause());
							}
						}

					});

				} catch ( Exception e ) {
					System.out.println("Error updating Binas.");
					updateBinas(stationsWSName, endpointManager);
				}

			}

			while (results.size() < majority && stations.size() != 0) {

				if (verbose) {
					System.out.println("Results size from stations:" + results.size());
				}

				Thread.sleep(100);
				System.out.print(".");
				System.out.flush();
			}

			/* Will hold the most recent data */
			Map<String, UserView> mostRecent = new HashMap<String, UserView>();

			/* We must update the data of each user with the data from the results that has the highest tag */
			for (List<UserView> users : results) {
				for (UserView user : users) {
					
					/* Update if we don't have this user yet or if his data is fresher. */
					if ((mostRecent.get(user.getEmail()) == null) || 
						(mostRecent.get(user.getEmail()).getTag() < user.getTag())) {
							
						mostRecent.put(user.getEmail(), user);
					}
	
				}
			}

			/* Now actually update the data on Binas */
			for (Map.Entry<String,UserView> entry : mostRecent.entrySet()) {

				String email = entry.getValue().getEmail();
				int credit = entry.getValue().getCredit();

				/* Don't forget to update the tag */
				if (tag.get() < entry.getValue().getTag()) {
					tag.set(entry.getValue().getTag());
				}

				try {
					User user = new User(email, this.defaultCredit);
					users.add(user);

				} catch (Exception e) {
					System.out.println("Couldn't add a user added on Stations.");
				}

				try {
					getUser(email).setCredit(credit);

				} catch (Exception e) {
					System.out.println("Couldn't update a users credit.");
				}

			}

		} catch (Exception e) {
			System.out.println("Error updating Binas, retrying.");
			updateBinas(stationsWSName, endpointManager);
		}

	}

	/* This function updates the data of a given User on a majority of stations */
	public synchronized void updateStations(String email, int tag, String stationsWSName, BinasEndpointManager endpointManager, boolean activateUserFlag) {

		try {

			/* Get a list of stations. */
			List<StationClient> stations = getStations(stationsWSName, endpointManager);

			/* Majority needed */
			int majority = (int) Math.floor(getStations(stationsWSName, endpointManager).size()/2) + 1;
			List<Response<ActivateUserResponse>> activateUserMajorityController = new ArrayList<Response<ActivateUserResponse>>();
			List<Response<SetBalanceResponse>> setBalanceMajorityController = new ArrayList<Response<SetBalanceResponse>>();

			if (activateUserFlag) {
				/* Get the tags from a majority of stations */
				for (int i = 0; i < stations.size(); i++) {

					/* If not registered, register. */
					try {
						stations.get(i).activateUserAsync(email, tag, new AsyncHandler<ActivateUserResponse>() {

							/* Activate specified user for each station */
							@Override
							public void handleResponse(Response<ActivateUserResponse> response) {
								try {

									if (verbose) {
										System.out.print("Asynchronous call result arrived:\n");
										System.out.println("Activated user: " + response.get().getUser().getEmail());
									}

									activateUserMajorityController.add(response);
								} catch (InterruptedException e) {
									System.out.println("Caught interrupted exception.");
									System.out.print("Cause: ");
									System.out.println(e.getCause());
								} catch (ExecutionException e) {
									System.out.println("Caught execution exception.");
									System.out.print("Cause: ");
									System.out.println(e.getCause());
								}
							}

						});
						
					} catch (Exception e) {
						System.out.println("Couldn't update data on all stations. Cause: " + e.getMessage());

					}
				}

				while (activateUserMajorityController.size() < majority) {
					Thread.sleep(100);
					System.out.print(".");
					System.out.flush();
				}
			}

			/* Get the tags from a majority of stations */
			for (int i = 0; i < stations.size(); i++) {
				
				try {
					int credit = getUser(email).getCredit();
					stations.get(i).setBalanceAsync(email, credit, tag, new AsyncHandler<SetBalanceResponse>() {

						/* Set balance for specified user */
						@Override
						public void handleResponse(Response<SetBalanceResponse> response) {
							try {

								if ( verbose ) {
									System.out.print("Asynchronous call result arrived:\n");
									System.out.println("Balance updated: " + response.get().toString());
								}

								setBalanceMajorityController.add(response);

							} catch (InterruptedException e) {
								System.out.println("Caught interrupted exception.");
								System.out.print("Cause: ");
								System.out.println(e.getCause());

							} catch (ExecutionException e) {
								System.out.println("Caught execution exception.");
								System.out.print("Cause: ");
								System.out.println(e.getCause());

							}
						}

					}); 

				} catch (Exception e) {
					System.out.println("Couldn't update data on all stations yet. Cause: " + e.getMessage());

				}
			}

			while (setBalanceMajorityController.size() < majority) {
				Thread.sleep(100);
				System.out.print(".");
				System.out.flush();
			}

		} catch (Exception e) {
			System.out.println("Error updating Stations.");

		}

	}

	// Helpers -------------------------------------------------------------

	/* Returns all connected stations. */
	public List<StationClient> getStations(String stationsWSName, BinasEndpointManager endpointManager) throws UDDINamingException, StationClientException {
		List<StationClient> stations = new ArrayList<StationClient>();
		
		UDDINaming uddiNaming = endpointManager.getUddiNaming();
		ArrayList<UDDIRecord> records = new ArrayList<UDDIRecord>(uddiNaming.listRecords(stationsWSName));

		/* Connect to each station */
		for (UDDIRecord record : records) {

			/* Create a Station Client for each Station we want to talk to */
			String stationURL = record.getUrl();
			StationClient client = new StationClient(stationURL);

			stations.add(client);
		}

		return stations;
	}

	/* Gets a specific registered station */
	public StationClient getStation(String id, String stationsWSName, BinasEndpointManager endpointManager) throws UDDINamingException, StationClientException, InvalidStationException {

		List<StationClient> stations = getStations(stationsWSName, endpointManager);

		/* Connect to each station  */
		for (StationClient station : stations) {
			if ( station.getInfo().getId().equals(id) ) {
				return station;
			}
		}

		throw new InvalidStationException("Station with id " + id + " not found.");

	}

}
