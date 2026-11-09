package brawlmula;

import java.awt.Graphics2D;
import static brawlmula.Avatar.HAUTEUR_AVATAR;
import static brawlmula.Avatar.LARGEUR_AVATAR;

/**
 * Physique de l'avatar local et rendu des combattants dans la boucle a 40 ms.
 */
public class Jeu {

    private static final double DISTANCE_TELEPORTATION = 180;

    public final Avatar avatar;
    private final PanneauCombat panneauCombat;
    private final Avatar[] avatars = new Avatar[4];
    private final boolean[] avatarsVisibles = new boolean[avatars.length];
    private final Reseau reseau;
    private final int indexLocal;

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
        this("Carrot", null, null, null, cheminCarte, null, 1);
    }

    public Jeu(String j1, String j2, String j3, String j4,
            String cheminCarte, Reseau reseau, int monNumeroJoueur) {
        if (monNumeroJoueur < 1 || monNumeroJoueur > avatars.length) {
            throw new IllegalArgumentException("Numéro de joueur invalide : " + monNumeroJoueur);
        }
        this.reseau = reseau;
        indexLocal = monNumeroJoueur - 1;
        String[] personnages = {j1, j2, j3, j4};
        for (int index = 0; index < avatars.length; index++) {
            if (reseau != null || index == indexLocal) {
                avatars[index] = new Avatar(personnages[index]);
            }
        }
        avatarsVisibles[indexLocal] = true;
        panneauCombat = new PanneauCombat(cheminCarte, avatars, avatarsVisibles);
        panneauCombat.setSize(ConfigurationCarte.LARGEUR, ConfigurationCarte.HAUTEUR);
        panneauCombat.setAfficherHitboxes(false);

        avatar = avatars[indexLocal];
        avatar.connecterPlateformes(panneauCombat.getGestionnairePlateformes());
        placerAvatarSurPlateformePrincipale();
        if (reseau != null) {
            reseau.demarrerSynchronisation(monNumeroJoueur, (int) avatar.x, (int) avatar.y);

            reseau.demarrerEcouteAttaques(numero -> {

                int index = numero - 1;

                if (index >= 0
                        && index < avatars.length
                        && index != indexLocal
                        && avatars[index] != null) {

                    avatars[index].afficherAttaqueDistante();
                }
            });

        }
    }

    /**
     * Apparition initiale sur le support principal.
     */
    private void placerAvatarSurPlateformePrincipale() {
        if (reseau != null || estCarteFlottante()) {
            Plateforme principale = chercherPlateformePrincipale();
            double fraction = reseau == null ? 0.5 : (indexLocal + 0.5) / avatars.length;
            avatar.x = principale.getX() + principale.getLargeur() * fraction - LARGEUR_AVATAR / 2.0;
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

    /**
     * Le Marche, le Potager et la Colonie ont une plateforme principale
     * flottante.
     */
    private boolean estCarteFlottante() {
        String nom = panneauCombat.getConfiguration().getNom();
        return "MARCHE".equals(nom)
                || "POTAGER".equals(nom)
                || "COLONIE".equals(nom);
    }

    /**
     * Plus grande plateforme solide de la carte.
     */
    private Plateforme chercherPlateformePrincipale() {
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
        // Un seul Timer pilote les plateformes et le personnage local (40 ms par tick).
        panneauCombat.getGestionnairePlateformes().miseAJour(0.040);
        avatar.miseAJour();

        // Sur les cartes flottantes, remettre l'avatar sur le plateau principal.
        if (estCarteFlottante() && avatar.y > ConfigurationCarte.HAUTEUR) {
            avatar.connecterPlateformes(panneauCombat.getGestionnairePlateformes());
            placerAvatarSurPlateformePrincipale();
        }

        if (reseau != null) {
            reseau.envoyerMaPosition((int) avatar.x, (int) avatar.y);
            actualiserAvatarsDistants();
        }
    }

    private void actualiserAvatarsDistants() {

        for (Reseau.JoueurData etat
                : reseau.lireTousLesJoueurs()) {

            int index = etat.emplacement() - 1;

            if (index == indexLocal
                    || index < 0
                    || index >= avatars.length) {
                continue;
            }

            Avatar distant = avatars[index];

            if (distant == null) {
                continue;
            }

            if (!avatarsVisibles[index]) {

                distant.x = etat.x();
                distant.y = etat.y();

                distant.actualiserAnimationDistante(0, 0);

                avatarsVisibles[index] = true;
                continue;
            }

            double ancienX = distant.x;
            double ancienY = distant.y;

            double dx = etat.x() - distant.x;
            double dy = etat.y() - distant.y;

            double distance = Math.hypot(dx, dy);

            double fraction
                    = distance < 1
                    || distance >= DISTANCE_TELEPORTATION
                            ? 1.0 : 0.5;

            distant.x += fraction * dx;
            distant.y += fraction * dy;

            distant.actualiserAnimationDistante(
                    distant.x - ancienX,
                    distant.y - ancienY
            );
        }
    }

    public void rendu(Graphics2D contexte) {
        panneauCombat.paint(contexte);
    }

    public void fermer() {
        if (reseau != null) {
            reseau.fermer();
        }
    }

    public PanneauCombat getPanneauCombat() {
        return panneauCombat;
    }

    public void gererToucheAppuyee(int codeTouche) {

        if (codeTouche == java.awt.event.KeyEvent.VK_J) {

            if (avatar.attaquer() && reseau != null) {
                reseau.envoyerAttaque();
            }

            return;
        }

        avatar.gererToucheAppuyee(codeTouche);
    }

    public void gererToucheRelachee(int codeTouche) {
        avatar.gererToucheRelachee(codeTouche);
    }
}
