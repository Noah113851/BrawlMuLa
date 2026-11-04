package brawlmula;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Apercu autonome : pas de connexion a la base de donnees,
 * pas de selection des joueurs ni de modification d'Avatar.java.
 * Dans NetBeans : clic droit sur ce fichier > Run File.
 */
public final class ApercuChateau {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame fenetre = new JFrame("BrawlMuLa - Test des plateformes du Chateau");
            PanneauCombat panneau = new PanneauCombat("/ressources/cartes/chateau.png");
            // Activer pour regler les zones de collision, desactiver pour le jeu.
            panneau.setAfficherHitboxes(false);
            System.out.println("PNG du Chateau charge : "
                    + panneau.spritePlateformesChateauCharge());
            fenetre.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            fenetre.setContentPane(panneau);
            fenetre.pack();
            fenetre.setLocationRelativeTo(null);
            fenetre.setVisible(true);
        });
    }
}
