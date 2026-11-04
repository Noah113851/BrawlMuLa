package brawlmula;

import java.awt.Graphics2D;

/**
 * Meme role que le Jeu.java d'origine : gere l'Avatar et son rendu. Pour
 * FenetreDeJeu, on lui ajoute simplement le Chateau et les plateformes.
 */
public class Jeu {

    public Avatar avatar;
    private final PanneauCombat panneauCombat;
    private static final double HAUTEUR_AVATAR = 100; // comme Avatar.rendu()

    /**
     * Compatibilite : FenetreDeJeu() continue d'ouvrir le Chateau.
     */
    public Jeu() {
        this("/ressources/Maps/chateau.png");
    }

    /**
     * Une meme physique, mais un decor et des plateformes selon la carte.
     */
    public Jeu(String cheminCarte) {
        panneauCombat = new PanneauCombat(cheminCarte);
        panneauCombat.setSize(ConfigurationCarte.LARGEUR, ConfigurationCarte.HAUTEUR);
        panneauCombat.setAfficherHitboxes(false);

        avatar = new Avatar();
        avatar.connecterPlateformes(panneauCombat.getGestionnairePlateformes());
        placerAvatarSurPlateformePrincipale();
    }

    /**
     * Place le personnage juste au-dessus du sol fixe de la carte choisie. Son
     * code de deplacement et de collision reste celui qui fonctionne deja.
     */
    private void placerAvatarSurPlateformePrincipale() {
        double solLePlusHaut = Double.POSITIVE_INFINITY;
        double avatarX = avatar.x; // 600 apres connecterPlateformes()
        boolean marche = "MARCH".equals(
                panneauCombat.getConfiguration().getNom());
        for (Plateforme p : panneauCombat.getGestionnairePlateformes().getPlateformes()) {
            boolean supportDepart = p.getType() == Plateforme.Type.SOLIDE
                    || (marche && p.getType() == Plateforme.Type.TRAVERSABLE
                            && p.getLargeur() >= 400);
            if (supportDepart
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
