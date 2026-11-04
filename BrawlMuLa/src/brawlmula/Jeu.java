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
        // Un seul objet PanneauCombat fournit decor, sprites, plateformes ET physique.
        panneauCombat = new PanneauCombat("/ressources/cartes/chateau.png");
        panneauCombat.setSize(ConfigurationCarte.LARGEUR, ConfigurationCarte.HAUTEUR);
        panneauCombat.setAfficherHitboxes(false);

        // Conserve le constructeur Avatar() et les controles existants.
        avatar = new Avatar();
        avatar.connecterPlateformes(panneauCombat.getGestionnairePlateformes());
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

