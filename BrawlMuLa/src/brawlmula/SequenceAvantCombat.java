package brawlmula;

import java.util.Locale;
import javax.swing.SwingUtilities;

/** Introduction cinematique -> ecran VS adapte -> combat. */
public final class SequenceAvantCombat {

    private SequenceAvantCombat() { }

    /** Appel historique : affiche quatre joueurs (mode demonstration). */
    public static void lancer(String j1, String j2, String j3, String j4,
                              String cheminCarte, Reseau reseau,
                              int monNumeroJoueur) {
        lancer(j1, j2, j3, j4, cheminCarte, reseau,
                monNumeroJoueur, new boolean[]{true, true, true, true});
    }

    /**
     * Version multijoueur : presents[0] = J1, ..., presents[3] = J4.
     * Affiche uniquement les joueurs connectes, sans les re-numeroter.
     */
    public static void lancer(String j1, String j2, String j3, String j4,
                              String cheminCarte, Reseau reseau,
                              int monNumeroJoueur, boolean[] presents) {
        if (presents == null || presents.length != 4) {
            throw new IllegalArgumentException(
                    "Il faut quatre indicateurs de presence (J1 a J4).");
        }
        final boolean[] slots = presents.clone();
        // Dans un salon reseau, l'hote J1 est toujours present.
        // Son message PRESENCE_P1 n'est pas forcement rejoue par Reseau
        // aux clients (il peut servir de borne initiale au polling SQL).
        if (reseau != null) {
            slots[0] = true;
        }
        // Le joueur local est forcement present.
        if (monNumeroJoueur >= 1 && monNumeroJoueur <= 4) {
            slots[monNumeroJoueur - 1] = true;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> lancer(j1, j2, j3, j4,
                    cheminCarte, reseau, monNumeroJoueur, slots));
            return;
        }
        String[] choix = {j1, j2, j3, j4};
        int nombre = 0;
        for (boolean present : slots) if (present) nombre++;
        System.out.println("[BrawlMuLa][Sequence] Joueurs detectes : " + nombre);
        if (nombre < 2) {
            // Alerte explicite au lieu d'un saut silencieux du VS.
            System.err.println("[BrawlMuLa][VS] Un seul joueur detecte ! "
                    + "Le VS a 2 joueurs ne peut pas etre affiche.");
            if (reseau != null) {
                javax.swing.JOptionPane.showMessageDialog(null,
                    "Un seul joueur detecte dans le salon.\n"
                    + "Verifie que les deux PC utilisent le meme projet "
                    + "et que J2 a rejoint avant de lancer Combat.",
                    "BrawlMuLa - Diagnostic VS",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            }
            // En solo, on ne cree aucun adversaire fictif : intro puis combat.
            Runnable combatSolo = () -> new FenetreDeJeu(j1, j2, j3, j4,
                    cheminCarte, reseau, monNumeroJoueur).setVisible(true);
            new FenetreIntroCarte(cheminCarte, nomDeLaCarte(cheminCarte),
                    combatSolo).setVisible(true);
            return;
        }
        String[] noms = new String[nombre];
        int[] numeros = new int[nombre];
        int index = 0;
        for (int i = 0; i < 4; i++) {
            if (slots[i]) {
                noms[index] = choix[i];
                numeros[index] = i + 1;
                index++;
            }
        }

        Runnable combat = () -> new FenetreDeJeu(j1, j2, j3, j4,
                cheminCarte, reseau, monNumeroJoueur).setVisible(true);
        Runnable vs = () -> {
            System.out.println("[BrawlMuLa][Sequence] Ouverture de la fenetre VS.");
            try {
                new FenetreVS(cheminCarte, noms, numeros, combat).setVisible(true);
            } catch (RuntimeException erreur) {
                erreur.printStackTrace();
                javax.swing.JOptionPane.showMessageDialog(null,
                        "Impossible d'afficher le VS : " + erreur.getMessage(),
                        "BrawlMuLa - Erreur VS",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                // Ne pas ouvrir le combat a la place en masquant l'erreur.
            }
        };
        new FenetreIntroCarte(cheminCarte, nomDeLaCarte(cheminCarte), vs)
                .setVisible(true);
    }

    private static String nomDeLaCarte(String chemin) {
        String c = chemin == null ? "" : chemin.replace('\\', '/')
                .toLowerCase(Locale.ROOT);
        if (c.endsWith("chateau.png")) return "Château";
        if (c.endsWith("marche.png")) return "Marché";
        if (c.endsWith("potager.png")) return "Potager";
        if (c.endsWith("colonie.png")) return "Colonie";
        throw new IllegalArgumentException("Carte inconnue : " + chemin);
    }
}