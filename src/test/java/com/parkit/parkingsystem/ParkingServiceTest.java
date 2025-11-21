package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.lang.reflect.Field;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private void setUpPerTest() throws Exception {
        try {

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void testProcessIncomingVehicle() throws Exception {

        when(inputReaderUtil.readSelection()).thenReturn(1);
        
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);

        parkingService.processIncomingVehicle();

        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));

    }

    @Test
    public void testProcessIncomingVehicleParkingFull() throws Exception { 

        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(0);

        parkingService.processIncomingVehicle();

        verify(parkingSpotDAO, times(0)).updateParking(any());
        verify(ticketDAO, times(0)).saveTicket(any());

    }

        @Test
    public void testProcessIncomingVehicleThrowsException() throws Exception { 

        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        doThrow(new RuntimeException("DB error")).when(parkingSpotDAO).updateParking(any());

        parkingService.processIncomingVehicle();

        verify(ticketDAO, times(0)).saveTicket(any());

    }

    @Test
    public void testProcessIncomingVehicleTestUpdateParkingThrowsException() throws Exception {

        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        
        doThrow(new RuntimeException("DB error")).when(parkingSpotDAO).updateParking(any());

        parkingService.processIncomingVehicle();

        verify(ticketDAO, times(0)).saveTicket(any());

    }

    @Test
    public void testProcessIncomingVehicleNoSlot() {

        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any())).thenReturn(0);

        parkingService.processIncomingVehicle();

        verify(ticketDAO, never()).saveTicket(any());
        verify(parkingSpotDAO, never()).updateParking(any());

    }

    @Test
    public void testProcessIncomingVehicleThrowsExceptionOnReadVehicleRegNumber() throws Exception {

        ParkingService realService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        ParkingService spyService = spy(realService);
        ParkingSpot mockSpot = new ParkingSpot(1, ParkingType.CAR, true);
        doReturn(mockSpot).when(spyService).getNextParkingNumberIfAvailable();
        doThrow(new Exception("Input error")).when(spyService).getVehicleRegNumber();

        spyService.processIncomingVehicle();

        verify(parkingSpotDAO, never()).updateParking(any());
        verify(ticketDAO, never()).saveTicket(any());


    }

    @Test
    public void testProcessIncomingVehicleParkingSpotNull() throws Exception {

        ParkingService realService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        ParkingService spyService = spy(realService);

        doReturn(null).when(spyService).getNextParkingNumberIfAvailable();

        spyService.processIncomingVehicle();

        verify(parkingSpotDAO, never()).updateParking(any());
        verify(ticketDAO, never()).saveTicket(any());

    }

    @Test
    public void testGetNextParkingNumberIfAvailable() throws Exception {

        when(inputReaderUtil.readSelection()).thenReturn(1);

        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);

        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        assertNotNull(parkingSpot);
        assertEquals(1, parkingSpot.getId());
        assertTrue(parkingSpot.isAvailable());
        assertEquals(ParkingType.CAR, parkingSpot.getParkingType());

    }

    @Test
    public void testGetNextParkingNumberIfAvailableThrowsException() throws Exception {

        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenThrow(new RuntimeException("DB Error"));

        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        assertNull(parkingSpot);
        
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() throws Exception {

        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(0);

        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        assertNull(parkingSpot);

    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() throws Exception {

        when(inputReaderUtil.readSelection()).thenReturn(3);

        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        assertNull(parkingSpot);

    }

    @Test
    public void testGetNextParkingNumberIfAvailableDaoThrowsException() throws Exception {

        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenThrow(new RuntimeException("DB Error"));

        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        assertNull(parkingSpot);

    }

    @Test
    public void processExitingVehicleTest() throws Exception {

        Ticket ticket = createTicketWithOneHourInTime();
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        ticket.setOutTime(null);

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket);
        when(ticketDAO.getNBTicket("ABCDEF")).thenReturn(2);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        parkingService.processExitingVehicle();

        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, times(1)).updateParking(parkingSpot);
    }

    @Test
    public void processExitingVehicleTestUnableUpdate() throws Exception {

        Ticket ticket = createTicketWithOneHourInTime();

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.getNBTicket(anyString())).thenReturn(1);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        parkingService.processExitingVehicle();

        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, times(0)).updateParking(any(ParkingSpot.class));

    }

    @Test
    public void processExitingVehicleTestTicketNotFound() throws Exception {

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket("ABCDEF")).thenReturn(null);

        parkingService.processExitingVehicle();

        verify(ticketDAO, times(0)).updateTicket(any());
        verify(parkingSpotDAO, times(0)).updateParking(any());

    }

    @Test
    public void processExitingVehicleTestFareException() throws Exception {

        Ticket ticket = createTicketWithOneHourInTime();

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket);
        when(ticketDAO.getNBTicket("ABCDEF")).thenReturn(1);

        FareCalculatorService fareCalculatorServiceMock = mock(FareCalculatorService.class);
        doThrow(new IllegalArgumentException("Invalid out time")).when(fareCalculatorServiceMock).calculateFare(any(Ticket.class), anyBoolean());

        Field field = ParkingService.class.getDeclaredField("fareCalculatorService");
        field.setAccessible(true);
        field.set(parkingService, fareCalculatorServiceMock);

        parkingService.processExitingVehicle();

        verify(ticketDAO, times(0)).updateTicket(any());
        verify(parkingSpotDAO, times(0)).updateParking(any());

    }

    @Test
    public void processExitingVehicleTestNonRecurringUser() throws Exception {

        Ticket ticket = createTicketWithOneHourInTime();

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket);
        when(ticketDAO.getNBTicket("ABCDEF")).thenReturn(1);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        parkingService.processExitingVehicle();

        verify(ticketDAO, times(1)).updateTicket(any());
        verify(parkingSpotDAO, times(1)).updateParking(any());

        assertEquals(ticket.getPrice(), ticket.getPrice() / 0.95 * 0.95);

    }
    
    private Ticket createTicketWithOneHourInTime() {
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        Ticket ticket = new Ticket();
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setInTime(new Date(System.currentTimeMillis() - (60* 60 * 1000)));
        ticket.setPrice(0);
        return ticket;
    }

}
