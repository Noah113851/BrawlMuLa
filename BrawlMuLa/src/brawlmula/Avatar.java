package brawlmula;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Personnage : touches, affichage, gravite, sauts et collisions.
 * Compatible avec les plateformes intermittentes de la Colonie.
 */
public class Avatar {

    private BufferedImage sprite;
    protected double x, y;
    private boolean gauche, droite, haut, bas;
    private boolean regardeGauche = false;

    private double vy;
    private boolean auSol;
    private static final double SOL = 420;
    private static final double GRAVITE = 15;
    private static final double VITESSE_SAUT = 70;
    private static final double LARGEUR_AVATAR = 100;
    private static final double HAUTEUR_AVATAR = 100;

    private GestionnairePlateforme gestionnaire;
    private Plateforme support;
    private double dernierSupportX;

    private Plateforme plateformeEnCoursDeTraverse;
    private static final double MARGE_TRAVERSEE = 8.0;

    public Avatar() {
        chargerSpritePersonnage("Carrot");
        this.x = 100;
        this.y = SOL;
        this.vy = 0;
        this.auSol = true;
        this.gauche = false;
        this.droite = false;
        this.bas = false;
    }

    public Avatar(String nomPersonnage) {
        this();
        if (nomPersonnage != null && !nomPersonnage.trim().isEmpty()) {
            chargerSpritePersonnage(nomPersonnage);
        }
    }

    /** Charge le personnage choisi sans changer la physique. */
    private void chargerSpritePersonnage(String nomPersonnage) {
        if (nomPersonnage == null) {
            return;
        }
        String fichier;
        switch (nomPersonnage.trim().toLowerCase(Locale.ROOT)) {
            case "carrot":
                fichier = "carrot_fight.png";
                break;
            case "nikku":
                fichier = "nikku_fight.png";
                break;
            case "tomato":
                fichier = "tomato_fight.png";
                break;
            case "potato":
                fichier = "potato_fight.png";
                break;
            default:
                fichier = "carrot_fight.png";
                break;
        }
        String chemin = "/ressources/avatars/" + fichier;
        URL url = getClass().getResource(chemin);
        if (url == null) {
            System.err.println("[BrawlMuLa] Sprite introuvable : " + chemin);
            return;
        }
        try {
            BufferedImage image = ImageIO.read(url);
            if (image != null) {
                sprite = image;
            }
        } catch (IOException e) {
            Logger.getLogger(Avatar.class.getName()).log(Level.SEVERE,
                    "Impossible de charger le sprite : " + chemin, e);
        }
    }

    public void connecterPlateformes(GestionnairePlateforme gestionnaire) {
        if (gestionnaire == null) {
            throw new IllegalArgumentException("GestionnairePlateforme manquant");
        }
        this.gestionnaire = gestionnaire;
        this.support = null;
        this.dernierSupportX = 0;
        this.plateformeEnCoursDeTraverse = null;
        this.x = 600;
        this.y = 400;
        this.vy = 0;
        this.auSol = true;
    }

    public void setGauche(boolean gauche) {
        this.gauche = gauche;
    }

    public void setDroite(boolean droite) {
        this.droite = droite;
    }

    public void setBas(boolean bas) {
        this.bas = bas;
    }

    public void saute() {
        if (this.auSol) {
            this.vy = -VITESSE_SAUT;
            this.auSol = false;
            this.support = null;
            this.plateformeEnCoursDeTraverse = null;
        }
    }

    // Methodes appelees par Jeu.java (correction des erreurs NetBeans).
    public void gererToucheAppuyee(int codeTouche) {
        switch (codeTouche) {
            case KeyEvent.VK_Q:
                setGauche(true);
                break;
            case KeyEvent.VK_D:
                setDroite(true);
                break;
            case KeyEvent.VK_SPACE:
                saute();
                break;
            case KeyEvent.VK_S:
                setBas(true);
                break;
            default:
                break;
        }
    }

    public void gererToucheRelachee(int codeTouche) {
        switch (codeTouche) {
            case KeyEvent.VK_Q:
                setGauche(false);
                break;
            case KeyEvent.VK_D:
                setDroite(false);
                break;
            case KeyEvent.VK_S:
                setBas(false);
                break;
            default:
                break;
        }
    }

    public void miseAJour() {
        // Une plateforme temporaire disparue ne porte plus le joueur.
        if (support != null && !support.estActive()) {
            support = null;
            auSol = false;
            vy = Math.max(0, vy);
            plateformeEnCoursDeTraverse = null;
        }

        double ancienX = x;
        double ancienY = y;

        // Conserver la vitesse horizontale actuelle du projet.
        if (this.gauche) {
            x -= 5;
            regardeGauche = true;
        }
        if (this.droite) {
            x += 5;
            regardeGauche = false;
        }
        if (this.gauche) {
            x -= 5;
        }
        if (this.droite) {
            x += 5;
        }

        double largeurMonde = gestionnaire == null
                ? 960 : ConfigurationCarte.LARGEUR;
        if (x > largeurMonde - LARGEUR_AVATAR) {
            x = largeurMonde - LARGEUR_AVATAR;
        }
        if (x < 0) {
            x = 0;
        }

        // S permet de traverser les petites plateformes par le dessus.
        if (gestionnaire != null && bas && auSol && support != null
                && support.getType() == Plateforme.Type.TRAVERSABLE) {
            plateformeEnCoursDeTraverse = support;
            support = null;
            auSol = false;
            vy = Math.max(vy, 7.0);
        }

        if (this.bas && !this.auSol) {
            if (vy < 0) {
                vy = 0;
            }
            vy += 3;
        }

        if (gestionnaire == null) {
            // Comportement historique hors des cartes a plateformes.
            if (!this.auSol) {
                y += vy;
                vy += GRAVITE;
                if (y >= SOL) {
                    y = SOL;
                    vy = 0;
                    this.auSol = true;
                }
            }
            if (this.bas && !this.auSol) {
                vy += 2;
            }
            return;
        }

        Plateforme supportPrecedent = auSol ? support : null;
        if (supportPrecedent != null) {
            // Suivre la translation horizontale du support.
            x += supportPrecedent.getX() - dernierSupportX;
            x = Math.max(0,
                    Math.min(x, ConfigurationCarte.LARGEUR - LARGEUR_AVATAR));
        }

        double ySouhaite;
        if (auSol) {
            ySouhaite = supportPrecedent == null
                    ? y + 1
                    : supportPrecedent.getY() - HAUTEUR_AVATAR + 1;
        } else {
            ySouhaite = y + vy;
            vy += GRAVITE;
            if (bas) {
                vy += 2;
            }
        }

        if (plateformeEnCoursDeTraverse != null
                && ancienY + HAUTEUR_AVATAR
                > plateformeEnCoursDeTraverse.getY() + MARGE_TRAVERSEE) {
            plateformeEnCoursDeTraverse = null;
        }
        boolean ignorerTraversables = plateformeEnCoursDeTraverse != null;

        ResultatColision resultat = gestionnaire.resoudre(
                ancienX, ancienY, x, ySouhaite,
                LARGEUR_AVATAR, HAUTEUR_AVATAR, ignorerTraversables);

        x = resultat.getX();
        y = resultat.getY();
        if (resultat.estAuSol()) {
            auSol = true;
            vy = 0;
            support = resultat.getSupport();
            plateformeEnCoursDeTraverse = null;
        } else if (supportPrecedent != null && supportPrecedent.estActive()
                && x < supportPrecedent.getX() + supportPrecedent.getLargeur()
                && x + LARGEUR_AVATAR > supportPrecedent.getX()) {
            // Conserver l'appui sur un ascenseur qui monte.
            y = supportPrecedent.getY() - HAUTEUR_AVATAR;
            vy = 0;
            auSol = true;
            support = supportPrecedent;
            plateformeEnCoursDeTraverse = null;
        } else {
            if (resultat.touchePlafond() && vy < 0) {
                vy = 0;
            }
            auSol = false;
            support = null;
        }
        if (support != null) {
            dernierSupportX = support.getX();
        }

        // Garde-fou de l'ancienne version (Jeu gere les cartes flottantes).
        if (y > ConfigurationCarte.HAUTEUR + 150) {
            x = 600;
            y = 400;
            vy = 0;
            auSol = true;
            support = null;
            plateformeEnCoursDeTraverse = null;
        }
    }

    public void rendu(Graphics2D contexte) {
        if (sprite == null) {
            return;
        }
        if (regardeGauche) {
            contexte.drawImage(sprite, (int) x + 100, (int) y,
                    -100, 100, null);
        } else {
            contexte.drawImage(sprite, (int) x, (int) y,
                    100, 100, null);
        }
    }
}