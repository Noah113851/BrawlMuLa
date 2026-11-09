package brawlmula;

import javax.swing.SwingUtilities;

/**
 * Lance uniquement le test de l'introduction du Marche.
 * Aucune modification de Selection.java ou du multijoueur.
 */
public final class FenetreIntroMarche {

    private FenetreIntroMarche() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new FenetreIntroCarte(
                        "/ressources/Maps/marche.png", "MARCHÉ")
                        .setVisible(true));
    }
}