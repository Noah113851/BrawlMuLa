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
     * "/ressources/Maps/chateau.png" a Combat.java.
     */
    public static ConfigurationCarte depuisChemin(String chemin) {
        if (chemin == null) {
            throw new IllegalArgumentException("Aucune carte choisie.");
        }
        String normalise = chemin.replace('\\', '/').toLowerCase(Locale.ROOT);
        ConfigurationCarte carte;

        if (normalise.endsWith("/colonie.png")) {
            carte = new ConfigurationCarte("COLONIE", "/ressources/Maps/colonie.png");
            carte.creerColonie();
        } else if (normalise.endsWith("/chateau.png")) {
            carte = new ConfigurationCarte("CHATEAU", "/ressources/Maps/chateau.png");
            carte.creerChateau();
        } else if (normalise.endsWith("/potager.png")) {
            carte = new ConfigurationCarte("POTAGER", "/ressources/Maps/potager.png");
            carte.creerPotager();
        } else if (normalise.endsWith("/marche.png")) {
            carte = new ConfigurationCarte("MARCHE", "/ressources/Maps/marche.png");
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
        // Grande terrasse : deja dessinee dans le decor.
        plateformes.ajouter(Plateforme.fixe(
                195, 500, 815, 34, Plateforme.Type.SOLIDE));

        // Les 2 plateformes du haut : visuel plateforme_chateau_1.png.
        // 230 x 52 px, plus large et plus epais qu'avant (172 x 18).
        // Haut gauche : mouvement horizontal de 56 pixels.
        plateformes.ajouter(Plateforme.mobile(
                245, 217, 301, 217, 230, 52, 46,
                Plateforme.Type.TRAVERSABLE));

        // Haut droite : meme sprite, direction opposee.
        plateformes.ajouter(Plateforme.mobile(
                805, 198, 749, 198, 230, 52, 46,
                Plateforme.Type.TRAVERSABLE));

        // Les 2 plateformes du milieu : visuel plateforme_chateau_2.png.
        // 240 x 55 px ; trajectoires verticales independantes.
        plateformes.ajouter(Plateforme.mobile(
                320, 323, 320, 365, 240, 55, 30,
                Plateforme.Type.TRAVERSABLE));

        plateformes.ajouter(Plateforme.mobile(
                720, 382, 720, 340, 240, 55, 30,
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

    /**
     * MARCHE : 2 petites plateformes mobiles et 1 grande plateforme centrale.
     * Coordonnees de depart pour un decor logique de 1280 x 760.
     * Ces valeurs pourront etre ajustees apres essai avec les PNG definitifs.
     *
     * Les 3 plateformes aeriennes sont TRAVERSABLES : passage en montant,
     * atterrissage en descendant, et chute au travers avec S.
     * Le sol de la place est SOLIDE : on ne peut pas le traverser avec S.
     */
    private void creerMarche() {
        // Sol de la place, visible dans l'image du decor.
        plateformes.ajouter(Plateforme.fixe(
                90, 635, 1100, 34, Plateforme.Type.SOLIDE));

        // En haut a gauche : sprite plateforme_marche_haut_gauche.png
        plateformes.ajouter(Plateforme.mobile(
                245, 205, 300, 205, 230, 52, 46,
                Plateforme.Type.TRAVERSABLE));

        // En haut a droite : sprite plateforme_marche_haut_droite.png
        plateformes.ajouter(Plateforme.mobile(
                805, 205, 750, 205, 230, 52, 46,
                Plateforme.Type.TRAVERSABLE));

        // Grande plateforme centrale : sprite plateforme_marche_principale.png
        plateformes.ajouter(Plateforme.fixe(
                310, 365, 660, 115, Plateforme.Type.TRAVERSABLE));
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
