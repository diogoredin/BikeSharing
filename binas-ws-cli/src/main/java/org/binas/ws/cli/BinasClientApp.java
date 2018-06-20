package org.binas.ws.cli;

import org.binas.ws.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

public class BinasClientApp {

    public static void main(String[] args) throws Exception {

        /* Check arguments */
        if (args.length == 0) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: java " + BinasClientApp.class.getName() + " wsURL OR uddiURL wsName");
            return;
        }

        String uddiURL = null;
        String wsName = null;
        String wsURL = null;

        if (args.length == 1) {
            wsURL = args[0];
        } else if (args.length >= 2) {
            uddiURL = args[0];
            wsName = args[1];
        }

        System.out.println(wsName);
		System.out.println(BinasClientApp.class.getSimpleName() + " running");

        /* Create client */
        BinasClient client = null;

        if (wsURL != null) {
            System.out.printf("Creating client for server at %s%n", wsURL);
            client = new BinasClient(wsURL);
        } else if (uddiURL != null) {
            System.out.printf("Creating client using UDDI at %s for server with name %s%n",
                uddiURL, wsName);
            client = new BinasClient(uddiURL, wsName);
        }

        /********** FAULT TOLERANCE TESTING ZONE / SECURITY TESTING ZONE *********/

        int num = 1;
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nFAULT TOLERANCE TESTING | SECURITY TESTING");
        System.out.println("Press (0) to shutdown.");
        System.out.println("Press (1) to create three users (already registered on Kerberos).");
        System.out.println("Press (2) to get the credit from those three users.");
        System.out.println("Press (3) to rent binas for users 1 and 3.");
        System.out.println("Press (4) to return binas for users 1 and 3.");
        System.out.println("Press (5) to rent binas for user 2.");
        System.out.println("Press (6) to return binas for user 2.");
        System.out.println("Press (7) to rent binas for user alice@T09.binas.org.");

        System.out.println("\nEnter Command:");

        while((num = scanner.nextInt()) > 0) {

            if (num == 1) {
                try {
                    System.out.println(client.activateUser("alice@T09.binas.org").getEmail() + " activated.");
                    System.out.println(client.activateUser("charlie@T09.binas.org").getEmail() + " activated.");
                    System.out.println(client.activateUser("eve@T09.binas.org").getEmail() + " activated.");
                    System.out.println("DONE\n");

                } catch (Exception e) {
                    System.out.println("Something went wrong while creating the users. Error message: " + e.getMessage());
                }

            } else if (num == 2) {

                try {
                    System.out.println("alice@T09.binas.org, credit: " + client.getCredit("alice@T09.binas.org"));
                    System.out.println("charlie@T09.binas.org, credit: " + client.getCredit("charlie@T09.binas.org"));
                    System.out.println("eve@T09.binas.org, credit: " + client.getCredit("eve@T09.binas.org"));
                    System.out.println("DONE\n");
                    
                } catch (Exception e) {
                    System.out.println("Something went wrong while fetching users' balances. Error message: " + e.getMessage());
                }

            } else if (num == 3) {

                try {

                    client.rentBina("T09_Station1", "alice@T09.binas.org");
                    System.out.println("Rented Bina for alice@T09.binas.org");

                    client.rentBina("T09_Station1", "eve@T09.binas.org");
                    System.out.println("Rented Bina for eve@T09.binas.org");

                    System.out.println("DONE\n");
    
                } catch (Exception e) {
                    System.out.println("Something went wrong while renting Bina. Error message: " + e.getMessage());
                }

            } else if (num == 4) {

                try {

                    client.returnBina("T09_Station1", "alice@T09.binas.org");
                    System.out.println("Returned Bina from alice@T09.binas.org");

                    client.returnBina("T09_Station1", "eve@T09.binas.org");
                    System.out.println("Returned Bina from eve@T09.binas.org");
                    
                    System.out.println("DONE\n");
                    
                } catch (Exception e) {
                    System.out.println("Something went wrong while returning Bina. Error message: " + e.getMessage());
                }

            } else if (num == 5) {

                try {

                    client.rentBina("T09_Station1", "charlie@T09.binas.org");
                    System.out.println("Rented Bina for charlie@T09.binas.org");

                    System.out.println("DONE\n");
                    
                } catch (Exception e) {
                    System.out.println("Something went wrong while renting Bina. Error message: " + e.getMessage());
                }

            } else if (num == 6) {

                try {

                    client.returnBina("T09_Station1", "charlie@T09.binas.org");
                    System.out.println("Returned Bina from charlie@T09.binas.org");
                    
                    System.out.println("DONE\n");
                    
                } catch (Exception e) {
                    System.out.println("Something went wrong while returning Bina. Error message: " + e.getMessage());
                }

            } else if (num == 7) {

                try {

                    client.rentBina("T09_Station1", "alice@T09.binas.org");
                    System.out.println("Rented Bina for alice@T09.binas.org");

                } catch (Exception e) {
                    System.out.println("Something went wrong while returning Bina. Error message: " + e.getMessage());
                }

            }
            
        }

        scanner.close();

        System.out.println("Binas Client shutdown.");
        System.exit(1);

    }
    
}
