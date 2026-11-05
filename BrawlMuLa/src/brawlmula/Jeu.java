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
    private static final double LARGEUR_AVATAR = 100; // comme Avatar.rendu()

    /** Compatibilite : FenetreDeJeu() continue d'ouvrir le Chateau. */
    public Jeu() {
        this("/ressources/Maps/chateau.png");
    }

    /** Une meme physique, mais un decor et des plateformes selon la carte. */
    public Jeu(String cheminCarte) {
        panneauCombat = new PanneauCombat(cheminCarte);
        panneauCombat.setSize(ConfigurationCarte.LARGEUR, ConfigurationCarte.HAUTEUR);
        panneauCombat.setAfficherHitboxes(false);

        avatar = new Avatar();
        avatar.connecterPlateformes(panneauCombat.getGestionnairePlateformes());
        placerAvatarSurPlateformePrincipale();
    }

    /** Apparition initiale sur le support principal. */
    private void placerAvatarSurPlateformePrincipale() {
        boolean carteFlottante = estCarteFlottante();
        if (carteFlottante) {
            Plateforme principale = chercherPlateformePrincipaleFlottante();
            avatar.x = principale.getX()
                    + (principale.getLargeur() - LARGEUR_AVATAR) / 2.0;
            avatar.y = principale.getY() - HAUTEUR_AVATAR;
            return;
        }
        // Conserver l'apparition du Chateau et des autres cartes.
        double solLePlusHaut = Double.POSITIVE_INFINITY;
        double avatarX = avatar.x;
        for (Plateforme p : panneauCombat.getGestionnairePlateformes().getPlateformes()) {
            if (p.getType() == Plateforme.Type.SOLIDE
                    && avatarX + LARGEUR_AVATAR > p.getX()
                    && avatarX < p.getX() + p.getLargeur()) {
                solLePlusHaut = Math.min(solLePlusHaut, p.getY());
            }
        }
        if (Double.isFinite(solLePlusHaut)) {
            avatar.y = solLePlusHaut - HAUTEUR_AVATAR;
        }
    }

    /** Le Marche, le Potager et la Colonie ont une plateforme principale flottante. */
    private boolean estCarteFlottante() {
        String nom = panneauCombat.getConfiguration().getNom();
        return "MARCHE".equals(nom) || "POTAGER".equals(nom)
                || "COLONIE".equals(nom);
    }

    /** Plus grande plateforme SOLIDE du Marche ou du Potager. */
    private Plateforme chercherPlateformePrincipaleFlottante() {
        Plateforme principale = null;
        for (Plateforme p : panneauCombat.getGestionnairePlateformes().getPlateformes()) {
            if (p.getType() == Plateforme.Type.SOLIDE
                    && (principale == null || p.getLargeur() > principale.getLargeur())) {
                principale = p;
            }
        }
        if (principale == null) {
            throw new IllegalStateException("Aucune plateforme principale solide sur cette carte.");
        }
        return principale;
    }

    public void miseAJour() {
        // Un seul Timer pilote les plateformes et le personnage.
        // Le Timer interne du panneau ne demarre pas : il n'est pas ajoute a Swing.
        panneauCombat.getGestionnairePlateformes().miseAJour(0.040);
        avatar.miseAJour();

        // Au MARCHE et au POTAGER, aucun sol invisible sous le plateau flottant.
        // Quand le personnage est entierement tombe hors ecran,
        // le replacer au centre du plateau principal.
        // Le Chateau conserve son comportement de reapparition actuel.
        if (estCarteFlottante() && avatar.y > ConfigurationCarte.HAUTEUR) {
            reapparaitreSurPlateformePrincipale();
        }
    }

    private void reapparaitreSurPlateformePrincipale() {
        Plateforme principale = chercherPlateformePrincipaleFlottante();
        // Remet a zero chute/support en conservant le meme Avatar.
        avatar.connecterPlateformes(panneauCombat.getGestionnairePlateformes());
        avatar.x = principale.getX()
                + (principale.getLargeur() - LARGEUR_AVATAR) / 2.0;
        avatar.y = principale.getY() - HAUTEUR_AVATAR;
    }

    public void rendu(Graphics2D contexte) {
        panneauCombat.paint(contexte);
        avatar.rendu(contexte);
    }

    public PanneauCombat getPanneauCombat() {
        return panneauCombat;
    }
}