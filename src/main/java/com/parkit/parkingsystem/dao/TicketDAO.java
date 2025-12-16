package com.parkit.parkingsystem.dao;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * DAO responsable de l acces aux donnees liees aux tickets de parking
 */

public class TicketDAO {

    private static final Logger logger = LogManager.getLogger("TicketDAO");

    public DataBaseConfig dataBaseConfig = new DataBaseConfig();

    /**
     * Enregistre un nouveau ticket en base de donnees
     * 
     * @param ticket ticket a sauvegarder
     * @return true si l insertion a reussi, false sinon
     */

    public boolean saveTicket(Ticket ticket){
        Connection con = null;
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.SAVE_TICKET);
            ps.setInt(1,ticket.getParkingSpot().getId());
            ps.setString(2, ticket.getVehicleRegNumber());
            ps.setDouble(3, ticket.getPrice());
            ps.setTimestamp(4, new Timestamp(ticket.getInTime().getTime()));
            // L heure de sortie est null tant que le vehicule n est pas sorti
            ps.setTimestamp(5, (ticket.getOutTime() == null)?null: (new Timestamp(ticket.getOutTime().getTime())) );
            
            int rowsInserted = ps.executeUpdate();

            dataBaseConfig.closePreparedStatement(ps);
            return rowsInserted > 0;

        }catch (Exception ex){
            logger.error("Error fetching next available slot",ex);
            return false;
        }finally {
            dataBaseConfig.closeConnection(con);
        }
    }

    /**
     * Recupere le ticket actif correspondant a un numero d immatriculation
     * 
     * @param vehicleRegNumber numero d immatriculation du vehicule
     * @return Ticket trouve ou null s il n existe pas
     */

    public Ticket getTicket(String vehicleRegNumber) {
        Connection con = null;
        Ticket ticket = null;
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.GET_TICKET);
            ps.setString(1,vehicleRegNumber);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                ticket = new Ticket();
                // Construction de l objet ParkingSpot a partir du resultat SQL
                ParkingSpot parkingSpot = new ParkingSpot(rs.getInt(1), ParkingType.valueOf(rs.getString(6)),false);
                ticket.setParkingSpot(parkingSpot);
                ticket.setId(rs.getInt(2));
                ticket.setVehicleRegNumber(vehicleRegNumber);
                ticket.setPrice(rs.getDouble(3));
                ticket.setInTime(rs.getTimestamp(4));
                ticket.setOutTime(rs.getTimestamp(5));
            }
            dataBaseConfig.closeResultSet(rs);
            dataBaseConfig.closePreparedStatement(ps);
        }catch (Exception ex){
            logger.error("Error fetching next available slot",ex);
        } finally {
            dataBaseConfig.closeConnection(con);
            
        }
        return ticket;
    }

    /**
     * Met a jour le ticket lors de la sortie du vehicule
     * (prix final et heure de sortie)
     * 
     * @param ticket ticket a mettre a jour
     * @return true si la mise a jour est reussie, false sinon
     */

    public boolean updateTicket(Ticket ticket) {
        Connection con = null;
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.UPDATE_TICKET);
            ps.setDouble(1, ticket.getPrice());
            ps.setTimestamp(2, new Timestamp(ticket.getOutTime().getTime()));
            ps.setInt(3,ticket.getId());
            ps.execute();
            return true;
        }catch (Exception ex){
            logger.error("Error saving ticket info",ex);
        }finally {
            dataBaseConfig.closeConnection(con);
        }
        return false;
    }

    /**
     * Recupere le nombre total de tickets associes a un vehicule.
     * Utilise pour determiner si l utilisateur est recurrent.
     * 
     * @param vehicleRegNumber numero d immatriculation
     * @return nombre de tickets trouves
     */

    public int getNBTicket(String vehicleRegNumber) {
        Connection con = null;
        int nbTicket = 0;
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.GET_NB_TICKET);
            ps.setString(1, vehicleRegNumber);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                nbTicket = rs.getInt(1);
            }

            dataBaseConfig.closeResultSet(rs);
            dataBaseConfig.closePreparedStatement(ps);

        } catch (Exception ex) {
            logger.error("Error counting tickets for vehicle: " + vehicleRegNumber, ex);
        } finally {
            dataBaseConfig.closeConnection(con);
        }
        return nbTicket;
    }


}
