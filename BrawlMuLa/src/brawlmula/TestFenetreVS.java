package brawlmula;

import javax.swing.SwingUtilities;

/**
 * Test independant : adapter NOMBRE_JOUEURS et CHEMIN_CARTE,
 * puis Run File. Ne modifie pas Selection.java ni le reseau.
 */
public final class TestFenetreVS {
    private static final int NOMBRE_JOUEURS = 3; // 2, 3 ou 4
    private static final String CHEMIN_CARTE = "/ressources/Maps/chateau.png";

    public static void main(String[] args) {
        if (NOMBRE_JOUEURS < 2 || NOMBRE_JOUEURS > 4) {
            throw new IllegalArgumentException("Choisir 2, 3 ou 4 joueurs.");
        }
        String[] tous = {"Carrot", "Nikku", "Tomato", "Potato"};
        String[] noms = new String[NOMBRE_JOUEURS];
        int[] numeros = new int[NOMBRE_JOUEURS];
        for (int i = 0; i < NOMBRE_JOUEURS; i++) {
            noms[i] = tous[i];
            numeros[i] = i + 1;
        }
        SwingUtilities.invokeLater(() ->
                new FenetreVS(CHEMIN_CARTE, noms, numeros,
                        () -> new FenetreDeJeu(CHEMIN_CARTE).setVisible(true))
                        .setVisible(true));
    }
}
