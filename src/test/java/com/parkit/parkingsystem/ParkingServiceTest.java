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
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // On mocke la saisie de l'immatriculation et retourne une valeur
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

        // Simulation de l'entrée du véhicule
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // On simule 1h de stationnement
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
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // On simule la saisie de l'utilisateur
        when(inputReaderUtil.readSelection()).thenReturn(1); // On simule l'arrivée d'une voiture
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true); // True car la place doit être libre à l'arrivée

        when(ticketDAO.getNbTicket(anyString())).thenReturn(0); // Le véhicule n'est jamais venu donc return 0
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(parkingSpot.getId()); // Cherche une place libre et retourne le numéro de la place
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true); // MaJ de la place qui sera marquée comme occupée
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true); // Sauvegarde du ticket en base

        /// WHEN
        // Appel de la méthode processIncomingVehicle que l'on teste
        parkingService.processIncomingVehicle();

        /// THEN
        verify(ticketDAO, Mockito.times(1)).getNbTicket(any(String.class)); // On vérifie si le véhicule est déjà venu
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class)); // On vérifie que le ticket a été crée
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class)); // On vérifie que la place a été MàJ
    }


    @Test
    public void processExitingVehicleTestUnableUpdate() throws Exception {
        /// GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // On simule la saisie de l'utilisateur
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false); // La place est occupée

        // Création d'un nouveau ticket
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // On simule 1h de stationnement
        ticket.setVehicleRegNumber("ABCDEF"); // On entre l'immatriculation qui doit être identique à celle qui a été moquée
        ticket.setParkingSpot(parkingSpot); //

        when(ticketDAO.getTicket(anyString())).thenReturn(ticket); // On vérifie que le ticket a bien été crée
        when(ticketDAO.getNbTicket(anyString())).thenReturn(1); // Passage unique

        // Ici on simule l'échec de la MaJ
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false); // C'EST ICI qu'on simule l'échec de l'update


        ///  WHEN
        // Appel de la méthode
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
        when(inputReaderUtil.readSelection()).thenReturn(1); // On simule l'arrivée d'une voiture
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true); // La place est libre à l'arrivée

        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1); // Numéro de la place libre

        /// WHEN
        // Appel de la méthode
        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        /// THEN
        assertNotNull(result); // La méthode doit retourner un résultat
        assertEquals(1, result.getId()); // Le spot libre doit être le 1
        verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class)); // On vérifie que la méthode a bien été appelée
    }
    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
        /// GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(1);
        ParkingSpot parkingSpot = new ParkingSpot(-1, ParkingType.CAR, true); // On entre un numéro de place négatif qui n'existe pas

        /// WHEN
        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        /// THEN
        assertNull(result); // Le résultat doit être null car la place de parking n'existe pas
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        /// GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(3); // Le choix 3 n'est pas disponible

        /// WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        /// ASSERT
        assertNull(parkingSpot); // On vérifie que l'application n'a pas utilisé ParkingSpot car l'application n'est pas censée s'exécuter
    }

}
