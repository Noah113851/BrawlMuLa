package brawlmula;

import java.util.Locale;

/**
 * Choisit le decor et les plateformes en fonction de la carte retenue
 * dans Selection.java. Toutes les coordonnees sont dans un repere
 * logique de 1280 x 760 pixels (origine en haut a gauche).
 *
 * IMPORTANT : Les positions ci-dessous sont des exemples A CALIBRER sur
 * les images reelles, et non un relevé exact des decors.
 */
public final class ConfigurationCarte {

    public static final int LARGEUR = 1280;
    public static final int HAUTEUR = 760;

    private final String nom;
    private final String cheminDecor;
    private final GestionnairePlateforme plateformes;

    private ConfigurationCarte(String nom, String cheminDecor) {
        this.nom = nom;
        this.cheminDecor = cheminDecor;
        this.plateformes = new GestionnairePlateforme();
    }

    /**
     * Selection.java passe deja la chaine du type
     * "/ressources/cartes/chateau.png" a Combat.java.
     */
    public static ConfigurationCarte depuisChemin(String chemin) {
        if (chemin == null) {
            throw new IllegalArgumentException("Aucune carte choisie.");
        }
        String normalise = chemin.replace('\\', '/').toLowerCase(Locale.ROOT);
        ConfigurationCarte carte;

        if (normalise.endsWith("/colonie.png")) {
            carte = new ConfigurationCarte("COLONIE", "/ressources/cartes/colonie.png");
            carte.creerColonie();
        } else if (normalise.endsWith("/chateau.png")) {
            carte = new ConfigurationCarte("CHATEAU", "/ressources/cartes/chateau.png");
            carte.creerChateau();
        } else if (normalise.endsWith("/potager.png")) {
            carte = new ConfigurationCarte("POTAGER", "/ressources/cartes/potager.png");
            carte.creerPotager();
        } else if (normalise.endsWith("/marche.png")) {
            carte = new ConfigurationCarte("MARCHE", "/ressources/cartes/marche.png");
            carte.creerMarche();
        } else {
            throw new IllegalArgumentException("Carte inconnue : " + chemin);
        }
        return carte;
    }

    // SOLIDE = collisions sur 4 faces ; TRAVERSABLE = atterrissage par-dessus.
    // x, y : coin superieur gauche ; largeur, hauteur : dimensions en pixels.
    // Pour une mobile : x/y de depart, x/y d'arrivee, largeur, hauteur,
    //                    vitesse en pixels/seconde, type.

    private void creerColonie() {
        plateformes.ajouter(Plateforme.fixe(100, 625, 1080, 36, Plateforme.Type.SOLIDE));
        plateformes.ajouter(Plateforme.fixe(235, 470, 220, 20, Plateforme.Type.TRAVERSABLE));
        plateformes.ajouter(Plateforme.mobile(815, 420, 960, 420, 185, 20, 80,
                                              Plateforme.Type.TRAVERSABLE));
        plateformes.ajouter(Plateforme.mobile(560, 365, 560, 260, 170, 20, 55,
                                              Plateforme.Type.TRAVERSABLE));
    }

    /**
     * CHATEAU : disposition inspiree du decor fourni (sans plateformes incrustees).
     * Grande plateforme basse fixe et quatre petites plateformes qui oscillent.
     * Repere logique 1280 x 760. Les trajectoires sont des valeurs de depart
     * a ajuster apres validation visuelle en jeu.
     *
     * Toutes les petites plateformes sont TRAVERSABLES par-dessous.
     * Le sol principal est SOLIDE, son sprite est dans le decor.
     */
    private void creerChateau() {
        // Terrasse principale du chateau (le dessus est a y=500).
        plateformes.ajouter(Plateforme.fixe(195, 500, 815, 34,
                                           Plateforme.Type.SOLIDE));

        // Haut gauche : glisse de 56 pixels vers la droite puis revient.
        plateformes.ajouter(Plateforme.mobile(264, 219, 320, 219,
                                              172, 18, 46,
                                              Plateforme.Type.TRAVERSABLE));

        // Haut droite : glisse de 56 pixels vers la gauche puis revient.
        plateformes.ajouter(Plateforme.mobile(851, 199, 795, 199,
                                              154, 18, 46,
                                              Plateforme.Type.TRAVERSABLE));

        // Milieu gauche : monte de 42 pixels puis redescend.
        plateformes.ajouter(Plateforme.mobile(342, 361, 342, 319,
                                              182, 18, 30,
                                              Plateforme.Type.TRAVERSABLE));

        // Milieu droite : descend de 42 pixels puis remonte.
        plateformes.ajouter(Plateforme.mobile(744, 344, 744, 386,
                                              174, 18, 30,
                                              Plateforme.Type.TRAVERSABLE));
    }

    private void creerPotager() {
        plateformes.ajouter(Plateforme.fixe(110, 610, 1060, 40, Plateforme.Type.SOLIDE));
        plateformes.ajouter(Plateforme.fixe(525, 375, 230, 20, Plateforme.Type.TRAVERSABLE));
        plateformes.ajouter(Plateforme.mobile(205, 475, 375, 475, 195, 20, 65,
                                              Plateforme.Type.TRAVERSABLE));
        plateformes.ajouter(Plateforme.mobile(890, 465, 890, 320, 195, 20, 65,
                                              Plateforme.Type.TRAVERSABLE));
    }

    private void creerMarche() {
        plateformes.ajouter(Plateforme.fixe(90, 635, 1100, 36, Plateforme.Type.SOLIDE));
        plateformes.ajouter(Plateforme.fixe(535, 420, 210, 20, Plateforme.Type.TRAVERSABLE));
        plateformes.ajouter(Plateforme.mobile(190, 330, 380, 330, 180, 20, 85,
                                              Plateforme.Type.TRAVERSABLE));
        plateformes.ajouter(Plateforme.mobile(910, 330, 720, 330, 180, 20, 85,
                                              Plateforme.Type.TRAVERSABLE));
    }

    public String getNom() {
        return nom;
    }

    public String getCheminDecor() {
        return cheminDecor;
    }

    public GestionnairePlateforme getPlateformes() {
        return plateformes;
    }
}