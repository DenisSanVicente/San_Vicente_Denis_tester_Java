package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket, boolean discount) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }

        long inHour = ticket.getInTime().getTime(); // On convertit le int inHour en long inHour et on ajout .getTime()
        long outHour = ticket.getOutTime().getTime(); // Idem

        //TODO: Some tests are failing here. Need to check if this logic is correct
        float duration = (float) (outHour - inHour) / 3600000;

        // Implementation des 30mn gratuites
        if (duration < 0.5) {
            ticket.setPrice(0.0);
        } else {
            switch (ticket.getParkingSpot().getParkingType()) {
                case CAR: {
                    ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
                    break;
                }
                case BIKE: {
                    ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unkown Parking Type");
            }
        }

        if (ticket.getDiscount()) {
            ticket.setPrice(ticket.getPrice() * Fare.DISCOUNT);
        }

    }

    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }

}
