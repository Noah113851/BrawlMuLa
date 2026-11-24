package brawlmula;

import java.awt.Color;

/**
 * Attaque lourde (spécialité) de chaque personnage.
 * Dégâts = dégâts de base (table Avatars) x multiplicateur.
 */
public enum AttaqueLourde {

    /** Carotte : coup d'estoc très long devant elle. */
    CAROTTE("Perforation", 2.1, 7_400_000_000L, new Color(255, 140, 0)),

    /** Nikku (poireau karatéka) : ruée vers l'avant qui frappe sur tout le trajet. */
    NIKKU("Ruée du dojo", 1.8, 6_000_000_000L, new Color(90, 200, 70)),

    /** Tomate : éclaboussure qui touche des deux côtés. */
    TOMATE("Éclaboussure", 1.6, 5_300_000_000L, new Color(230, 40, 40)),

    /** Patate : écrasement très puissant mais lent, plonge vers le sol en l'air. */
    PATATE("Écrasement", 2, 7_200_000_000L, new Color(200, 160, 90));

    /** Durée de l'animation d'une attaque lourde. */
    public static final long DUREE = 450_000_000L;

    private final String nom;
    private final double multiplicateur;
    private final long delai;
    private final Color couleur;

    AttaqueLourde(String nom, double multiplicateur, long delai, Color couleur) {
        this.nom = nom;
        this.multiplicateur = multiplicateur;
        this.delai = delai;
        this.couleur = couleur;
    }

    /** Attaque lourde associée au nom de personnage ("carrot", "nikku"...). */
    public static AttaqueLourde pour(String personnage) {
        return switch (personnage) {
            case "nikku" -> NIKKU;
            case "tomato" -> TOMATE;
            case "potato" -> PATATE;
            default -> CAROTTE;
        };
    }

    /** Dégâts de l'attaque lourde à partir des dégâts de base. */
    public int calculerDegats(int degatsDeBase) {
        return (int) Math.round(degatsDeBase * multiplicateur);
    }

    public String getNom() { return nom; }
    public double getMultiplicateur() { return multiplicateur; }
    /** Temps de recharge minimum entre deux attaques lourdes (ns). */
    public long getDelai() { return delai; }
    public Color getCouleur() { return couleur; }
}