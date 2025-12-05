package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DAOIntegrationTestIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void cleanDB() throws Exception {

        dataBasePrepareService.clearDataBaseEntries();
        dataBasePrepareService.initParkingSpot();
    }

    @Test
    public void testGetNextAvailableSlot_ReturnsAvailable() {

        int slot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        assertTrue(slot > 0, "Doit retourner une place de parking valide");

    }

    @Test
    public void testGetNextAvailableSlot_NoAvailable_ReturnsMinusOne() throws Exception {

        try (Connection con = dataBaseTestConfig.getConnection()) {

            PreparedStatement ps = con.prepareStatement(
                "UPDATE parking SET AVAILABLE = FALSE WHERE TYPE = 'CAR'"
            );

            ps.executeUpdate();
            ps.close();

        }

        int slot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        assertEquals(-1, slot, "devrait retourner -1 si la place n'est pas disponible");

    }

    @Test
    public void testUpdateParking_Success() {

        ParkingSpot spot = new ParkingSpot(1, ParkingType.CAR, false);
       boolean updated = parkingSpotDAO.updateParking(spot);
       assertTrue(updated, "la mise à jour doit fonctionner");
       ParkingSpot check = parkingSpotDAO.getParkingSpot(1);
       assertFalse(check.isAvailable(), "la place doit maintenant être indisponible");

    }

    @Test
    public void testGetParkingSpot_Exists() {

        ParkingSpot spot = parkingSpotDAO.getParkingSpot(1);
        assertNotNull(spot);
        assertEquals(1, spot.getId());

    }

    @Test
    public void testGetParkingSpot_NotExists() {

        ParkingSpot spot = parkingSpotDAO.getParkingSpot(999);
        assertNull(spot);       

    }

    @Test
    public void testSaveTicket_Success() {

        ParkingSpot spot = new ParkingSpot(1, ParkingType.CAR, true);
        Ticket ticket = new Ticket();
        ticket.setParkingSpot(spot);
        ticket.setVehicleRegNumber("TEST234");
        ticket.setPrice(0);
        ticket.setInTime(java.util.Date.from(Instant.now()));
        ticket.setOutTime(null);

        boolean saved = ticketDAO.saveTicket(ticket);
        assertTrue(saved, "le ticket doit être enregistré");

        Ticket retrieved = ticketDAO.getTicket("TEST234");
        assertNotNull(retrieved);
        assertEquals("TEST234", retrieved.getVehicleRegNumber());

    }

    @Test 
    public void testGetTicket_NotExists() {

        Ticket ticket = ticketDAO.getTicket("INCONNU");
        assertNull(ticket);

    }

    @Test
    public void testUpdateTicket_Success() throws Exception {

        ParkingSpot spot = new ParkingSpot(1, ParkingType.CAR, true);
        Ticket ticket = new Ticket();
        ticket.setParkingSpot(spot);
        ticket.setVehicleRegNumber("TEST567");
        ticket.setPrice(0);
        ticket.setInTime(java.util.Date.from(Instant.now()));
        ticketDAO.saveTicket(ticket);

        Ticket savedTicket = ticketDAO.getTicket("TEST567");
        savedTicket.setPrice(5.0);
        savedTicket.setOutTime(java.util.Date.from(Instant.now()));

        boolean updated = ticketDAO.updateTicket(savedTicket);
        assertTrue(updated, "Le ticket doit être mit à jour");

        Ticket retrieved = ticketDAO.getTicket("TEST567");
        assertEquals(5.0, retrieved.getPrice());
        assertNotNull(retrieved.getOutTime());

    }

    @Test
    public void testGetNBTicket() {

        ParkingSpot spot = new ParkingSpot(1, ParkingType.CAR, true);
        Ticket ticket1 = new Ticket();
        ticket1.setParkingSpot(spot);
        ticket1.setVehicleRegNumber("EF890GH");
        ticket1.setInTime(java.util.Date.from(Instant.now()));
        ticketDAO.saveTicket(ticket1);

        Ticket ticket2 = new Ticket();
        ticket2.setParkingSpot(spot);
        ticket2.setVehicleRegNumber("EF890GH");
        ticket2.setInTime(java.util.Date.from(Instant.now()));
        ticketDAO.saveTicket(ticket2);

        int count = ticketDAO.getNBTicket("EF890GH");
        assertEquals(2, count);

    }

    @Test
    public void testGetNextAvailableSlot_ExceptionHandled() throws Exception {

        ParkingSpotDAO dao = new ParkingSpotDAO();

        dao.dataBaseConfig.getConnection().close();
        int slot = dao.getNextAvailableSlot(ParkingType.CAR);
        assertEquals(-1, slot);

    }
    
}
