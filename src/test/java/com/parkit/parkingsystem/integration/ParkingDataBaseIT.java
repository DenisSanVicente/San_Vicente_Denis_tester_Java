package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() throws Exception {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown() {

    }

    @Test
    public void testParkingACar() {
        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // ACT
        parkingService.processIncomingVehicle();

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = dataBaseTestConfig.getConnection();

            // 1. Vérifier qu’un ticket est bien créé
            ps = con.prepareStatement("SELECT * FROM ticket WHERE VEHICLE_REG_NUMBER=?");
            ps.setString(1, "ABCDEF");
            rs = ps.executeQuery();

            assertTrue(rs.next());

            int parkingSpotId = rs.getInt("PARKING_NUMBER");
            Timestamp inTime = rs.getTimestamp("IN_TIME");
            Timestamp outTime = rs.getTimestamp("OUT_TIME");

            assertNotNull(inTime);
            assertNull(outTime); // la voiture vient d’entrer

            // Fermer avant nouvelle requête
            dataBaseTestConfig.closeResultSet(rs);
            dataBaseTestConfig.closePreparedStatement(ps);

            // 2. Vérifier que la place est occupée
            ps = con.prepareStatement("SELECT AVAILABLE FROM parking WHERE PARKING_NUMBER=?");
            ps.setInt(1, parkingSpotId);
            rs = ps.executeQuery();

            assertTrue(rs.next());
            assertFalse(rs.getBoolean("AVAILABLE"));

        } catch (Exception e) {
            fail("Erreur lors du test : " + e.getMessage());
        } finally {
            dataBaseTestConfig.closeResultSet(rs);
            dataBaseTestConfig.closePreparedStatement(ps);
            dataBaseTestConfig.closeConnection(con);
        }
    }


    @Test
    public void testParkingLotExit() {

        // ARRANGE : voiture entre
        testParkingACar();

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = dataBaseTestConfig.getConnection();

            // Simuler 1h de stationnement pour que le prix > 0
            ps = con.prepareStatement(
                    "UPDATE ticket SET IN_TIME = ? WHERE VEHICLE_REG_NUMBER = ?"
            );
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusHours(1)));
            ps.setString(2, "ABCDEF");
            ps.executeUpdate();
            dataBaseTestConfig.closePreparedStatement(ps);

            // ACT : la voiture sort
            parkingService.processExitingVehicle();

            //  Récupérer le ticket
            ps = con.prepareStatement(
                    "SELECT * FROM ticket WHERE VEHICLE_REG_NUMBER=? ORDER BY IN_TIME DESC LIMIT 1"
            );
            ps.setString(1, "ABCDEF");
            rs = ps.executeQuery();

            assertTrue(rs.next(), "Le ticket pour le véhicule ABCDEF n'a pas été trouvé");

            int parkingSpotID = rs.getInt("PARKING_NUMBER");
            Timestamp outTime = rs.getTimestamp("OUT_TIME");
            double fare = rs.getDouble("PRICE");

            assertNotNull(outTime, "OUT_TIME n'a pas été enregistré");
            assertTrue(fare > 0, "Le prix du ticket doit être supérieur à 0");

            dataBaseTestConfig.closeResultSet(rs);
            dataBaseTestConfig.closePreparedStatement(ps);

            // Vérifier que la place est libérée
            ps = con.prepareStatement("SELECT AVAILABLE FROM parking WHERE PARKING_NUMBER=?");
            ps.setInt(1, parkingSpotID);
            rs = ps.executeQuery();

            assertTrue(rs.next(), "La place de parking n'a pas été trouvée");
            assertTrue(rs.getBoolean("AVAILABLE"), "La place doit être disponible");

        } catch (Exception e) {
            fail("Erreur lors du test testParkingLotExit : " + e.getMessage());
        } finally {
            dataBaseTestConfig.closeResultSet(rs);
            dataBaseTestConfig.closePreparedStatement(ps);
            dataBaseTestConfig.closeConnection(con);
        }
    }

    @Test
    public void testParkingLotExitRecurringUser() {

        // TestParkingLotExit x2
        // initialiseer double price = 0;



        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;


        try {
            con = dataBaseTestConfig.getConnection();


            // Récupérer le ticket
            ps = con.prepareStatement(
                    DBConstants.GET_TICKET_PRICE
            );
            ps.setString(1, "ABCDEF");
            // rs.executeQuery(); // Pour récupérer prix ticket associé

            // rs.next pour setPrice (setPrice initialisé L197


        } catch (Exception e) {
            fail("Erreur testParkingLotExitRecurringUser : " + e.getMessage());
        } finally {
            dataBaseTestConfig.closeResultSet(rs);
            dataBaseTestConfig.closePreparedStatement(ps);
            dataBaseTestConfig.closeConnection(con);
        }

        // valeur double expected = CAR_RATE_PER_HOUR * FARE_DISCOUNT
        // assertThat(valeur de price qui a été set, .isEqualsto(expected)
    }
}
