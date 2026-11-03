package brawlmula;

import java.awt.geom.Rectangle2D;

/**
 * Une zone de collision rectangulaire du decor, independante des personnages.
 * Les coordonnees sont exprimees en pixels (origine en haut a gauche).
 */
public final class Plateforme {

    public enum Type {
        /** Collision sur le dessus, le dessous et les deux cotes. */
        SOLIDE,
        /** Collision uniquement lors d'un atterrissage par-dessus. */
        TRAVERSABLE
    }

    private final Type type;
    private final double largeur;
    private final double hauteur;
    private final double departX;
    private final double departY;
    private final double arriveeX;
    private final double arriveeY;
    private final double vitesse;  // pixels par seconde, 0 pour une plateforme fixe
    private final double longueurTrajet;

    private double x;
    private double y;
    private double ancienX;
    private double ancienY;
    private double progression;

    private Plateforme(double departX, double departY, double arriveeX,
                      double arriveeY, double largeur, double hauteur,
                      double vitesse, Type type) {
        if (type == null) {
            throw new IllegalArgumentException("Le type ne peut pas etre null.");
        }
        if (!Double.isFinite(departX) || !Double.isFinite(departY)
                || !Double.isFinite(arriveeX) || !Double.isFinite(arriveeY)
                || !Double.isFinite(largeur) || !Double.isFinite(hauteur)
                || !Double.isFinite(vitesse) || largeur <= 0 || hauteur <= 0
                || vitesse < 0) {
            throw new IllegalArgumentException("Dimensions, positions ou vitesse invalides.");
        }
        this.type = type;
        this.largeur = largeur;
        this.hauteur = hauteur;
        this.departX = departX;
        this.departY = departY;
        this.arriveeX = arriveeX;
        this.arriveeY = arriveeY;
        this.vitesse = vitesse;
        this.longueurTrajet = Math.hypot(arriveeX - departX, arriveeY - departY);
        if (vitesse > 0 && longueurTrajet == 0) {
            throw new IllegalArgumentException("Une plateforme mobile doit avoir un trajet non nul.");
        }
        this.x = departX;
        this.y = departY;
        this.ancienX = departX;
        this.ancienY = departY;
    }

    /** Cree une plateforme immobile. */
    public static Plateforme fixe(double x, double y, double largeur,
                                  double hauteur, Type type) {
        return new Plateforme(x, y, x, y, largeur, hauteur, 0, type);
    }

    /**
     * Cree une plateforme faisant l'aller-retour entre deux positions.
     * La vitesse est en pixels par seconde.
     */
    public static Plateforme mobile(double xDepart, double yDepart,
                                    double xArrivee, double yArrivee,
                                    double largeur, double hauteur,
                                    double vitesse, Type type) {
        if (vitesse <= 0) {
            throw new IllegalArgumentException("La vitesse d'une plateforme mobile doit etre positive.");
        }
        return new Plateforme(xDepart, yDepart, xArrivee, yArrivee,
                              largeur, hauteur, vitesse, type);
    }

    /** Avance la plateforme de dt secondes. Aucun mouvement de personnage ici. */
    public void miseAJour(double dt) {
        if (!Double.isFinite(dt) || dt < 0) {
            throw new IllegalArgumentException("dt doit etre positif ou nul et fini.");
        }
        ancienX = x;
        ancienY = y;
        if (vitesse == 0 || dt == 0) {
            return;
        }
        double cycle = 2 * longueurTrajet;
        progression = (progression + (vitesse * dt) % cycle) % cycle;
        double parcours = progression <= longueurTrajet
                ? progression : cycle - progression;
        double ratio = parcours / longueurTrajet;
        x = departX + (arriveeX - departX) * ratio;
        y = departY + (arriveeY - departY) * ratio;
    }

    public Type getType() { return type; }
    public boolean estMobile() { return vitesse > 0; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getAncienX() { return ancienX; }
    public double getAncienY() { return ancienY; }
    public double getLargeur() { return largeur; }
    public double getHauteur() { return hauteur; }
    public double getDeplacementX() { return x - ancienX; }
    public double getDeplacementY() { return y - ancienY; }

    public Rectangle2D.Double getHitbox() {
        return new Rectangle2D.Double(x, y, largeur, hauteur);
    }
}