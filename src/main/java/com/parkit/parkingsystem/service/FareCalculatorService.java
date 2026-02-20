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
        long durationMillis = outMillis - inMillis;


        // Calcul de la durée en heures avec double pour précision
        double durationHours = durationMillis / (1000.0 * 60 * 60);
        double price;

        // Implémentation des 30mn gratuites
        if (durationMillis <= (30 * 60 * 1000)) {
            ticket.setPrice(0.0);
        } else {
            switch (ticket.getParkingSpot().getParkingType()) {
                case CAR: {
                    price = durationHours * Fare.CAR_RATE_PER_HOUR;
                    ticket.setPrice(price);
                    break;
                }
                case BIKE: {
                    price = durationHours * Fare.BIKE_RATE_PER_HOUR;
                    ticket.setPrice(price);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unkown Parking Type");
            }
        }

        if (discount) {
            switch (ticket.getParkingSpot().getParkingType()) {
                case CAR: {
                    price = durationHours * (Fare.CAR_RATE_PER_HOUR * 0.95);
                            ticket.setPrice(price);
                            break;
                }
                case BIKE: {
                    price = durationHours * (Fare.BIKE_RATE_PER_HOUR * 0.95);
                    ticket.setPrice(price);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknow Parking Type");
            }
        }
    }



    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }
}