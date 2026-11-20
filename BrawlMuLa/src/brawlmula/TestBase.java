/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package brawlmula;

import java.sql.*;

public class TestBase {

    public static void main(String[] args) {
        try {
            Connection c = DriverManager.getConnection("jdbc:mariadb://nemrod.ens2m.fr:3306/2026-2027_s1_vs2_tp2_brawlmula", "etudiant", "YTDTvj9TR3CDYCmP");
            PreparedStatement r = c.prepareStatement("SELECT nom, avatars_image, pv, degats FROM Avatars");
            ResultSet rs = r.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getString("nom") + " / " + rs.getString("avatars_image")
                        + " / " + rs.getInt("pv") + " / " + rs.getInt("degats"));
            }
            r.close();
            c.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
