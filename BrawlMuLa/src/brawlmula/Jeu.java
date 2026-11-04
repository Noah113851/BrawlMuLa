package brawlmula;

import java.awt.Graphics2D;

/**
 * Jeu commun aux fenetres de test Chateau et Marche.
 * Garde le meme Avatar, les memes commandes et la meme boucle a 40 ms.
 */
public class Jeu {

    public Avatar avatar;
    private final PanneauCombat panneauCombat;
    private static final double HAUTEUR_AVATAR = 100; // comme Avatar.rendu()

    /** Compatibilite : FenetreDeJeu() continue d'ouvrir le Chateau. */
    public Jeu() {
<<<<<<< Updated upstream
        // Un seul objet PanneauCombat fournit decor, sprites, plateformes ET physique.
        panneauCombat = new PanneauCombat("/ressources/cartes/chateau.png");
=======
        this("/ressources/Maps/chateau.png");
    }

    /** Une meme physique, mais un decor et des plateformes selon la carte. */
    public Jeu(String cheminCarte) {
        panneauCombat = new PanneauCombat(cheminCarte);
>>>>>>> Stashed changes
        panneauCombat.setSize(ConfigurationCarte.LARGEUR, ConfigurationCarte.HAUTEUR);
        panneauCombat.setAfficherHitboxes(false);

        avatar = new Avatar();
        avatar.connecterPlateformes(panneauCombat.getGestionnairePlateformes());
        placerAvatarSurPlateformePrincipale();
    }

    /**
     * Place le personnage juste au-dessus du sol fixe de la carte choisie.
     * Son code de deplacement et de collision reste celui qui fonctionne deja.
     */
    private void placerAvatarSurPlateformePrincipale() {
        double solLePlusHaut = Double.POSITIVE_INFINITY;
        double avatarX = avatar.x; // 600 apres connecterPlateformes()
        for (Plateforme p : panneauCombat.getGestionnairePlateformes().getPlateformes()) {
            if (p.getType() == Plateforme.Type.SOLIDE
                    && avatarX + HAUTEUR_AVATAR > p.getX()
                    && avatarX < p.getX() + p.getLargeur()) {
                solLePlusHaut = Math.min(solLePlusHaut, p.getY());
            }
        }
        if (Double.isFinite(solLePlusHaut)) {
            avatar.y = solLePlusHaut - HAUTEUR_AVATAR;
        }
    }

    public void miseAJour() {
        // Un seul Timer pilote les plateformes et le personnage.
        // Le Timer interne du panneau ne demarre pas : il n'est pas ajoute a Swing.
        panneauCombat.getGestionnairePlateformes().miseAJour(0.040);
        avatar.miseAJour();
    }

    public void rendu(Graphics2D contexte) {
        panneauCombat.paint(contexte);
        avatar.rendu(contexte);
    }

    public PanneauCombat getPanneauCombat() {
        return panneauCombat;
    }
}