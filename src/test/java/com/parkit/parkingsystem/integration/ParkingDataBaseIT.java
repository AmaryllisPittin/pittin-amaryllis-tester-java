package com.parkit.parkingsystem.integration;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        lenient().when(inputReaderUtil.readSelection()).thenReturn(1);
        lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
        dataBasePrepareService.initParkingSpot();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar(){

        /*dataBasePrepareService.clearDataBaseEntries();
        dataBasePrepareService.initParkingSpot();*/

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();

        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket, "le ticket devrait être enregistré en base");
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        assertNotNull(ticket.getInTime(), "L'heure d'entrée ne devrait pas être à null");

        ParkingSpot updateSpot = parkingSpotDAO.getParkingSpot(ticket.getParkingSpot().getId());
        assertFalse(updateSpot.isAvailable(), "La place devrait être indiquée comme indisponible");

    }

    @Test
    public void testParkingLotExit() throws Exception {

        testParkingACar();

        InputReaderUtil inputReaderUtil = mock(InputReaderUtil.class);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Date outTime = new Date();
        outTime.setTime(System.currentTimeMillis() + 45 * 60 * 1000);
        parkingService.processExitingVehicle(outTime);

        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket, "le ticket doit exister");
        assertNotNull(ticket.getOutTime(), "L'heure de sortie devrait être remplie");
        assertTrue(ticket.getPrice() >= 0, "Le prix devrait être renseigné");

        ParkingSpot parkingSpot = parkingSpotDAO.getParkingSpot(ticket.getParkingSpot().getId());
        assertTrue(parkingSpot.isAvailable(), "La place doit être disponible après sortie du véhicule");

    
    }

    @Test
    public void testParkingLotExitRecurringUser() throws Exception {

        dataBasePrepareService.clearDataBaseEntries();
        dataBasePrepareService.initParkingSpot();

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();

        try (Connection con = dataBaseTestConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "UPDATE ticket SET in_time=? WHERE vehicle_reg_number=?")) {

                    ps.setTimestamp(1, Timestamp.from(Instant.now().minusSeconds(3600)));
                    ps.setString(2, "ABCDEF");
                    ps.executeUpdate();

            }

        parkingService.processExitingVehicle(new Date());

        parkingService.processIncomingVehicle();

        parkingService.processExitingVehicle(new Date());

        Ticket secondTicket = ticketDAO.getTicket("ABCDEF");

        assertNotNull(secondTicket.getOutTime());
        assertTrue(secondTicket.getPrice() > 0);

        double expectedPrice = Fare.CAR_RATE_PER_HOUR * 0.95;
        assertEquals(expectedPrice, secondTicket.getPrice(), 0.01, "Le tarif récurrent doit être appliqué");
        
        ParkingSpot spot = parkingSpotDAO.getParkingSpot(secondTicket.getParkingSpot().getId());
        assertTrue(spot.isAvailable(), "La place doit être livre après la sortie du véhicule.");

    }

}
