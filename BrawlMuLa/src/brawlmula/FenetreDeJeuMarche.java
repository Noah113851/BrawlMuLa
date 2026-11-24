/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

import javax.swing.SwingUtilities;

/**
 * Fenetre de test MARCHE, distincte du test Chateau.
 * Herite des touches, du Timer, de l'Avatar et de l'affichage de FenetreDeJeu.
 * Aucun code de deplacement n'est duplique.
 */
public final class FenetreDeJeuMarche extends FenetreDeJeu {

    public FenetreDeJeuMarche() {
        super("/ressources/Maps/marche.png");
        setTitle("BrawlMuLa - Test du Marche");
    }

    /** Dans NetBeans : clic droit > Run File. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new FenetreDeJeuMarche().setVisible(true));
    }
}
