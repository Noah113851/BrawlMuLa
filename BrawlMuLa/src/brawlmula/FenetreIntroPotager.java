package brawlmula;

import javax.swing.SwingUtilities;

/**
 * Lance le test autonome de l'introduction du POTAGER.
 * Le Chateau et le Marche restent disponibles dans leurs propres lanceurs.
 */
public final class FenetreIntroPotager {

    private FenetreIntroPotager() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new FenetreIntroCarte(
                        "/ressources/Maps/potager.png", "POTAGER")
                        .setVisible(true));
    }
}