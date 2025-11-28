package com.parkit.parkingsystem.integration.service;

import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DataBasePrepareService {

    DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();

    public void clearDataBaseEntries() throws SQLException, ClassNotFoundException {
        Connection connection = null;
        try{
            connection = dataBaseTestConfig.getConnection();

            //set parking entries to available
            connection.prepareStatement("update parking set available = true").execute();

            //clear ticket entries;
            connection.prepareStatement("truncate table ticket").execute();

        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            dataBaseTestConfig.closeConnection(connection);
        }
    }

    public void initParkingSpot() throws SQLException, ClassNotFoundException {

        try (Connection con = dataBaseTestConfig.getConnection();
            Statement stmt = con.createStatement()) {

                stmt.executeUpdate("INSERT INTO parking(PARKING_NUMBER, TYPE, AVAILABLE) VALUES (1, 'CAR', true)");
                stmt.executeUpdate("INSERT INTO parking(PARKING_NUMBER, TYPE, AVAILABLE) VALUES (2, 'BIKE', true)");
                stmt.executeUpdate("INSERT INTO parking(PARKING_NUMBER, TYPE, AVAILABLE) VALUES (3, 'CAR', true)");

            } catch (SQLException e) {

                e.printStackTrace();

            }

    }

}
