package brawlmula;

import javax.swing.SwingUtilities;

/** Test autonome de l'introduction futuriste de la Colonie. */
public final class FenetreIntroColonie {

    private FenetreIntroColonie() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new FenetreIntroCarte(
                        "/ressources/Maps/colonie.png", "COLONIE")
                        .setVisible(true));
    }
}
