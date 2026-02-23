package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket, boolean discount) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }

        long inMillis = ticket.getInTime().getTime();
        long outMillis = ticket.getOutTime().getTime();

        //TODO: Some tests are failing here. Need to check if this logic is correct

        float durationMillis = (float) (outMillis - inMillis) / 3600000;

        // Calcul de la durée en heures avec double pour précision

        // Implémentation des 30mn gratuites
        if (durationMillis < 0.5) {
            ticket.setPrice(0.0);
        } else {
            switch (ticket.getParkingSpot().getParkingType()) {
                case CAR: {
                    ticket.setPrice(durationMillis * Fare.CAR_RATE_PER_HOUR);
                    break;
                }
                case BIKE: {
                    ticket.setPrice(durationMillis * Fare.BIKE_RATE_PER_HOUR);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unkown Parking Type");
            }
        }


        if (ticket.getDiscount()) {
            ticket.setPrice(ticket.getPrice() * Fare.FARE_DISCOUNT); // Remplacer 0.95 par fare.discount
        }
    }


    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }
}