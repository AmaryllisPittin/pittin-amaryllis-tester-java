package com.parkit.parkingsystem.dao;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
* DAO responsable de l'acces aux donnees liees aux emplacements de parking.
*/

public class ParkingSpotDAO {
    private static final Logger logger = LogManager.getLogger("ParkingSpotDAO");

    public DataBaseConfig dataBaseConfig = new DataBaseConfig();

    /**
     * Recupere le numero du prochain emplacement disponible
     * pour un type de vehicule donne
     * 
     * @param parkingType type de vehicule (CAR ou BIKE)
     * @return numero de l emplacement disponible ou -1 si aucun n est disponible
     */

    public int getNextAvailableSlot(ParkingType parkingType){
        Connection con = null;
        int result=-1;
        
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.GET_NEXT_PARKING_SPOT);
            ps.setString(1, parkingType.toString());
            ResultSet rs = ps.executeQuery();

            int minParkingNumber = -1;

            // Recupere le plus petit numero de place disponible
            if(rs.next()){
                minParkingNumber = rs.getInt(1);
            } if (!rs.wasNull()) {
                result = minParkingNumber;
            } else {
                result = -1;
            }
            dataBaseConfig.closeResultSet(rs);
            dataBaseConfig.closePreparedStatement(ps);
        }catch (Exception ex){
            logger.error("Error fetching next available slot",ex);
        }finally {
            dataBaseConfig.closeConnection(con);
        }
        return result;
    }

    /**
     * Met a jour la disponibilite d un emplacement de parking
     * 
     * @param parkingSpot emplacement a mettre a jour
     * @return true si la mise a jour est reussie, false dans le cas contraire
     */

    public boolean updateParking(ParkingSpot parkingSpot){
        
        Connection con = null;

        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.UPDATE_PARKING_SPOT);
            ps.setBoolean(1, parkingSpot.isAvailable());
            ps.setInt(2, parkingSpot.getId());
            int updateRowCount = ps.executeUpdate();
            dataBaseConfig.closePreparedStatement(ps);
            //Une seule ligne doit etre modifiee
            return (updateRowCount == 1);
        }catch (Exception ex){
            logger.error("Error updating parking info",ex);
            return false;
        }finally {
            dataBaseConfig.closeConnection(con);
        }
    }

    /**
     * Recupere un emplacement de parking a l aide de son identifiant
     * 
     * @param id identifiant de l emplacement
     * @return ParkingSpot correspondant ou null s il n existe pas
     */

    public ParkingSpot getParkingSpot(int id) {

        Connection con = null;
        ParkingSpot parkingSpot = null;
        
        try {

            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT PARKING_NUMBER, TYPE, AVAILABLE FROM parking WHERE PARKING_NUMBER=?"
            );
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if(rs.next()) {

                int parkingNumber = rs.getInt("PARKING_NUMBER");
                ParkingType type = ParkingType.valueOf(rs.getString("TYPE"));
                boolean available = rs.getBoolean("AVAILABLE");

                // Creation de l objet metier a partir du resultat SQL
                parkingSpot = new ParkingSpot(parkingNumber, type, available);

            }

            dataBaseConfig.closeResultSet(rs);
            dataBaseConfig.closePreparedStatement(ps);   

        } catch (Exception ex) {

            logger.error("error fetching parking spot", ex);

        } finally {

            dataBaseConfig.closeConnection(con);

        }

        return parkingSpot;

    }

}
