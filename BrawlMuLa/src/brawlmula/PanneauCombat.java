
package brawlmula;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Affichage des decors et des plateformes.
 * La physique est geree par GestionnairePlateforme.
 */
public final class PanneauCombat extends JPanel {

    // ===== CHATEAU =====

    public static final String SPRITE_CHATEAU_HAUT =
            "/ressources/plateformes/plateforme_chateau_1.png";

    public static final String SPRITE_CHATEAU_MILIEU =
            "/ressources/plateformes/plateforme_chateau_2.png";

    // ===== MARCHE =====

    public static final String SPRITE_MARCHE_HAUT_GAUCHE =
            "/ressources/plateformes/plateforme_marche_haut_gauche.png";

    public static final String SPRITE_MARCHE_HAUT_DROITE =
            "/ressources/plateformes/plateforme_marche_haut_droite.png";

    public static final String SPRITE_MARCHE_PRINCIPALE =
            "/ressources/plateformes/plateforme_marche_principale.png";

    // ===== POTAGER =====

    public static final String SPRITE_POTAGER_PETITE =
            "/ressources/plateformes/plateforme_gauche.png";

    public static final String SPRITE_POTAGER_GRANDE =
            "/ressources/plateformes/plateforme_grande.png";

    private static final int POTAGER_PETITE_LARGEUR_AFF = 250;
    private static final int POTAGER_PETITE_HAUTEUR_AFF = 78;

    private static final int POTAGER_GRANDE_LARGEUR_AFF = 680;
    private static final int POTAGER_GRANDE_HAUTEUR_AFF = 170;

    // ===== COLONIE =====

    public static final String SPRITE_COLONIE_PETITE =
            "/ressources/plateformes/petite_plateforme_mars.png";

    public static final String SPRITE_COLONIE_GRANDE =
            "/ressources/plateformes/Grande platforme mars.png";

    // Nouveau PNG pour les plateformes temporaires.
    public static final String SPRITE_COLONIE_HOLOGRAPHIQUE =
            "/ressources/plateformes/petite_plateforme_holographique.png";

    // ===== ATTRIBUTS =====

    private final ConfigurationCarte carte;
    private final BufferedImage decor;

    private BufferedImage spriteChateauHaut;
    private BufferedImage spriteChateauMilieu;

    private BufferedImage spriteMarcheGauche;
    private BufferedImage spriteMarcheDroite;
    private BufferedImage spriteMarchePrincipale;

    private BufferedImage spritePotagerPetite;
    private BufferedImage spritePotagerGrande;

    private BufferedImage spriteColoniePetite;
    private BufferedImage spriteColonieGrande;
    private BufferedImage spriteColonieHolographique;

    private final Map<Plateforme, BufferedImage> imagesPlateformes =
            new IdentityHashMap<>();

    private final Timer timer;
    private long precedentNanos;
    private boolean afficherHitboxes = false;

    // ===== CONSTRUCTEUR =====

    public PanneauCombat(String cheminCarte) {

        carte = ConfigurationCarte.depuisChemin(cheminCarte);
        decor = lireImageObligatoire(carte.getCheminDecor());

        if ("CHATEAU".equals(carte.getNom())) {

            spriteChateauHaut =
                    lireSpriteFacultatif(SPRITE_CHATEAU_HAUT);

            spriteChateauMilieu =
                    lireSpriteFacultatif(SPRITE_CHATEAU_MILIEU);

            attribuerImagesChateau();

        } else if ("MARCHE".equals(carte.getNom())) {

            spriteMarcheGauche =
                    lireSpriteFacultatif(SPRITE_MARCHE_HAUT_GAUCHE);

            spriteMarcheDroite =
                    lireSpriteFacultatif(SPRITE_MARCHE_HAUT_DROITE);

            spriteMarchePrincipale =
                    lireSpriteFacultatif(SPRITE_MARCHE_PRINCIPALE);

            attribuerImagesMarche();

        } else if ("POTAGER".equals(carte.getNom())) {

            spritePotagerPetite =
                    lireSpriteFacultatif(SPRITE_POTAGER_PETITE);

            spritePotagerGrande =
                    lireSpriteFacultatif(SPRITE_POTAGER_GRANDE);

            attribuerImagesPotager();

        } else if ("COLONIE".equals(carte.getNom())) {

            spriteColoniePetite =
                    lireSpriteFacultatif(SPRITE_COLONIE_PETITE);

            spriteColonieGrande =
                    lireSpriteFacultatif(SPRITE_COLONIE_GRANDE);

            spriteColonieHolographique =
                    lireSpriteFacultatif(SPRITE_COLONIE_HOLOGRAPHIQUE);

            attribuerImagesColonie();
        }

        setPreferredSize(new Dimension(
                ConfigurationCarte.LARGEUR,
                ConfigurationCarte.HAUTEUR));

        setDoubleBuffered(true);
        setBackground(Color.BLACK);

        timer = new Timer(16, e -> animer());
        timer.setCoalesce(true);
    }

    // ===== CHARGEMENT DES IMAGES =====

    private BufferedImage lireImageObligatoire(String chemin) {

        URL url = getClass().getResource(chemin);

        if (url == null) {
            throw new IllegalStateException(
                    "Decor introuvable : " + chemin);
        }

        try {
            BufferedImage image = ImageIO.read(url);

            if (image == null) {
                throw new IOException("Image du decor invalide");
            }

            return image;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Erreur de lecture : " + chemin, e);
        }
    }

    /**
     * Charge un sprite et supprime les marges transparentes.
     */
    private BufferedImage lireSpriteFacultatif(String chemin) {

        URL url = getClass().getResource(chemin);

        if (url == null) {
            System.err.println(
                    "[BrawlMuLa] Sprite non trouve : " + chemin);
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(url);

            if (image == null) {
                throw new IOException("Image invalide");
            }

            int gauche = image.getWidth();
            int droite = -1;
            int haut = image.getHeight();
            int bas = -1;

            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {

                    if ((image.getRGB(x, y) >>> 24) > 4) {
                        gauche = Math.min(gauche, x);
                        droite = Math.max(droite, x);
                        haut = Math.min(haut, y);
                        bas = Math.max(bas, y);
                    }
                }
            }

            if (droite < gauche) {
                throw new IOException("Image transparente ou vide");
            }

            return image.getSubimage(
                    gauche,
                    haut,
                    droite - gauche + 1,
                    bas - haut + 1);

        } catch (IOException e) {
            System.err.println(
                    "[BrawlMuLa] Sprite inutilisable : " + chemin);
            return null;
        }
    }

    // ===== ASSOCIATION DES SPRITES =====

    private void attribuerImagesChateau() {

        List<Plateforme> aeriennes = new ArrayList<>();

        for (Plateforme p : carte.getPlateformes().getPlateformes()) {

            if (p.getType() == Plateforme.Type.TRAVERSABLE) {
                aeriennes.add(p);
            }
        }

        aeriennes.sort(
                Comparator.comparingDouble(Plateforme::getY));

        for (int i = 0; i < aeriennes.size(); i++) {

            imagesPlateformes.put(
                    aeriennes.get(i),
                    i < 2 ? spriteChateauHaut : spriteChateauMilieu);
        }
    }

    private void attribuerImagesMarche() {

        for (Plateforme p : carte.getPlateformes().getPlateformes()) {

            if (p.getType() != Plateforme.Type.TRAVERSABLE
                    && p.getLargeur() < 400) {
                continue;
            }

            if (p.getLargeur() >= 400) {

                imagesPlateformes.put(p, spriteMarchePrincipale);

            } else if (p.getX() < ConfigurationCarte.LARGEUR / 2) {

                imagesPlateformes.put(p, spriteMarcheGauche);

            } else {

                imagesPlateformes.put(p, spriteMarcheDroite);
            }
        }
    }

    private void attribuerImagesPotager() {

        for (Plateforme p : carte.getPlateformes().getPlateformes()) {

            imagesPlateformes.put(
                    p,
                    p.getType() == Plateforme.Type.SOLIDE
                            ? spritePotagerGrande
                            : spritePotagerPetite);
        }
    }

    /**
     * Colonie :
     * - 5 plateformes en orbite : PNG martien classique
     * - 2 plateformes temporaires : PNG holographique
     * - 1 plateforme centrale : grand PNG martien
     */
    private void attribuerImagesColonie() {

        for (Plateforme p : carte.getPlateformes().getPlateformes()) {

            BufferedImage sprite;

            if (p.estIntermittente()) {

                // Plateformes qui apparaissent et disparaissent.
                sprite = spriteColonieHolographique;

            } else if (p.getType() == Plateforme.Type.SOLIDE) {

                // Grande plateforme centrale.
                sprite = spriteColonieGrande;

            } else {

                // Les cinq plateformes en orbite.
                sprite = spriteColoniePetite;
            }

            imagesPlateformes.put(p, sprite);
        }
    }

    // ===== ANIMATION =====

    private void animer() {

        long maintenant = System.nanoTime();

        double dt = (maintenant - precedentNanos)
                / 1_000_000_000.0;

        precedentNanos = maintenant;

        carte.getPlateformes().miseAJour(
                Math.max(0.0, Math.min(dt, 0.05)));

        repaint();
    }

    @Override
    public void addNotify() {
        super.addNotify();

        precedentNanos = System.nanoTime();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    // ===== AFFICHAGE =====

    @Override
    protected void paintComponent(Graphics graphismes) {

        super.paintComponent(graphismes);

        Graphics2D g = (Graphics2D) graphismes.create();

        try {
            g.scale(
                    getWidth() / (double) ConfigurationCarte.LARGEUR,
                    getHeight() / (double) ConfigurationCarte.HAUTEUR);

            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // Dessin du decor.
            g.drawImage(
                    decor, 0, 0,
                    ConfigurationCarte.LARGEUR,
                    ConfigurationCarte.HAUTEUR,
                    null);

            // Dessin des plateformes.
            for (Plateforme p :
                    carte.getPlateformes().getPlateformes()) {

                // Si une plateforme temporaire est inactive,
                // son PNG n'est pas affiche.
                if (!p.estActive()) {
                    continue;
                }

                // Les grandes plateformes du Marche, Potager
                // et Colonie sont solides mais visibles en PNG.
                boolean principaleFlottante =
                        ("MARCHE".equals(carte.getNom())
                        || "POTAGER".equals(carte.getNom())
                        || "COLONIE".equals(carte.getNom()))
                        && p.getType() == Plateforme.Type.SOLIDE;

                if (p.getType() != Plateforme.Type.TRAVERSABLE
                        && !principaleFlottante) {
                    continue;
                }

                Rectangle zone;

                if ("MARCHE".equals(carte.getNom())) {

                    zone = rectangleAffichageMarche(p);

                } else if ("POTAGER".equals(carte.getNom())) {

                    zone = rectangleAffichagePotager(p);

                } else if ("COLONIE".equals(carte.getNom())) {

                    zone = rectangleAffichageColonie(
                            p, imagesPlateformes.get(p));

                } else {

                    zone = new Rectangle(
                            (int) Math.round(p.getX()),
                            (int) Math.round(p.getY()),
                            (int) Math.round(p.getLargeur()),
                            (int) Math.round(p.getHauteur()));
                }

                BufferedImage sprite = imagesPlateformes.get(p);

                if (sprite != null) {

                    g.drawImage(
                            sprite,
                            zone.x,
                            zone.y,
                            zone.width,
                            zone.height,
                            null);

                } else {

                    dessinerPlateformeProvisoire(
                            g,
                            zone.x,
                            zone.y,
                            zone.width,
                            zone.height);
                }
            }

            if (afficherHitboxes) {
                carte.getPlateformes().dessinerHitboxes(g);
            }

        } finally {
            g.dispose();
        }
    }

    // ===== DIMENSIONS DU MARCHE =====

    static Rectangle rectangleAffichageMarche(Plateforme p) {

        boolean grande = p.getLargeur() >= 400;

        int margeX = grande ? 16 : 10;
        int margeY = grande ? 12 : 9;
        int hauteurVisuelle = grande ? 115 : 57;

        return new Rectangle(
                (int) Math.round(p.getX()) - margeX,
                (int) Math.round(p.getY()) - margeY,
                (int) Math.round(p.getLargeur()) + 2 * margeX,
                hauteurVisuelle);
    }

    // ===== DIMENSIONS DU POTAGER =====

    static Rectangle rectangleAffichagePotager(Plateforme p) {

        boolean grande = p.getType() == Plateforme.Type.SOLIDE;

        int largeur = grande
                ? POTAGER_GRANDE_LARGEUR_AFF
                : POTAGER_PETITE_LARGEUR_AFF;

        int hauteur = grande
                ? POTAGER_GRANDE_HAUTEUR_AFF
                : POTAGER_PETITE_HAUTEUR_AFF;

        int x = (int) Math.round(p.getX())
                - (largeur - (int) Math.round(p.getLargeur())) / 2;

        int y = (int) Math.round(p.getY())
                - (grande ? 32 : 18);

        return new Rectangle(x, y, largeur, hauteur);
    }

    // ===== DIMENSIONS DE LA COLONIE =====

    /**
     * Conserve les proportions originales de chaque PNG.
     * Les plateformes holographiques sont aussi proportionnelles.
     */
    static Rectangle rectangleAffichageColonie(
            Plateforme p, BufferedImage sprite) {

        boolean grande = p.getType() == Plateforme.Type.SOLIDE;

        int largeur = grande ? 724 : 226;
        int hauteur;

        if (sprite != null && sprite.getWidth() > 0) {

            hauteur = (int) Math.round(
                    largeur * (sprite.getHeight()
                    / (double) sprite.getWidth()));

        } else {

            hauteur = grande ? 165 : 70;
        }

        int x = (int) Math.round(
                p.getX()
                + (p.getLargeur() - largeur) / 2.0);

        // Ajustement pour que le personnage ait les pieds
        // sur le dessus de la grande plateforme.
        int y = (int) Math.round(p.getY())
                - (grande ? 30 : 10);

        return new Rectangle(
                x, y, largeur, Math.max(1, hauteur));
    }

    // ===== DESSIN DE SECOURS =====

    private static void dessinerPlateformeProvisoire(
            Graphics2D g, int x, int y, int w, int h) {

        g.setColor(new Color(0, 0, 0, 95));
        g.fillRoundRect(x + 4, y + 7, w, h, 12, 12);

        g.setColor(new Color(126, 84, 52));
        g.fillRoundRect(x, y, w, h, 10, 10);

        g.setColor(new Color(220, 175, 110));
        g.fillRoundRect(
                x + 3,
                y + 2,
                Math.max(1, w - 6),
                Math.max(1, h / 3),
                8,
                8);

        g.setColor(new Color(67, 46, 32));
        g.drawRoundRect(x, y, w, h, 10, 10);
    }

    // ===== ACCESSEURS =====

    public ConfigurationCarte getConfiguration() {
        return carte;
    }

    public GestionnairePlateforme getGestionnairePlateformes() {
        return carte.getPlateformes();
    }

    public boolean spritePlateformesChateauCharge() {
        return spriteChateauHaut != null
                && spriteChateauMilieu != null;
    }

    public boolean spritePlateformesMarcheCharge() {
        return spriteMarcheGauche != null
                && spriteMarcheDroite != null
                && spriteMarchePrincipale != null;
    }

    public boolean spritePlateformesPotagerCharge() {
        return spritePotagerPetite != null
                && spritePotagerGrande != null;
    }

    public boolean spritePlateformesColonieCharge() {
        return spriteColoniePetite != null
                && spriteColonieGrande != null;
    }

    public boolean spriteColonieHolographiqueCharge() {
        return spriteColonieHolographique != null;
    }

    public void setAfficherHitboxes(boolean afficher) {
        afficherHitboxes = afficher;
        repaint();
    }
}
