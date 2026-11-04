package brawlmula;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Physique des plateformes (independante de la physique des personnages).
 *
 * Le code des personnages fournit une hitbox actuelle et une position souhaitee.
 * Ce gestionnaire renvoie une position corrigee et les contacts detectes.
 * Il ne gere ni clavier, ni saut, ni gravite, ni sprites.
 */
public final class GestionnairePlateforme {
    private static final double EPS = 1e-7;
    private final List<Plateforme> plateformes = new ArrayList<>();

    public void ajouter(Plateforme plateforme) {
        if (plateforme == null) {
            throw new IllegalArgumentException("La plateforme ne peut pas etre null.");
        }
        plateformes.add(plateforme);
    }

    public List<Plateforme> getPlateformes() {
        return Collections.unmodifiableList(plateformes);
    }

    /** Met a jour les plateformes mobiles ; dt en secondes. */
    public void miseAJour(double dt) {
        for (Plateforme plateforme : plateformes) {
            plateforme.miseAJour(dt);
        }
    }

    /**
     * Teste un mouvement de hitbox rectangulaire, sans deplacer l'objet teste.
     * Les x/y designent le coin SUPERIEUR GAUCHE de la hitbox.
     * Les collisions sont resolues d'abord horizontalement puis verticalement.
     *
     * @param xAvant x actuel du rectangle
     * @param yAvant y actuel du rectangle
     * @param xSouhaite x voulu par le code du personnage
     * @param ySouhaite y voulu par le code du personnage
     * @param largeur largeur de sa hitbox
     * @param hauteur hauteur de sa hitbox
     * @param ignorerTraversables vrai pour tomber volontairement a travers
     */
    public ResultatColision resoudre(double xAvant, double yAvant,
                                      double xSouhaite, double ySouhaite,
                                      double largeur, double hauteur,
                                      boolean ignorerTraversables) {
        if (!Double.isFinite(xAvant) || !Double.isFinite(yAvant)
                || !Double.isFinite(xSouhaite) || !Double.isFinite(ySouhaite)
                || !Double.isFinite(largeur) || !Double.isFinite(hauteur)
                || largeur <= 0 || hauteur <= 0) {
            throw new IllegalArgumentException("La hitbox ou les positions sont invalides.");
        }

        double x = xSouhaite;
        double y = ySouhaite;
        boolean murGauche = false;
        boolean murDroit = false;
        boolean plafond = false;
        boolean sol = false;
        Plateforme support = null;

        // 1. Axe horizontal : seules les plateformes solides ont des murs.
        // Utilise l'ancienne et la nouvelle position des plateformes mobiles.
        for (Plateforme p : plateformes) {
            if (p.getType() != Plateforme.Type.SOLIDE
                    || !chevauche(yAvant, hauteur, p.getY(), p.getHauteur())) {
                continue;
            }
            if (xAvant + largeur <= p.getAncienX() + EPS
                    && x + largeur >= p.getX() - EPS
                    && x + largeur > xAvant + largeur - EPS) {
                double bord = p.getX() - largeur;
                if (x > bord) {
                    x = bord;
                }
                murDroit = true;
            } else if (xAvant >= p.getAncienX() + p.getLargeur() - EPS
                    && x <= p.getX() + p.getLargeur() + EPS
                    && x < xAvant + EPS) {
                double bord = p.getX() + p.getLargeur();
                if (x < bord) {
                    x = bord;
                }
                murGauche = true;
            }
        }

        // 2. Axe vertical : toutes les plateformes ont un dessus.
        // Une traversable n'a ni plafond ni collision laterale.
        for (Plateforme p : plateformes) {
            if (!chevauche(x, largeur, p.getX(), p.getLargeur())) {
                continue;
            }
            if (p.getType() == Plateforme.Type.TRAVERSABLE && ignorerTraversables) {
                continue;
            }
            double ancienDessus = p.getAncienY();
            double nouveauDessus = p.getY();
            double piedAvant = yAvant + hauteur;
            double piedSouhaite = ySouhaite + hauteur;

            // La hitbox traverse le haut de la plateforme, y compris si celle-ci
            // monte vers elle. Selectionne la surface la plus haute atteinte.
            // Une plateforme TRAVERSABLE ne bloque que si les pieds
            // DESCENDENT (ou restent immobiles) dans le repere de l'ecran.
            // Ainsi un saut vers le haut n'est jamais intercepte, meme si
            // la plateforme mobile est elle aussi en train de monter.
            boolean descendOuImmobile = piedSouhaite >= piedAvant - EPS;
            boolean collisionParDessus = piedAvant <= ancienDessus + EPS
                    && piedSouhaite >= nouveauDessus - EPS;
            if (collisionParDessus && (p.getType() == Plateforme.Type.SOLIDE
                    || descendOuImmobile)) {
                double dessusCorrige = nouveauDessus - hauteur;
                if (y >= dessusCorrige - EPS) {
                    if (!sol || dessusCorrige < y - EPS) {
                        y = dessusCorrige;
                        support = p;
                    }
                    sol = true;
                }
            }

            if (p.getType() == Plateforme.Type.SOLIDE) {
                double teteAvant = yAvant;
                double ancienDessous = p.getAncienY() + p.getHauteur();
                double nouveauDessous = p.getY() + p.getHauteur();
                if (teteAvant >= ancienDessous - EPS
                        && ySouhaite <= nouveauDessous + EPS
                        && ySouhaite <= yAvant + EPS) {
                    if (y <= nouveauDessous + EPS) {
                        y = Math.max(y, nouveauDessous);
                        plafond = true;
                    }
                }
            }
        }
        return new ResultatColision(x, y, sol, plafond,
                                     murGauche, murDroit, support);
    }

    private static boolean chevauche(double debutA, double tailleA,
                                     double debutB, double tailleB) {
        return debutA < debutB + tailleB - EPS
                && debutA + tailleA > debutB + EPS;
    }

    /** Outil VISUEL de debug, facultatif. Ne constitue pas un rendu de decor. */
    public void dessinerHitboxes(Graphics2D g) {
        Color ancienneCouleur = g.getColor();
        for (Plateforme p : plateformes) {
            g.setColor(p.getType() == Plateforme.Type.SOLIDE
                    ? new Color(240, 75, 75, 90) : new Color(65, 195, 255, 90));
            Rectangle2D zone = p.getHitbox();
            g.fill(zone);
            g.setColor(p.getType() == Plateforme.Type.SOLIDE
                    ? Color.RED : Color.CYAN);
            g.draw(zone);
        }
        g.setColor(ancienneCouleur);
    }
}