
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

/**
 * Decor et plateformes de la carte choisie. Aucune gestion de personnage.
 * Compatible avec le test Chateau et la nouvelle fenetre Marche.
 */
public final class PanneauCombat extends JPanel {

    public static final String SPRITE_CHATEAU_HAUT =
            "/ressources/plateformes/plateforme_chateau_1.png";
    public static final String SPRITE_CHATEAU_MILIEU =
            "/ressources/plateformes/plateforme_chateau_2.png";

    // Noms deja presents dans ton dossier ressources.plateformes de NetBeans.
    public static final String SPRITE_MARCHE_HAUT_GAUCHE =
            "/ressources/plateformes/plateforme_marche_haut_gauche.png";
    public static final String SPRITE_MARCHE_HAUT_DROITE =
            "/ressources/plateformes/plateforme_marche_haut_droite.png";
    public static final String SPRITE_MARCHE_PRINCIPALE =
            "/ressources/plateformes/plateforme_marche_principale.png";

    private final ConfigurationCarte carte;
    private final BufferedImage decor;
    private BufferedImage spriteChateauHaut;
    private BufferedImage spriteChateauMilieu;
    private BufferedImage spriteMarcheGauche;
    private BufferedImage spriteMarcheDroite;
    private BufferedImage spriteMarchePrincipale;

    // Association stable aux plateformes, meme si elles bougent.
    private final Map<Plateforme, BufferedImage> imagesPlateformes =
            new IdentityHashMap<>();

    private final Timer timer;
    private long precedentNanos;
    private boolean afficherHitboxes = false;

    public PanneauCombat(String cheminCarte) {
        carte = ConfigurationCarte.depuisChemin(cheminCarte);
        decor = lireImageObligatoire(carte.getCheminDecor());

        if ("CHATEAU".equals(carte.getNom())) {
            spriteChateauHaut = lireSpriteFacultatif(SPRITE_CHATEAU_HAUT);
            spriteChateauMilieu = lireSpriteFacultatif(SPRITE_CHATEAU_MILIEU);
            attribuerImagesChateau();
        } else if ("MARCHE".equals(carte.getNom())) {
            spriteMarcheGauche = lireSpriteFacultatif(SPRITE_MARCHE_HAUT_GAUCHE);
            spriteMarcheDroite = lireSpriteFacultatif(SPRITE_MARCHE_HAUT_DROITE);
            spriteMarchePrincipale = lireSpriteFacultatif(SPRITE_MARCHE_PRINCIPALE);
            attribuerImagesMarche();
        }

        setPreferredSize(new Dimension(ConfigurationCarte.LARGEUR,
                                       ConfigurationCarte.HAUTEUR));
        setDoubleBuffered(true);
        setBackground(Color.BLACK);
        timer = new Timer(16, e -> animer());
        timer.setCoalesce(true);
    }

    private BufferedImage lireImageObligatoire(String chemin) {
        URL url = getClass().getResource(chemin);
        if (url == null) {
            throw new IllegalStateException("Decor introuvable : " + chemin);
        }
        try {
            BufferedImage image = ImageIO.read(url);
            if (image == null) {
                throw new IOException("Image du decor invalide");
            }
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Erreur de lecture : " + chemin, e);
        }
    }

    /** Charge une image PNG et supprime ses marges transparentes. */
    private BufferedImage lireSpriteFacultatif(String chemin) {
        URL url = getClass().getResource(chemin);
        if (url == null) {
            System.err.println("[BrawlMuLa] Sprite non trouve : " + chemin
                    + " (dessin provisoire utilise)");
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(url);
            if (image == null) throw new IOException("Image invalide");
            int gauche = image.getWidth(), droite = -1;
            int haut = image.getHeight(), bas = -1;
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
            if (droite < gauche) throw new IOException("Image transparente/vide");
            System.out.println("Sprite charge : " + chemin);
            return image.getSubimage(gauche, haut,
                    droite - gauche + 1, bas - haut + 1);
        } catch (IOException e) {
            System.err.println("[BrawlMuLa] Sprite inutilisable : " + chemin
                    + " (" + e.getMessage() + ")");
            return null;
        }
    }

    private void attribuerImagesChateau() {
        List<Plateforme> aeriennes = new ArrayList<>();
        for (Plateforme p : carte.getPlateformes().getPlateformes()) {
            if (p.getType() == Plateforme.Type.TRAVERSABLE) aeriennes.add(p);
        }
        aeriennes.sort(Comparator.comparingDouble(Plateforme::getY));
        for (int i = 0; i < aeriennes.size(); i++) {
            imagesPlateformes.put(aeriennes.get(i),
                    i < 2 ? spriteChateauHaut : spriteChateauMilieu);
        }
    }

    private void attribuerImagesMarche() {
        for (Plateforme p : carte.getPlateformes().getPlateformes()) {
            if (p.getType() != Plateforme.Type.TRAVERSABLE) continue;
            if (p.getLargeur() >= 400) {
                // Grande plateforme centrale (distincte des deux petites).
                imagesPlateformes.put(p, spriteMarchePrincipale);
            } else if (p.getX() < ConfigurationCarte.LARGEUR / 2) {
                imagesPlateformes.put(p, spriteMarcheGauche);
            } else {
                imagesPlateformes.put(p, spriteMarcheDroite);
            }
        }
    }

    private void animer() {
        long maintenant = System.nanoTime();
        double dt = (maintenant - precedentNanos) / 1_000_000_000.0;
        precedentNanos = maintenant;
        carte.getPlateformes().miseAJour(Math.max(0.0, Math.min(dt, 0.05)));
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

    @Override
    protected void paintComponent(Graphics graphismes) {
        super.paintComponent(graphismes);
        Graphics2D g = (Graphics2D) graphismes.create();
        try {
            g.scale(getWidth() / (double) ConfigurationCarte.LARGEUR,
                    getHeight() / (double) ConfigurationCarte.HAUTEUR);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(decor, 0, 0, ConfigurationCarte.LARGEUR,
                    ConfigurationCarte.HAUTEUR, null);

            for (Plateforme p : carte.getPlateformes().getPlateformes()) {
                // Le sol SOLIDE est deja visible dans le decor.
                if (p.getType() != Plateforme.Type.TRAVERSABLE) continue;
                int x = (int) Math.round(p.getX());
                int y = (int) Math.round(p.getY());
                int w = (int) Math.round(p.getLargeur());
                int h = (int) Math.round(p.getHauteur());
                BufferedImage sprite = imagesPlateformes.get(p);
                if (sprite != null) {
                    g.drawImage(sprite, x, y, w, h, null);
                } else {
                    dessinerPlateformeProvisoire(g, x, y, w, h);
                }
            }
            if (afficherHitboxes) {
                carte.getPlateformes().dessinerHitboxes(g);
            }
        } finally {
            g.dispose();
        }
    }

    private static void dessinerPlateformeProvisoire(Graphics2D g,
                                                     int x, int y, int w, int h) {
        g.setColor(new Color(0, 0, 0, 95));
        g.fillRoundRect(x + 4, y + 7, w, h, 12, 12);
        g.setColor(new Color(126, 84, 52));
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(new Color(220, 175, 110));
        g.fillRoundRect(x + 3, y + 2, Math.max(1, w - 6), Math.max(1, h / 3), 8, 8);
        g.setColor(new Color(67, 46, 32));
        g.drawRoundRect(x, y, w, h, 10, 10);
    }

    public ConfigurationCarte getConfiguration() { return carte; }

    // Orthographe du projet local : SANS "s" final.
    public GestionnairePlateforme getGestionnairePlateformes() {
        return carte.getPlateformes();
    }

    public boolean spritePlateformesChateauCharge() {
        return spriteChateauHaut != null && spriteChateauMilieu != null;
    }

    public boolean spritePlateformesMarcheCharge() {
        return spriteMarcheGauche != null && spriteMarcheDroite != null
                && spriteMarchePrincipale != null;
    }

    public void setAfficherHitboxes(boolean afficher) {
        afficherHitboxes = afficher;
        repaint();
    }
}
