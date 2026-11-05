/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

import javax.swing.SwingUtilities;

/** Test independant du Potager : meme mecanique que Chateau/Marche. */
public final class FenetreDeJeuPotager extends FenetreDeJeu {

    public FenetreDeJeuPotager() {
        super("/ressources/Maps/potager.png");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new FenetreDeJeuPotager().setVisible(true));
    }
}
