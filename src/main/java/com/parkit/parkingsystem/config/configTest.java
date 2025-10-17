package com.parkit.parkingsystem.config;

public class configTest {
    public static void main(String[] args) {
        DataBaseConfig dbConfig = new DataBaseConfig();
        try {
            dbConfig.getConnection();
            System.out.println("✅ Connexion réussie à la base MySQL !");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

