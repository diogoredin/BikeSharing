package org.binas.domain;

import org.binas.domain.exception.InvalidEmailException;
import org.binas.domain.exception.NoBinaRentedException;
import org.binas.domain.exception.AlreadyHasBinaException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class User {

    /* Properties. */
    public String email;
    public boolean hasBina = false;
    public int credit;

    /* Constructor. */
    public User(String email, int credit) throws InvalidEmailException {
        if ( checkEmail(email) && credit > 0 ) {
            this.email = email;
            this.credit = credit;
        }
    }

    /* Gets the email of the user. */
    public String getEmail() {
        return email;
    }

    /* Teels if the user has a bina rented. */
    public boolean hasBina() {
        return hasBina;
    }

    /* Sets if the user has a bina rented. */
    public void takeBina() throws AlreadyHasBinaException {
        if (this.hasBina == true) {
            throw new AlreadyHasBinaException("User already has Bina.");
        } else {
            this.hasBina = true;
        }
    }

    /* Sets if the user has a bina rented. */
    public void parkBina() throws NoBinaRentedException {
        if (this.hasBina == false) {
            throw new NoBinaRentedException("User has no Bina.");
        } else {
            this.hasBina = false;
        }
    }

    /* Gets the credit of the client. */
    public int getCredit() {
        if ( credit < 0 ) {
            return 0;
        } else {
            return credit;
        }
    }

    /* Changes the credit of this user to the specified. */
    public void setCredit(int credit) {
        this.credit = credit;
    }

    /* Validates Email addresses. */
    public boolean checkEmail( final String email ) throws InvalidEmailException {

        Pattern pattern;
        Matcher matcher;

        String regularExpression = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

        pattern = Pattern.compile(regularExpression);
        matcher = pattern.matcher(email);
        
        if ( matcher.matches() ) {
            return true;
        } else {
            throw new InvalidEmailException("Email not well formatted.");
        }

    }

}

