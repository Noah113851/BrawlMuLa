
package brawlmula;

import java.awt.Color;
import java.awt.Dimension;
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

public final class PanneauCombat extends JPanel {

    // Images des deux types de plateformes du Chateau
    public static final String SPRITE_CHATEAU_HAUT =
            "/ressources/plateformes/plateforme_chateau_1.png";

    public static final String SPRITE_CHATEAU_MILIEU =
            "/ressources/plateformes/plateforme_chateau_2.png";

    private final ConfigurationCarte carte;
    private final BufferedImage decor;

    private final BufferedImage spriteChateauHaut;
    private final BufferedImage spriteChateauMilieu;

    // Associe chaque plateforme a son image
    private final Map<Plateforme, BufferedImage> imagesPlateformes =
            new IdentityHashMap<>();

    private final Timer timer;
    private long precedentNanos;
    private boolean afficherHitboxes = false;

    public PanneauCombat(String cheminCarte) {

        // Chargement de la carte selectionnee
        this.carte = ConfigurationCarte.depuisChemin(cheminCarte);
        this.decor = lireImageObligatoire(carte.getCheminDecor());

        // Chargement des sprites du Chateau
        if ("CHATEAU".equals(carte.getNom())) {

            spriteChateauHaut =
                    lireSpriteFacultatif(SPRITE_CHATEAU_HAUT);

            spriteChateauMilieu =
                    lireSpriteFacultatif(SPRITE_CHATEAU_MILIEU);

            attribuerImagesChateau();

        } else {
            spriteChateauHaut = null;
            spriteChateauMilieu = null;
        }

        setPreferredSize(new Dimension(
                ConfigurationCarte.LARGEUR,
                ConfigurationCarte.HAUTEUR
        ));

        setDoubleBuffered(true);
        setBackground(Color.BLACK);

        // Actualisation des plateformes toutes les 16 ms
        timer = new Timer(16, e -> animer());
        timer.setCoalesce(true);
    }

    // Charge le decor obligatoire
    private BufferedImage lireImageObligatoire(String chemin) {

        URL url = getClass().getResource(chemin);

        if (url == null) {
            throw new IllegalStateException(
                    "Decor introuvable : " + chemin
            );
        }

        try {
            BufferedImage image = ImageIO.read(url);

            if (image == null) {
                throw new IOException("Image invalide");
            }

            return image;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Erreur de chargement : " + chemin, e
            );
        }
    }

    // Charge un sprite PNG avec fond transparent
    private BufferedImage lireSpriteFacultatif(String chemin) {

        URL url = getClass().getResource(chemin);

        if (url == null) {
            System.err.println(
                    "Sprite introuvable : " + chemin
            );
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(url);

            if (image == null) {
                throw new IOException("Image invalide");
            }

            // Suppression des marges transparentes
            int gauche = image.getWidth();
            int droite = -1;
            int haut = image.getHeight();
            int bas = -1;

            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {

                    int alpha = image.getRGB(x, y) >>> 24;

                    if (alpha > 4) {
                        gauche = Math.min(gauche, x);
                        droite = Math.max(droite, x);
                        haut = Math.min(haut, y);
                        bas = Math.max(bas, y);
                    }
                }
            }

            if (droite < gauche) {
                throw new IOException("PNG transparent ou vide");
            }

            BufferedImage sprite = image.getSubimage(
                    gauche,
                    haut,
                    droite - gauche + 1,
                    bas - haut + 1
            );

            System.out.println("Sprite charge : " + chemin);

            return sprite;

        } catch (IOException e) {
            System.err.println(
                    "Erreur sprite : " + e.getMessage()
            );
            return null;
        }
    }

    // Attribution des images aux plateformes du Chateau
    private void attribuerImagesChateau() {

        List<Plateforme> aeriennes = new ArrayList<>();

        for (Plateforme p : carte.getPlateformes().getPlateformes()) {

            if (p.getType() == Plateforme.Type.TRAVERSABLE) {
                aeriennes.add(p);
            }
        }

        // Classement selon la hauteur initiale :
        // les deux plateformes les plus hautes en premier
        aeriennes.sort(
                Comparator.comparingDouble(Plateforme::getY)
        );

        for (int i = 0; i < aeriennes.size(); i++) {

            BufferedImage image;

            if (i < 2) {
                image = spriteChateauHaut;
            } else {
                image = spriteChateauMilieu;
            }

            imagesPlateformes.put(aeriennes.get(i), image);
        }
    }

    // Animation des plateformes mobiles
    private void animer() {

        long maintenant = System.nanoTime();

        double dt = (maintenant - precedentNanos)
                / 1_000_000_000.0;

        precedentNanos = maintenant;

        dt = Math.max(0.0, Math.min(dt, 0.05));

        carte.getPlateformes().miseAJour(dt);

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

    // Affichage du jeu
    @Override
    protected void paintComponent(Graphics graphismes) {

        super.paintComponent(graphismes);

        Graphics2D g = (Graphics2D) graphismes.create();

        try {

            // Adaptation a la taille de la fenetre
            g.scale(
                    getWidth() / (double) ConfigurationCarte.LARGEUR,
                    getHeight() / (double) ConfigurationCarte.HAUTEUR
            );

            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            // Affichage du decor
            g.drawImage(
                    decor,
                    0,
                    0,
                    ConfigurationCarte.LARGEUR,
                    ConfigurationCarte.HAUTEUR,
                    null
            );

            // Affichage des plateformes
            for (Plateforme p : carte.getPlateformes().getPlateformes()) {

                // Les plateformes solides sont deja dans le decor
                if (p.getType() != Plateforme.Type.TRAVERSABLE) {
                    continue;
                }

                int x = (int) Math.round(p.getX());
                int y = (int) Math.round(p.getY());

                int w = (int) Math.round(p.getLargeur());
                int h = (int) Math.round(p.getHauteur());

                BufferedImage sprite = imagesPlateformes.get(p);

                if (sprite != null) {

                    // Affichage du PNG correspondant
                    g.drawImage(sprite, x, y, w, h, null);

                } else {

                    // Affichage provisoire si aucun PNG
                    dessinerPlateformeProvisoire(g, x, y, w, h);
                }
            }

            // Affichage optionnel des collisions
            if (afficherHitboxes) {
                carte.getPlateformes().dessinerHitboxes(g);
            }

        } finally {
            g.dispose();
        }
    }

    // Dessin de secours
    private static void dessinerPlateformeProvisoire(
            Graphics2D g, int x, int y, int w, int h) {

        g.setColor(new Color(126, 84, 52));
        g.fillRoundRect(x, y, w, h, 10, 10);

        g.setColor(new Color(220, 175, 110));
        g.fillRoundRect(
                x + 3,
                y + 2,
                Math.max(1, w - 6),
                Math.max(1, h / 3),
                8,
                8
        );
    }

    public ConfigurationCarte getConfiguration() {
        return carte;
    }

    // Attention : GestionnairePlateforme SANS S
    public GestionnairePlateforme getGestionnairePlateformes() {
        return carte.getPlateformes();
    }

    public boolean spritePlateformesChateauCharge() {
        return spriteChateauHaut != null
                && spriteChateauMilieu != null;
    }

    public void setAfficherHitboxes(boolean afficher) {
        afficherHitboxes = afficher;
        repaint();
    }
}

