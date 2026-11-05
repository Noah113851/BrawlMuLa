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

    /**
     * COLONIE : cinq PETITES plateformes qui se suivent en orbite
     * elliptique, plus une grande plateforme centrale fixe.
     * Cette configuration est unique aux quatre cartes.
     *
     * L'ellipse decrit les positions des COINS SUPERIEURS GAUCHES
     * des hitbox : le dessin PNG est centre sur celles-ci.
     */
    private void creerColonie() {
        final double centreX = 548;
        final double centreY = 274;
        final double rayonX = 340;
        final double rayonY = 110;
        final double vitesseAngulaire = 0.42; // radians par seconde

        // Cinq plateformes differentes avec un ecart constant de 72 degres.
        for (int i = 0; i < 5; i++) {
            plateformes.ajouter(Plateforme.elliptique(
                    centreX, centreY, rayonX, rayonY,
                    182, 14, vitesseAngulaire,
                    i * (2.0 * Math.PI / 5.0),
                    Plateforme.Type.TRAVERSABLE));
        }

        // Seul support SOLIDE : pas de collision avec le sol du decor.
        // Le personnage tombe dans le vide et reapparait sur ce plateau.
        plateformes.ajouter(Plateforme.fixe(
                298, 535, 684, 18, Plateforme.Type.SOLIDE));

        // DEUX PLATEFORMES TEMPORAIRES, en bas aux extremites :
        // 10 secondes visibles / 10 secondes absentes, synchronisees.
        // Elles sont TRAVERSABLES et n'ont pas de collision invisibles.
        plateformes.ajouter(Plateforme.intermittente(
                72, 651, 182, 14, 5.0, Plateforme.Type.TRAVERSABLE));
        plateformes.ajouter(Plateforme.intermittente(
                1026, 651, 182, 14, 5.0, Plateforme.Type.TRAVERSABLE));
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

    /**
     * POTAGER : seulement TROIS petites plateformes, plus la grande centrale.
     * Contrairement au Chateau et au Marche : AUCUN mouvement circulaire,
     * et deux longues trajectoires diagonales en miroir.
     *
     * Les petites restent TRAVERSABLES avec S. La grande est SOLIDE,
     * et il n'y a aucun sol invisible en bas de l'image.
     * Les hitbox de surface et le dessin restent ceux de PanneauCombat.
     *
     * Ordre : diagonale gauche, ascenseur central, diagonale droite,
     *         grande plateforme fixe.
     */
    private void creerPotager() {
        // GAUCHE : la plateforme monte en se rapprochant du centre,
        // puis revient en bas a gauche (aller-retour diagonal).
        plateformes.ajouter(Plateforme.mobile(
                220, 365, 350, 285, 206, 14, 64,
                Plateforme.Type.TRAVERSABLE));

        // CENTRE : un petit ascenseur qui oscille verticalement.
        plateformes.ajouter(Plateforme.mobile(
                537, 210, 537, 278, 206, 14, 42,
                Plateforme.Type.TRAVERSABLE));

        // DROITE : diagonale symetrique, vers le haut et le centre.
        plateformes.ajouter(Plateforme.mobile(
                854, 365, 724, 285, 206, 14, 64,
                Plateforme.Type.TRAVERSABLE));

        // GRANDE CENTRALE : seul support SOLIDE du Potager.
        // Memes dimensions et position que la version precedente.
        plateformes.ajouter(Plateforme.fixe(
                320, 515, 640, 18, Plateforme.Type.SOLIDE));
    }

    /**
     * MARCHE : cinq plateformes visuelles, AUCUN sol invisible.
     * Les quatre petites sont TRAVERSABLES ; la grande centrale est SOLIDE.
     * Les hitbox sont de fines bandes situees a la surface des PNG.
     * PanneauCombat applique les memes marges visuelles qu'avant.
     *
     * Ordre : haute gauche, basse gauche, haute droite,
     * basse droite, grande centrale.
     */
    private void creerMarche() {
        // HAUTE GAUCHE : cercle horaire de rayon 40 px.
        plateformes.ajouter(Plateforme.circulaire(
                190, 138, 40, 212, 14, 52, 0.0, true,
                Plateforme.Type.TRAVERSABLE));

        // BASSE GAUCHE : aller-retour horizontal.
        plateformes.ajouter(Plateforme.mobile(
                178, 315, 250, 315, 212, 14, 45,
                Plateforme.Type.TRAVERSABLE));

        // HAUTE DROITE : cercle antihoraire.
        plateformes.ajouter(Plateforme.circulaire(
                878, 138, 40, 212, 14, 52, Math.PI, false,
                Plateforme.Type.TRAVERSABLE));

        // BASSE DROITE : aller-retour horizontal, sens inverse.
        plateformes.ajouter(Plateforme.mobile(
                888, 315, 816, 315, 212, 14, 45,
                Plateforme.Type.TRAVERSABLE));

        // GRANDE CENTRALE (plateforme du bas) : SEUL SOLIDE DU MARCHE.
        // Hitbox sur le dessus du PNG 674 x 115 dessine en x=303,y=433.
        // Aucun sol de collision n'existe en bas de l'image : la chute
        // hors du plateau est donc possible des deux cotes.
        plateformes.ajouter(Plateforme.fixe(
                319, 445, 642, 18, Plateforme.Type.SOLIDE));
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