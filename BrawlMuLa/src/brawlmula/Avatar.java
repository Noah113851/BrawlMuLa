/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Avatar d'origine avec branchement facultatif aux plateformes. Garde les memes
 * touches, vitesses, saut, gravite, rendu et constructeur.
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

    // Ajouts minimaux pour le test du Chateau.
    private GestionnairePlateforme gestionnaire;
    private Plateforme support;
    private double dernierSupportX;

    // Plateforme que le joueur vient de quitter en appuyant sur S.
    // On ignore temporairement les plateformes TRAVERSABLES pendant
    // que ses pieds depassent le dessus de cette plateforme.
    private Plateforme plateformeEnCoursDeTraverse;
    private static final double MARGE_TRAVERSEE = 8.0;

    public Avatar() {
        try {
            this.sprite = ImageIO.read(getClass().getClassLoader()
                    .getResource("ressources/avatars/carrot_fight.png"));
        } catch (IOException ex) {
            Logger.getLogger(Avatar.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.x = 100;
        this.y = SOL;
        this.vy = 0;
        this.auSol = true;
        this.gauche = false;
        this.droite = false;
        this.bas = false;
    }

    public Avatar(String nomPersonnage) {
        this(); // Appelle le constructeur par défaut
        // Charger le sprite spécifique au nom (ex: "Carrot", "Nikku", "Tomato", "Potato")
        if (nomPersonnage != null && !nomPersonnage.isEmpty()) {
            chargerSpritePersonnage(nomPersonnage);
        }
    }

    /**
     * A appeler UNE fois depuis Jeu : le personnage partage exactement les
     * memes plateformes que celles dessinees par PanneauCombat.
     */
    public void connecterPlateformes(GestionnairePlateforme gestionnaire) {
        if (gestionnaire == null) {
            throw new IllegalArgumentException("GestionnairePlateforme manquant");
        }
        this.gestionnaire = gestionnaire;
        this.support = null;
        this.dernierSupportX = 0;
        this.plateformeEnCoursDeTraverse = null;
        this.x = 600;
        this.y = 400; // Pieds en y=500 : dessus de la grande plateforme du Chateau.
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

    public void miseAJour() {
        // Si une plateforme temporaire disparait sous les pieds,
        // abandonner son support AVANT la resolution des collisions.
        // Sinon la correction des plateformes mobiles peut maintenir
        // artificiellement l'avatar en l'air sur le support invisible.
        if (support != null && !support.estActive()) {
            support = null;
            auSol = false;
            vy = Math.max(0, vy);
            plateformeEnCoursDeTraverse = null;
        }

        double ancienX = x;
        double ancienY = y;

        // Deplacement horizontal d'origine, conserve tel quel.
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

        // Sur Chateau, le repere est 1280 px et non 960.
        double largeurMonde = gestionnaire == null ? 960 : ConfigurationCarte.LARGEUR;
        if (x > largeurMonde - LARGEUR_AVATAR) {
            x = largeurMonde - LARGEUR_AVATAR;
        }
        if (x < 0) {
            x = 0;
        }

        // Appuyer sur S lorsque les pieds reposent sur une petite plateforme :
        // on la quitte sans la rendre solide pendant notre descente.
        // Attention : la grande plateforme SOLIDE reste infranchissable.
        if (gestionnaire != null && bas && auSol && support != null
                && support.getType() == Plateforme.Type.TRAVERSABLE) {
            plateformeEnCoursDeTraverse = support;
            support = null;
            auSol = false;
            vy = Math.max(vy, 7.0); // lancer une petite impulsion vers le bas
        }

        // Chute rapide d'origine.
        if (this.bas && !this.auSol) {
            if (vy < 0) {
                vy = 0;
            }
            vy += 3;
        }

        if (gestionnaire == null) {
            // Comportement historique si l'avatar est cree hors du test Chateau.
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

        // Partie nouvelle : adapter les positions voulues a la physique du Chateau.
        Plateforme supportPrecedent = auSol ? support : null;
        if (supportPrecedent != null) {
            // Porter le joueur lorsqu'une plateforme se deplace horizontalement.
            x += supportPrecedent.getX() - dernierSupportX;
            x = Math.max(0, Math.min(x, ConfigurationCarte.LARGEUR - LARGEUR_AVATAR));
        }

        double ySouhaite;
        if (auSol) {
            ySouhaite = supportPrecedent == null ? y + 1
                    : supportPrecedent.getY() - HAUTEUR_AVATAR + 1;
        } else {
            ySouhaite = y + vy;
            vy += GRAVITE;
            if (bas) {
                vy += 2;
            }
        }

        // Apres le debut de la traversee, on reactivera les collisions
        // TRAVERSABLES des que les pieds auront depasse de 8 px le dessus
        // de la plateforme quittee (meme si S a deja ete relachee).
        // La comparaison est faite AVANT la collision pour ne pas se recoller.
        if (plateformeEnCoursDeTraverse != null
                && ancienY + HAUTEUR_AVATAR
                > plateformeEnCoursDeTraverse.getY() + MARGE_TRAVERSEE) {
            plateformeEnCoursDeTraverse = null;
        }
        boolean ignorerTraversables = plateformeEnCoursDeTraverse != null;

        // ResultatColision : un seul l, conforme au projet BrawlMuLa.
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
            // Une plateforme verticale qui monte peut ne pas etre detectee par
            // le balayage ; dans ce cas conserver son appui jusqu'a la quitter.
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

        // Reapparition si on tombe hors de l'arene pendant les tests.
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
        if (regardeGauche) {
            contexte.drawImage(this.sprite, (int) x + 100, (int) y,
                    -100, 100, null);
        } else {
            contexte.drawImage(this.sprite, (int) x, (int) y,
                    100, 100, null);
        }
    }
}
