package com.parkit.parkingsystem.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Scanner;

/**
 * Utilitaire de lecture des entrees utilisateur depuis la console.
 */

public class InputReaderUtil {

    private static Scanner scan = new Scanner(System.in);
    private static final Logger logger = LogManager.getLogger("InputReaderUtil");

    /**
     * Lit une selection utilisateur depuis la console
     * 
     * @return le choix saisi ou -1 en cas d erreur de lecture
     */

    public int readSelection() {
        try {
            int input = Integer.parseInt(scan.nextLine());
            return input;
        }catch(Exception e){
            logger.error("Error while reading user input from Shell", e);
            System.out.println("Error reading input. Please enter valid number for proceeding further");
            return -1;
        }
    }

    /**
     * Lit le numero d immatriculation d un vehicule
     * 
     * @return numero d immatriculation valide
     * @throws Exception si la saisie est vide ou invalide
     */

    public String readVehicleRegistrationNumber() throws Exception {
        try {
            String vehicleRegNumber= scan.nextLine();
            if(vehicleRegNumber == null || vehicleRegNumber.trim().length()==0) {
                throw new IllegalArgumentException("Invalid input provided");
            }
            return vehicleRegNumber;
        }catch(Exception e){
            logger.error("Error while reading user input from Shell", e);
            System.out.println("Error reading input. Please enter a valid string for vehicle registration number");
            throw e;
        }
    }


}
