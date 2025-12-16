package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

public class ParkingService {

    private static final Logger logger = LogManager.getLogger("ParkingService");

    private FareCalculatorService fareCalculatorService = new FareCalculatorService();

    private InputReaderUtil inputReaderUtil;
    private ParkingSpotDAO parkingSpotDAO;
    private  TicketDAO ticketDAO;

    /*
    * Constructeur de ParkingService
    *
    * @param inputReaderUtil Utilitaire pour lire les entrees utilisateur
    * @param parkingSpotDAO DAO pour acceder aux emplacements du parking
    * @param ticketDAO DAO pour acceder aux tickets
    */

    public ParkingService(InputReaderUtil inputReaderUtil, ParkingSpotDAO parkingSpotDAO, TicketDAO ticketDAO){
        this.inputReaderUtil = inputReaderUtil;
        this.parkingSpotDAO = parkingSpotDAO;
        this.ticketDAO = ticketDAO;
    }

    /*
    *Traite l'arrivee d un vehicule :
    * - Recupere le prochain emplacement disponible
    *  - Demande le numero d immatriculation
    *  - Cree le ticket et le sauvegarde dans la base de donnees
    * - Marque l emplacement comme indisponible
     */

    public void processIncomingVehicle() {
        try{
           ParkingSpot parkingSpot = getNextParkingNumberIfAvailable();
            if(parkingSpot !=null && parkingSpot.getId() > 0){
                String vehicleRegNumber = getVehicleRegNumber();
                parkingSpot.setAvailable(false);
                parkingSpotDAO.updateParking(parkingSpot);
                // Indique la place comme occupee

                Date inTime = new Date();
                Ticket ticket = new Ticket();
                ticket.setParkingSpot(parkingSpot);
                ticket.setVehicleRegNumber(vehicleRegNumber);
                ticket.setPrice(0);
                ticket.setInTime(inTime);
                ticket.setOutTime(null);
                ticketDAO.saveTicket(ticket); // Enregistre le ticket en BDD
                System.out.println("Ticket generated and saved in the database");
                System.out.println("Please park your vehicle in spot number:"+parkingSpot.getId());
                System.out.println("Recorded in-time for vehicle number:"+vehicleRegNumber+" is:"+inTime);
            }
        }catch(Exception e){
            logger.error("Unable to process incoming vehicle",e);
        }
    }

    /*
    * Demande a l utilisateur le numero d immatriculation du vehicule
    * @return numero d immatriculation saisi
    * @throws Exception si la lecture echoue
    */

    public String getVehicleRegNumber() throws Exception {
        System.out.println("Please type the vehicle registration number and press enter key");
        return inputReaderUtil.readVehicleRegistrationNumber();
    }

    /*
    * Recupere le prochain emplacement de parking disponible pour le type de vehicule inscrit
    *
    * @return ParkingSpot disponible ou null si aucun emplacement
    */

    public ParkingSpot getNextParkingNumberIfAvailable(){
        int parkingNumber=0;
        ParkingSpot parkingSpot = null;
        try{
            ParkingType parkingType = getVehicleType();
            parkingNumber = parkingSpotDAO.getNextAvailableSlot(parkingType);
            if(parkingNumber > 0){
                parkingSpot = new ParkingSpot(parkingNumber,parkingType, true);
            }else{
                throw new Exception("Error fetching parking number from DB. Parking slots might be full");
            }
        }catch(IllegalArgumentException ie){
            logger.error("Error parsing user input for type of vehicle", ie);
        }catch(Exception e){
            logger.error("Error fetching next available parking slot", e);
        }
        return parkingSpot;
    }

    /*
    *Demande a l utilisateur de choisir le type de vehicule: voiture ou moto
    *
    * @return le ParkingType choisi
    */

    private ParkingType getVehicleType(){
        System.out.println("Please select vehicle type from menu");
        System.out.println("1 CAR");
        System.out.println("2 BIKE");
        int input = inputReaderUtil.readSelection();
        switch(input){
            case 1: {
                return ParkingType.CAR;
            }
            case 2: {
                return ParkingType.BIKE;
            }
            default: {
                System.out.println("Incorrect input provided");
                throw new IllegalArgumentException("Entered input is invalid");
            }
        }
    }

    /*
    * Traite la sortie d un vehicule : 
    * - Recupere le ticket correspondant
    *  - Calcule le tarif avec reduction si l utilisateur est recurrent
    *  - Met a jour le ticket et libere la place
    */

    public void processExitingVehicle(Date outTime) {
        try{
            String vehicleRegNumber = getVehicleRegNumber();
            Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);

            // Verifie si le vehicule est un utilisateur recurrent
            int nbVisits = ticketDAO.getNBTicket(vehicleRegNumber);
            boolean isRecurringUser = nbVisits > 1;           
            ticket.setOutTime(outTime);
            fareCalculatorService.calculateFare(ticket, isRecurringUser); // Calcule le tarif

            if(ticketDAO.updateTicket(ticket)) {
                ParkingSpot parkingSpot = ticket.getParkingSpot();
                parkingSpot.setAvailable(true);
                parkingSpotDAO.updateParking(parkingSpot); // libere la place
                System.out.println("Please pay the parking fare:" + ticket.getPrice());
                System.out.println("Recorded out-time for vehicle number:" + ticket.getVehicleRegNumber() + " is:" + outTime);
            }else{
                System.out.println("Unable to update ticket information. Error occurred");
            }
        }catch(Exception e){
            logger.error("Unable to process exiting vehicle",e);
        }
    }
}
