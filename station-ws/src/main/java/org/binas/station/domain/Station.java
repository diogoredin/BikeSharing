package org.binas.station.domain;

import org.binas.station.ws.StationEndpointManager;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;

import org.binas.station.domain.exception.InvalidEmailException;
import org.binas.station.domain.exception.UserNotExistsException;
import org.binas.station.domain.exception.EmailExistsException;
import org.binas.station.domain.exception.BadInitException;
import org.binas.station.domain.exception.NoBinaAvailException;
import org.binas.station.domain.exception.NoSlotAvailException;

/** Domain Root. */
public class Station {
	
	/** Creates and returns default coordinates. */
	private static final Coordinates DEFAULT_COORDINATES = new Coordinates(5, 5);
	private static final int DEFAULT_MAX_CAPACITY = 20;
	private static final int DEFAULT_BONUS = 5; /* Changed for testing purposes */
	public static final int DEFAULT_CREDIT = 10;
	
	/** Station identifier. */
	private String id;
	/** Station data tag. */
	private int tag;
	/** Station location coordinates. */
	private Coordinates coordinates;
	/** Maximum capacity of station. */
    private int maxCapacity;
	/** Bonus for returning bike at this station. */
	private int bonus;
	/** Copy of users on the Binas server. */
	public ArrayList<User> users;

	/**
	 * Global counter of Binas Gets. Uses lock-free thread-safe single variable.
	 * This means that multiple threads can update this variable concurrently with
	 * correct synchronization.
	 */
    private AtomicInteger totalGets = new AtomicInteger(0);
    /** Global counter of Binas Returns. Uses lock-free thread-safe single variable. */
    private AtomicInteger totalReturns = new AtomicInteger(0);
    /** Global with current number of free docks. Uses lock-free thread-safe single variable. */
    private AtomicInteger freeDocks = new AtomicInteger(0);

    // Singleton -------------------------------------------------------------

 	/** Private constructor prevents instantiation from other classes. */
 	private Station() {
 		//Initialization of default values
		reset();
 	}

 	/**
 	 * SingletonHolder is loaded on the first execution of
 	 * Singleton.getInstance() or the first access to SingletonHolder.INSTANCE,
 	 * not before.
 	 */
 	private static class SingletonHolder {
 		private static final Station INSTANCE = new Station();
 	}
 	
 	/** Synchronized locks object to configure initial values */
 	public void init(int x, int y, int capacity, int returnPrize) throws BadInitException {
 		if (x < 0 || y < 0 || capacity < 0 || returnPrize < 0)
 			throw new BadInitException();
		this.coordinates = new Coordinates(x, y);
 		this.maxCapacity = capacity;
 		this.bonus = returnPrize;
 	}
 	
	public void reset() {
 		freeDocks.set(0);
 		maxCapacity = DEFAULT_MAX_CAPACITY;
 		bonus = DEFAULT_BONUS;
		coordinates = DEFAULT_COORDINATES;
		users = new ArrayList<User>();
		tag = 0;
 		
		totalGets.set(0);
		totalReturns.set(0);
	}
 	
 	public void setId(String id) {
 		this.id = id;
 	}

 	/** Synchronized locks object before attempting to return Bina */
	public int returnBina() throws NoSlotAvailException {
		if (getFreeDocks() == 0)
			throw new NoSlotAvailException();
		freeDocks.decrementAndGet();
		totalReturns.incrementAndGet();
		return getBonus();
	}

	/** Synchronized locks object before attempting to get Bina */
	public void getBina() throws NoBinaAvailException {
		if (getFreeDocks() == getMaxCapacity())
			throw new NoBinaAvailException();
		freeDocks.incrementAndGet();
		totalGets.incrementAndGet();
	}

 	// Getters -------------------------------------------------------------
 	
 	public synchronized static Station getInstance() {
 		return SingletonHolder.INSTANCE;
 	}
	
    public String getId() {
    	return id;
    }
    
	public Coordinates getCoordinates() {
    	return coordinates;
    }
    
    /** Synchronized locks object before returning max capacity */
    public int getMaxCapacity() {
    	return maxCapacity;
    }
    
    public int getTotalGets() {
    	return totalGets.get();
    }

    public int getTotalReturns() {
    	return totalReturns.get();
    }

    public int getFreeDocks() {
    	return freeDocks.get();
    }
    
    /** Synchronized locks object before returning bonus */
    public int getBonus() {
    	return bonus;
    }
    
    /** Synchronized locks object before returning available Binas */
    public int getAvailableBinas() {
    	return maxCapacity - freeDocks.get();
	}
	
	/* Gets the number of users registered. */
	public int getNumberOfUsers() {
		return users.size();
	}

	/* Return a given user the specified email registered on Binas. */
	public User getUser(String email) throws UserNotExistsException {

		for (User user : users) {
			if (user.getEmail().equals(email)) {
				return user;
			}
		}

		throw new UserNotExistsException("User with email " + email + " not found.");
	}

	/* Returns all users registered on this station. */
	public ArrayList<User> getUsers() {
		return users;
	}

	// Helpers -------------------------------------------------------------

	/* Registers a user with the given email on Binas. */
	public User activateUser(String email, int tag) throws EmailExistsException, InvalidEmailException {

		if ( emailExists(email) ) {
			throw new EmailExistsException(email);
		}

		User user = new User(email, DEFAULT_CREDIT, tag);
		users.add(user);

		return user;
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

}
