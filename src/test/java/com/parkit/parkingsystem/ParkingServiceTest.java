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

import static org.junit.jupiter.api.Assertions.*;
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
//            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
//            Ticket ticket = new Ticket();
//            ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
//            ticket.setParkingSpot(parkingSpot);
//            ticket.setVehicleRegNumber("ABCDEF");
//            when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
//            when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
//
//            when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }

    /// /// ETAPE 5 /// ///

    @Test
    public void processExitingVehicleTest() throws Exception {

        /// GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // On mocke l'appel à l'utilisateur inputReaderUtil
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");

        when(ticketDAO.getTicket(anyString())).thenReturn(ticket); // On mocke l'appel à la création d'un ticket
        when(ticketDAO.getNbTicket(anyString())).thenReturn(1); // On mocke le nombre de ticket
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true); // On mocke la MaJ du ticket
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        /// WHEN
        parkingService.processExitingVehicle();

        /// THEN
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        verify(ticketDAO, Mockito.times(1)).getNbTicket(any(String.class));

    }

    @Test
    public void testProcessIncomingVehicle() throws Exception {
        /// GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(inputReaderUtil.readSelection()).thenReturn(1);
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true); // True car la place doit être libre à l'arrivée

        when(ticketDAO.getNbTicket(anyString())).thenReturn(0); // On crée le ticket donc return est à 0
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(parkingSpot.getId());
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);

        /// WHEN
        parkingService.processIncomingVehicle();

        /// THEN
        verify(ticketDAO, Mockito.times(1)).getNbTicket(any(String.class));
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
    }


    @Test
    public void processExitingVehicleTestUnableUpdate() throws Exception {
        /// GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        //when(inputReaderUtil.readSelection()).thenReturn(1);
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setParkingSpot(parkingSpot);

        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.getNbTicket(anyString())).thenReturn(1);
        // when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        // Ici on simule l'échec de la MaJ
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);


        ///  WHEN
        parkingService.processExitingVehicle();

        /// THEN
        // On vérifie que la MaJ du ticket a bien été tentée
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));

        // MAIS la place ne doit pas être mise à jour si le ticket échoue
        verify(parkingSpotDAO, Mockito.never()).updateParking(any(ParkingSpot.class));
    }

    @Test
    public void testGetNextParkingNumberIfAvailable() {
        /// GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(1);
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true);

        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);

        /// WHEN
        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        /// THEN
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
    }
    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
        /// GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(1);
        ParkingSpot parkingSpot = new ParkingSpot(-1, ParkingType.CAR, true); // On entre un numéro de place négatif

        /// WHEN
        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        /// THEN
        assertNull(result);
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        /// GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(3);

        /// WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        /// ASSERT
        assertNull(parkingSpot);
    }

}
