/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package brawlmula;

import javax.swing.SwingUtilities;
/**
 *
 * @author ndufour
 */
public class BrawlMuLa {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            
            MainMenu menu = new MainMenu();
            
            menu.setVisible(true);            
        });
    }
    
}
