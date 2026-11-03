package brawlmula;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Affiche la carte retenue dans Selection et anime UNIQUEMENT ses plateformes.
 * Aucun mouvement d'avatar ni aucune gestion du clavier ici.
 *
 * Les positions restent dans le repere logique de 1280 x 760 pixels.
 */
public final class PanneauCombat extends JPanel {

    // Le PNG doit contenir UNE plateforme sur fond transparent, pas quatre
    // plateformes collees dans une seule image. S'il a un autre nom, changer
    // cette constante ou renommer le fichier dans NetBeans.
    public static final String SPRITE_CHATEAU = "/ressources/plateformes_chateau.png";

    private final ConfigurationCarte carte;
    private final BufferedImage decor;
    private final BufferedImage spriteChateau; // null : dessin de secours
    private final Timer timer;
    private long precedentNanos;
    private boolean afficherHitboxes;

    public PanneauCombat(String cheminCarte) {
        this.carte = ConfigurationCarte.depuisChemin(cheminCarte);
        this.decor = lireImageObligatoire(carte.getCheminDecor());

        // Pour les autres cartes, garder provisoirement les plateformes dessinees.
        if ("CHATEAU".equals(carte.getNom())) {
            this.spriteChateau = lireSpriteFacultatif(SPRITE_CHATEAU);
        } else {
            this.spriteChateau = null;
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
            throw new IllegalStateException("Decor introuvable : " + chemin
                    + " (verifier BrawlMuLa/src/ressources/cartes/)");
        }
        try {
            BufferedImage image = ImageIO.read(url);
            if (image == null) {
                throw new IllegalStateException("Format du decor invalide : " + chemin);
            }
            return image;
        } catch (IOException ex) {
            throw new IllegalStateException("Lecture impossible : " + chemin, ex);
        }
    }

    private BufferedImage lireSpriteFacultatif(String chemin) {
        URL url = getClass().getResource(chemin);
        if (url == null) {
            System.err.println("[BrawlMuLa] PNG des plateformes introuvable : " + chemin
                    + ". Dessin provisoire active. Renommer le PNG dans src/ressources/.");
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(url);
            if (image == null) {
                throw new IOException("Image PNG non reconnue");
            }
            // Enleve les marges transparentes, souvent presentes dans les PNG.
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
            if (droite < gauche) {
                throw new IOException("PNG transparent/vide");
            }
            return image.getSubimage(gauche, haut,
                                     droite - gauche + 1, bas - haut + 1);
        } catch (IOException e) {
            System.err.println("[BrawlMuLa] PNG inutilisable : " + chemin
                    + " (" + e.getMessage() + "). Dessin provisoire active.");
            return null;
        }
    }

    private void animer() {
        long maintenant = System.nanoTime();
        double dt = (maintenant - precedentNanos) / 1_000_000_000.0;
        precedentNanos = maintenant;
        // Blocage momentane de la fenetre : eviter un bond important.
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
            // Meme repere physique, meme si l'utilisateur redimensionne la fenetre.
            g.scale(getWidth() / (double) ConfigurationCarte.LARGEUR,
                    getHeight() / (double) ConfigurationCarte.HAUTEUR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(decor, 0, 0,
                        ConfigurationCarte.LARGEUR,
                        ConfigurationCarte.HAUTEUR, null);

            for (Plateforme p : carte.getPlateformes().getPlateformes()) {
                // La grande plateforme SOLIDE du Chateau existe deja sur le decor.
                if (p.getType() != Plateforme.Type.TRAVERSABLE) {
                    continue;
                }
                int x = (int) Math.round(p.getX());
                int y = (int) Math.round(p.getY());
                int w = (int) Math.round(p.getLargeur());
                int h = (int) Math.round(p.getHauteur());
                if (spriteChateau != null) {
                    g.drawImage(spriteChateau, x, y, w, h, null);
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
        g.fillRoundRect(x + 3, y + 2,
                        Math.max(1, w - 6), Math.max(1, h / 3), 8, 8);
        g.setColor(new Color(67, 46, 32));
        g.drawRoundRect(x, y, w, h, 10, 10);
    }

    public ConfigurationCarte getConfiguration() { return carte; }
    public GestionnairePlateforme getGestionnairePlateformes() {
        return carte.getPlateformes();
    }
    public boolean spritePlateformesChateauCharge() { return spriteChateau != null; }
    public void setAfficherHitboxes(boolean afficher) {
        afficherHitboxes = afficher;
        repaint();
    }
}

