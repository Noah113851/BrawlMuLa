/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

import javax.swing.SwingUtilities;

/** Fenetre de test independante de la carte COLONIE. */
public final class FenetreDeJeuColonie extends FenetreDeJeu {

    public FenetreDeJeuColonie() {
        super("/ressources/Maps/colonie.png");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new FenetreDeJeuColonie().setVisible(true));
    }
}
