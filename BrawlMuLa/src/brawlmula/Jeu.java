package brawlmula;

import java.awt.Graphics2D;

/**
 * Meme role que le Jeu.java d'origine : gere l'Avatar et son rendu.
 * Pour FenetreDeJeu, on lui ajoute simplement le Chateau et les plateformes.
 */
public class Jeu {

    public Avatar avatar;
    private final PanneauCombat panneauCombat;

    public Jeu() {
<<<<<<< Updated upstream
        // Un seul objet PanneauCombat fournit decor, sprites, plateformes ET physique.
        panneauCombat = new PanneauCombat("/ressources/Maps/chateau.png");
=======
        this("/ressources/Maps/chateau.png");
    }

    /** Une meme physique, mais un decor et des plateformes selon la carte. */
    public Jeu(String cheminCarte) {
        panneauCombat = new PanneauCombat(cheminCarte);
>>>>>>> Stashed changes
        panneauCombat.setSize(ConfigurationCarte.LARGEUR, ConfigurationCarte.HAUTEUR);
        panneauCombat.setAfficherHitboxes(false);

        // Conserve le constructeur Avatar() et les controles existants.
        avatar = new Avatar();
        avatar.connecterPlateformes(panneauCombat.getGestionnairePlateformes());
<<<<<<< Updated upstream
=======
        placerAvatarSurPlateformePrincipale();
    }

    /**
     * Place le personnage sur un support de depart de la carte choisie.
     * Au Marche, il apparait sur la grande plateforme centrale abaissee.
     * Le Chateau conserve son sol solide habituel.
     * Son code de deplacement et de collision reste celui qui fonctionne deja.
     */
    private void placerAvatarSurPlateformePrincipale() {
        double solLePlusHaut = Double.POSITIVE_INFINITY;
        double avatarX = avatar.x; // 600 apres connecterPlateformes()
        boolean marche = "MARCHE".equals(
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
>>>>>>> Stashed changes
    }

    public void miseAJour() {
        // IMPORTANT : ici, le panneau n'est PAS ajoute comme composant Swing,
        // son propre Timer de 16 ms ne demarre donc jamais.
        // Une seule mise a jour des plateformes par tick de FenetreDeJeu.
        panneauCombat.getGestionnairePlateformes().miseAJour(0.040);
        avatar.miseAJour();
    }

    public void rendu(Graphics2D contexte) {
        // Le panneau est peint dans le framebuffer original de FenetreDeJeu.
        panneauCombat.paint(contexte);
        avatar.rendu(contexte);
    }
}

<<<<<<< Updated upstream
=======
    public PanneauCombat getPanneauCombat() {
        return panneauCombat;
    }
}
>>>>>>> Stashed changes
