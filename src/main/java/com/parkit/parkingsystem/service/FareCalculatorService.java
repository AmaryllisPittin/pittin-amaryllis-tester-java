package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    /*
    Calcul le tarif d'un ticket de parking
    *
    @param ticket le ticket à calculer
    @param discount Indique si la reduction doit etre appliquee
    @throws IllegalArgumentException si l'heure de sortie est null ou avant l'heure d'entree
    */

    public void calculateFare(Ticket ticket, boolean discount){
        // Verifie que l'heure de sortie est correcte
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime());
        }

        // Calcul de la duree de stationnement en heures
        long durationInMillis = ticket.getOutTime().getTime() - ticket.getInTime().getTime();
        double duration = durationInMillis / (1000.00 * 60 * 60);

        // Gratuit si le stationnement est inferieur ou egal a 30 minutes
        if (duration <= 0.5) {
            ticket.setPrice(0);
            return;
        }

        // Calcul du prix selon le type de vehicule
        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
                break;
            }
            case BIKE: {
                ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
                break;
            }
            default: throw new IllegalArgumentException("Unknown Parking Type");
        }

        // Reduction appliquee si necessaire
        if (discount) {
            ticket.setPrice(ticket.getPrice() * 0.95);
        }

    }
}