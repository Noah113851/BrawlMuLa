package brawlmula;

import javax.swing.SwingUtilities;

/**
 * Test hors reseau : lancer via Run File dans NetBeans.
 * On teste intro de carte -> VS 2/3/4 joueurs -> combat.
 * Il s'agit seulement d'un apercu ; les vrais choix viennent de Selection.
 */
public final class TestSequenceComplete {

    private static final String CARTE = "/ressources/Maps/chateau.png";
    // Modifier a 2, 3 ou 4 pour tester le VS adaptatif.
    private static final int NOMBRE_JOUEURS = 3;

    private TestSequenceComplete() { }

    public static void main(String[] args) {
        if (NOMBRE_JOUEURS < 2 || NOMBRE_JOUEURS > 4) {
            throw new IllegalArgumentException("Choisir 2, 3 ou 4 joueurs.");
        }
        boolean[] presents = new boolean[4];
        for (int i = 0; i < NOMBRE_JOUEURS; i++) {
            presents[i] = true;
        }
        SwingUtilities.invokeLater(() -> SequenceAvantCombat.lancer(
                "Carrot", "Nikku", "Tomato", "Potato",
                CARTE, null, 1, presents));
    }
}