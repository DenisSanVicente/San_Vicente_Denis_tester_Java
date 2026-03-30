package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.Date;

import static junit.framework.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    public void setUpPerTest() {
        try {
//            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
//
//            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
//
//            Ticket ticket = new Ticket();
//            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
//            ticket.setParkingSpot(parkingSpot);
//            ticket.setVehicleRegNumber("ABCDEF");
//
//            when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
//            when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
//            when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(2);
//            when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }

    /// ETAPE 5 ///
    @Test
    public void processExitingVehicleTest() throws Exception {

        // GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");

        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        verify(ticketDAO, Mockito.times(1)).getNbTicket(any(String.class));
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
    }


    @Test
    public void testProcessIncomingVehicle() throws Exception {

        /// GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // On mocke la la lecture de la lecture de la plaque
        when(inputReaderUtil.readSelection()).thenReturn(1); // On mocke le readSelection
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true); // On passe le booleéen à true car la place doit être libre

        when(ticketDAO.getNbTicket(anyString())).thenReturn(0); // On crée un ticket qui n'existe pas encore
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(parkingSpot.getId()); // On récupère le numéro de la place de parking
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true); // On passe le booléen à true car la place n'est pas encore occupée
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);

        /// WHEN
        // Appel de la méthode
        parkingService.processIncomingVehicle();

        /// THEN
        verify(ticketDAO, Mockito.times(1)).getNbTicket(any(String.class));
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class)); // On enregistre le ticket
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class)); // On met à jour le ticket
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
        /// GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(-1);

        /// WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        /// THEN
        assertNull(parkingSpot);
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        /// GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(3); // On simule une mauvaise saisie de l'utilisateur => Erreur

        /// WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable(); // Appel de la méthode

        /// THEN
        assertNull(parkingSpot); // Doit renvoyer null

    }
}




