package org.binas.station.domain;

import org.binas.station.domain.exception.InvalidEmailException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class User {

    /* Properties. */
    public String email;
    public int credit;
    public int tag;

    /* Constructor. */
    public User(String email, int credit, int tag) throws InvalidEmailException {
        if ( checkEmail(email) && credit > 0 ) {
            this.email = email;
            this.credit = credit;
            this.tag = tag;
        }
    }

    /* Return the tag that indicates the freshness of this data */
	public int getTag() {
    	return tag;
    }
    
    /* Gets the email of the user. */
    public String getEmail() {
        return email;
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
    public void setCredit(int credit, int tag) {
        this.credit = credit;
        this.tag = tag;
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

