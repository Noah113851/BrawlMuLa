package brawlmula;

import java.awt.Graphics2D;
import java.util.List;

/**
 * Moteur du jeu : carte, personnage, collisions et synchronisation reseau.
 */
public class Jeu {

    public Avatar avatar;
    private final PanneauCombat panneauCombat;
    private static final double HAUTEUR_AVATAR = 100;
    private static final double LARGEUR_AVATAR = 100;

    // Conserve la compatibilite avec le multijoueur existant.
    private Reseau reseau;
    private int monNumeroJoueur = 1;

    public Jeu() {
        this("/ressources/Maps/chateau.png");
    }

    public Jeu(String cheminCarte) {
        panneauCombat = new PanneauCombat(cheminCarte);
        panneauCombat.setSize(ConfigurationCarte.LARGEUR, ConfigurationCarte.HAUTEUR);
        panneauCombat.setAfficherHitboxes(false);

        avatar = new Avatar();
        avatar.connecterPlateformes(panneauCombat.getGestionnairePlateformes());
        placerAvatarSurPlateformePrincipale();
    }

    public Jeu(String j1, String j2, String j3, String j4,
               String cheminCarte, Reseau reseau, int monNumeroJoueur) {
        this(cheminCarte);
        this.reseau = reseau;
        this.monNumeroJoueur = monNumeroJoueur;
    }

    private void placerAvatarSurPlateformePrincipale() {
        if (estCarteFlottante()) {
            Plateforme principale = chercherPlateformePrincipaleFlottante();
            avatar.x = principale.getX()
                    + (principale.getLargeur() - LARGEUR_AVATAR) / 2.0;
            avatar.y = principale.getY() - HAUTEUR_AVATAR;
            return;
        }

        // Chateau : sol principal deja represente dans le decor.
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

    private boolean estCarteFlottante() {
        String nom = panneauCombat.getConfiguration().getNom();
        return "MARCHE".equals(nom)
                || "POTAGER".equals(nom)
                || "COLONIE".equals(nom);
    }

    private Plateforme chercherPlateformePrincipaleFlottante() {
        Plateforme principale = null;
        for (Plateforme p : panneauCombat.getGestionnairePlateformes().getPlateformes()) {
            if (p.getType() == Plateforme.Type.SOLIDE
                    && (principale == null || p.getLargeur() > principale.getLargeur())) {
                principale = p;
            }
        }
        if (principale == null) {
            throw new IllegalStateException(
                    "Aucune plateforme principale solide sur cette carte.");
        }
        return principale;
    }

    public void miseAJour() {
        // Un seul Timer pilote le monde et le personnage (40 ms par tick).
        panneauCombat.getGestionnairePlateformes().miseAJour(0.040);
        avatar.miseAJour();

        // Marche, Potager et Colonie : reapparition sur le grand plateau.
        if (estCarteFlottante() && avatar.y > ConfigurationCarte.HAUTEUR) {
            reapparaitreSurPlateformePrincipale();
        }

        // Synchronisation reseau deja presente dans le projet.
        if (reseau != null) {
            reseau.envoyerMaPosition(
                    monNumeroJoueur, (int) avatar.x, (int) avatar.y);

            List<Reseau.JoueurData> etatsBDD = reseau.lireTousLesJoueurs();
            for (Reseau.JoueurData data : etatsBDD) {
                if (!data.pseudo.equals("ChoixJ" + monNumeroJoueur)) {
                    // Les positions distantes pourront servir a l'affichage
                    // des autres joueurs lors de l'integration multijoueur.
                }
            }
        }
    }

    private void reapparaitreSurPlateformePrincipale() {
        Plateforme principale = chercherPlateformePrincipaleFlottante();
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

    // Compatibilite avec les appels des touches depuis les autres fenetres.
    public void gererToucheAppuyee(int codeTouche) {
        if (avatar != null) {
            avatar.gererToucheAppuyee(codeTouche);
        }
    }

    public void gererToucheRelachee(int codeTouche) {
        if (avatar != null) {
            avatar.gererToucheRelachee(codeTouche);
        }
    }
}