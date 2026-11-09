package brawlmula;

import java.util.Locale;
import javax.swing.SwingUtilities;

/**
 * Intro de la carte -> VS des personnages selectionnes -> combat.
 * Pas de changement dans FenetreIntroCarte ou dans le moteur physique.
 */
public final class SequenceAvantCombat {

    private SequenceAvantCombat() { }

    public static void lancer(String j1, String j2, String j3, String j4,
                              String cheminCarte, Reseau reseau,
                              int monNumeroJoueur) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> lancer(j1, j2, j3, j4,
                    cheminCarte, reseau, monNumeroJoueur));
            return;
        }
        String nom = nomDeLaCarte(cheminCarte);
        Runnable combat = () -> new FenetreDeJeu(j1, j2, j3, j4,
                cheminCarte, reseau, monNumeroJoueur).setVisible(true);
        Runnable vs = () -> new FenetreVS(cheminCarte, j1, j2, j3, j4,
                combat).setVisible(true);
        new FenetreIntroCarte(cheminCarte, nom, vs).setVisible(true);
    }

    private static String nomDeLaCarte(String chemin) {
        String c = chemin == null ? "" : chemin.replace('\\','/')
                .toLowerCase(Locale.ROOT);
        if (c.endsWith("chateau.png")) return "Château";
        if (c.endsWith("marche.png")) return "Marché";
        if (c.endsWith("potager.png")) return "Potager";
        if (c.endsWith("colonie.png")) return "Colonie";
        throw new IllegalArgumentException("Carte inconnue : " + chemin);
    }
}