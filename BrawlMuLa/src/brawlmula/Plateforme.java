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

    // Parametres de l'eventuelle trajectoire circulaire.
    // Le cercle decrit le mouvement du coin superieur gauche du sprite.
    private final boolean cercle;
    private final double centreX;
    private final double centreY;
    private final double rayon;
    private final int sensRotation; // +1 horaire a l'ecran, -1 antihoraire
    private double angle;

    private double x;
    private double y;
    private double ancienX;
    private double ancienY;
    private double progression;

    private Plateforme(double departX, double departY, double arriveeX,
                      double arriveeY, double largeur, double hauteur,
                      double vitesse, Type type) {
        this(departX, departY, arriveeX, arriveeY,
                largeur, hauteur, vitesse, type,
                false, 0.0, 0.0, 0.0, 0.0, 0);
    }

    private Plateforme(double departX, double departY, double arriveeX,
                       double arriveeY, double largeur, double hauteur,
                       double vitesse, Type type, boolean cercle,
                       double centreX, double centreY, double rayon,
                       double angleInitial, int sensRotation) {
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
        if (vitesse > 0 && longueurTrajet == 0 && !cercle) {
            throw new IllegalArgumentException("Une plateforme mobile doit avoir un trajet non nul.");
        }
        if (cercle && (!Double.isFinite(centreX) || !Double.isFinite(centreY)
                || !Double.isFinite(rayon) || rayon <= 0
                || !Double.isFinite(angleInitial) || vitesse <= 0
                || (sensRotation != 1 && sensRotation != -1))) {
            throw new IllegalArgumentException("Parametres du cercle invalides.");
        }
        this.cercle = cercle;
        this.centreX = centreX;
        this.centreY = centreY;
        this.rayon = rayon;
        this.angle = angleInitial;
        this.sensRotation = sensRotation;
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

    /**
     * Cree une plateforme mobile sur une trajectoire CIRCULAIRE.
     * centreX/centreY : centre du cercle decrit par le coin superieur gauche.
     * rayon : rayon en pixels ; vitesse : vitesse tangentielle en pixels/s.
     * angleInitial : en radians (0 = point a droite du centre).
     * sensHoraire : true = rotation horaire a l'ecran (Y augmente vers le bas).
     * La plateforme reste horizontale pendant le mouvement.
     */
    public static Plateforme circulaire(double centreX, double centreY,
                                        double rayon, double largeur,
                                        double hauteur, double vitesse,
                                        double angleInitial, boolean sensHoraire,
                                        Type type) {
        if (!Double.isFinite(centreX) || !Double.isFinite(centreY)
                || !Double.isFinite(rayon) || rayon <= 0
                || !Double.isFinite(angleInitial)
                || !Double.isFinite(vitesse) || vitesse <= 0) {
            throw new IllegalArgumentException("Cercle ou vitesse invalides.");
        }
        double initialX = centreX + rayon * Math.cos(angleInitial);
        double initialY = centreY + rayon * Math.sin(angleInitial);
        return new Plateforme(initialX, initialY, initialX, initialY,
                largeur, hauteur, vitesse, type,
                true, centreX, centreY, rayon, angleInitial,
                sensHoraire ? 1 : -1);
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
        if (cercle) {
            // Conversion vitesse tangentielle -> vitesse angulaire (rad/s).
            angle = (angle + sensRotation * (vitesse / rayon) * dt)
                    % (2.0 * Math.PI);
            x = centreX + rayon * Math.cos(angle);
            y = centreY + rayon * Math.sin(angle);
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
    public boolean estCirculaire() { return cercle; }
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
