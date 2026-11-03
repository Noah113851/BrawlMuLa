package brawlmula;

/**
 * Reponse du moteur de plateformes a une tentative de mouvement.
 * Ne modifie PAS la position, la vitesse ni les commandes d'un Avatar.
 */
public final class ResultatColision {

    private final double x;
    private final double y;
    private final boolean auSol;
    private final boolean touchePlafond;
    private final boolean toucheMurGauche;
    private final boolean toucheMurDroit;
    private final Plateforme support;

    ResultatColision(double x, double y, boolean auSol,
                      boolean touchePlafond, boolean toucheMurGauche,
                      boolean toucheMurDroit, Plateforme support) {
        this.x = x;
        this.y = y;
        this.auSol = auSol;
        this.touchePlafond = touchePlafond;
        this.toucheMurGauche = toucheMurGauche;
        this.toucheMurDroit = toucheMurDroit;
        this.support = support;
    }

    /** Position possible apres resolution des collisions. */
    public double getX() { return x; }
    public double getY() { return y; }
    public boolean estAuSol() { return auSol; }
    public boolean touchePlafond() { return touchePlafond; }
    public boolean toucheMurGauche() { return toucheMurGauche; }
    public boolean toucheMurDroit() { return toucheMurDroit; }

    /** Plateforme sous les pieds, ou null si le personnage n'est pas pose. */
    public Plateforme getSupport() { return support; }
}