package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.time.LocalDateTime;

import static com.mysql.cj.conf.PropertyKey.logger;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

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
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // Appel de la méthode processIncomingVehicle
        parkingService.processIncomingVehicle();

        // Création du ticket
        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        // Vérification que le ticket existe réellement
        assertThat(ticket).isNotNull();

        ParkingSpot parkingSpot = ticket.getParkingSpot();

        // Vérification que le parkingSpot a bien été crée
        assertThat(parkingSpot).isNotNull();

        // Vérification que la place de parking n'est plus libre
        assertThat(parkingSpot.isAvailable()).isFalse();
    }


    @Test
    public void testParkingLotExit(){

        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // Simuler la sortie après 1h de parking
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        ticket.setInTime(new Date(System.currentTimeMillis() - 3600000));

        // MaJ du ticket dans la BDD pour simuler une entrée
        Connection con = null;
        try {
            con = dataBaseTestConfig.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE ticket SET IN_TIME=? WHERE VEHICLE_REG_NUMBER=?");
            ps.setTimestamp(1, new Timestamp(ticket.getInTime().getTime())); // Paramètre 1  SET_IN_TIME
            ps.setString(2, "ABCDEF"); //Paramètre 2 VEHICLE_REG_NUMBER
            ps.executeUpdate();
            dataBaseTestConfig.closePreparedStatement(ps);

        }catch (Exception ex){
            fail("Error updating IN_TIME in database",ex);
        }finally {
            dataBaseTestConfig.closeConnection(con);
        }

        parkingService.processExitingVehicle();

        ticket = ticketDAO.getTicket("ABCDEF");
        ParkingSpot parkingSpot = ticket.getParkingSpot();

        assertThat(ticket).isNotNull();
        assertThat(ticket.getOutTime()).isNotNull();
        assertThat(ticket.getOutTime()).isAfter(ticket.getInTime());
        assertThat(ticket.getPrice()).isGreaterThan(0);

        // Retrieve the availability status of the parking spot from the database       boolean available = false;
        boolean available = false;
        try {
            con = dataBaseTestConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.GET_PARKING_SPOT_ISAVAILABLE);
            ps.setInt(1, parkingSpot.getId());
            ResultSet rs = (ps.executeQuery());
            if (rs.next()){
                available = rs.getBoolean(1);
            }
            dataBaseTestConfig.closeResultSet(rs);
            dataBaseTestConfig.closePreparedStatement(ps);
        }catch (Exception ex){
            fail("Error fetching ParkingSpot Availibility in database",ex);
        }finally {
            dataBaseTestConfig.closeConnection(con);
        }
        parkingSpot.setAvailable(available);

        assertThat(parkingSpot).isNotNull();
        assertTrue(parkingSpot.isAvailable());
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        testParkingLotExit();
        testParkingLotExit();

        double price = 0.0;

        Connection con = null;

        try {
            con = dataBaseTestConfig.getConnection();

            PreparedStatement ps = con.prepareStatement(
                    DBConstants.GET_TICKET_PRICE
            );

            ps.setString(1, "ABCDEF");
            ResultSet rs = (ps.executeQuery());

            if (rs.next()) {
                price = Math.round(rs.getDouble(1) * 10.0) / 10.0;
            }

            dataBaseTestConfig.closePreparedStatement(ps);
            dataBaseTestConfig.closeResultSet(rs);


        } catch (Exception e) {
            fail("Erreur testParkingLotExitRecurringUser : " + e.getMessage());
        } finally {

            dataBaseTestConfig.closeConnection(con);
        }

        double expected = Math.round(Fare.CAR_RATE_PER_HOUR * Fare.DISCOUNT * 10.0) / 10.0;
        //assertThat(price).isEqualTo(expected);
    }
}
