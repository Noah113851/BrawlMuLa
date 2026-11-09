package brawlmula;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Personnage : touches, affichage, gravite, sauts et collisions. Compatible
 * avec les plateformes intermittentes de la Colonie.
 */
public class Avatar {

    private static final Map<String, BufferedImage> SPRITES = new ConcurrentHashMap<>();
    private final BufferedImage sprite;
    protected double x, y;
    private boolean gauche, droite, haut, bas;
    private boolean regardeGauche = false;

    private double vy;
    private boolean auSol;
    private static final double SOL = 420;
    private static final double GRAVITE = 15;
    private static final double VITESSE_SAUT = 70;
    static final int LARGEUR_AVATAR = 125;
    static final int HAUTEUR_AVATAR = 125;
    static final int LIMITE_CHUTE = ConfigurationCarte.HAUTEUR + 150;
    private static final int LARGEUR_HITBOX = 90;
    private static final int HAUTEUR_HITBOX = 110;

    private static final int DECALAGE_HITBOX_X
            = (LARGEUR_AVATAR - LARGEUR_HITBOX) / 2;

    private static final int DECALAGE_HITBOX_Y
            = HAUTEUR_AVATAR - HAUTEUR_HITBOX;

    private GestionnairePlateforme gestionnaire;
    private Plateforme support;
    private double dernierSupportX;

    private Plateforme plateformeEnCoursDeTraverse;
    private static final double MARGE_TRAVERSEE = 8.0;

    public Avatar() {
        this("Carrot");
    }

    public Avatar(String nomPersonnage) {
        String nom = nomPersonnage == null ? "carrot"
                : nomPersonnage.trim().toLowerCase(Locale.ROOT);
        String personnage = switch (nom) {
            case "nikku", "tomato", "potato" ->
                nom;
            default ->
                "carrot";
        };
        BufferedImage image = SPRITES.computeIfAbsent(personnage, Avatar::chargerSpritePersonnage);
        sprite = image == null && !"carrot".equals(personnage)
                ? SPRITES.computeIfAbsent("carrot", Avatar::chargerSpritePersonnage) : image;
        this.x = 100;
        this.y = SOL;
        this.vy = 0;
        this.auSol = true;
        this.gauche = false;
        this.droite = false;
        this.bas = false;
    }

    /**
     * Redimensionne le sprite une fois au chargement pour la boucle de rendu.
     */
    private static BufferedImage chargerSpritePersonnage(String personnage) {

        String chemin = "/ressources/avatars/"
                + personnage + "_fight.png";

        URL url = Avatar.class.getResource(chemin);

        if (url == null) {
            System.err.println(
                    "[BrawlMuLa] Sprite introuvable : " + chemin);
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(url);

            if (image == null) {
                throw new IOException("Format d'image invalide");
            }

            // Garder la taille actuelle des personnages
            BufferedImage sprite = new BufferedImage(
                    LARGEUR_AVATAR,
                    HAUTEUR_AVATAR,
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g = sprite.createGraphics();

            try {
                g.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR
                );

                g.drawImage(
                        image,
                        0,
                        0,
                        LARGEUR_AVATAR,
                        HAUTEUR_AVATAR,
                        null
                );

            } finally {
                g.dispose();
            }

            // Chercher le dernier pixel visible du personnage
            int dernierPixelVisible = -1;

            for (int y = 0; y < HAUTEUR_AVATAR; y++) {
                for (int x = 0; x < LARGEUR_AVATAR; x++) {

                    int pixel = sprite.getRGB(x, y);
                    int alpha = (pixel >>> 24) & 0xFF;

                    if (alpha > 20) {
                        dernierPixelVisible = y;
                    }
                }
            }

            // Calculer la marge transparente sous les pieds
            int decalageY = 0;

            if (dernierPixelVisible >= 0) {
                decalageY = HAUTEUR_AVATAR
                        - 1 - dernierPixelVisible;
            }

            // Repositionner le personnage vers le bas
            BufferedImage spriteAligne = new BufferedImage(
                    LARGEUR_AVATAR,
                    HAUTEUR_AVATAR,
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g2 = spriteAligne.createGraphics();

            try {
                g2.drawImage(
                        sprite,
                        0,
                        decalageY,
                        null
                );
            } finally {
                g2.dispose();
            }

            System.out.println(
                    "[BrawlMuLa] Alignement de "
                    + personnage
                    + " : " + decalageY + " pixels"
            );

            return spriteAligne;

        } catch (IOException e) {
            Logger.getLogger(Avatar.class.getName()).log(
                    Level.SEVERE,
                    "Impossible de charger le sprite : " + chemin,
                    e
            );
            return null;
        }
    }

    public void connecterPlateformes(GestionnairePlateforme gestionnaire) {
        if (gestionnaire == null) {
            throw new IllegalArgumentException("GestionnairePlateforme manquant");
        }
        this.gestionnaire = gestionnaire;
        this.support = null;
        this.dernierSupportX = 0;
        this.plateformeEnCoursDeTraverse = null;
        this.x = 600;
        this.y = 400;
        this.vy = 0;
        this.auSol = true;
    }

    public void setGauche(boolean gauche) {
        this.gauche = gauche;
    }

    public void setDroite(boolean droite) {
        this.droite = droite;
    }

    public void setBas(boolean bas) {
        this.bas = bas;
    }

    public void saute() {
        if (this.auSol) {
            this.vy = -VITESSE_SAUT;
            this.auSol = false;
            this.support = null;
            this.plateformeEnCoursDeTraverse = null;
        }
    }

    // Methodes appelees par Jeu.java (correction des erreurs NetBeans).
    public void gererToucheAppuyee(int codeTouche) {
        switch (codeTouche) {
            case KeyEvent.VK_Q:
                setGauche(true);
                break;
            case KeyEvent.VK_D:
                setDroite(true);
                break;
            case KeyEvent.VK_SPACE:
                saute();
                break;
            case KeyEvent.VK_S:
                setBas(true);
                break;
            default:
                break;
        }
    }

    public void gererToucheRelachee(int codeTouche) {
        switch (codeTouche) {
            case KeyEvent.VK_Q:
                setGauche(false);
                break;
            case KeyEvent.VK_D:
                setDroite(false);
                break;
            case KeyEvent.VK_S:
                setBas(false);
                break;
            default:
                break;
        }
    }

    public void miseAJour() {
        // Une plateforme temporaire disparue ne porte plus le joueur.
        if (support != null && !support.estActive()) {
            support = null;
            auSol = false;
            vy = Math.max(0, vy);
            plateformeEnCoursDeTraverse = null;
        }

        double ancienX = x;
        double ancienY = y;

        // Conserver la vitesse horizontale actuelle du projet.
        if (this.gauche) {
            x -= 5;
            regardeGauche = true;
        }
        if (this.droite) {
            x += 5;
            regardeGauche = false;
        }
        if (this.gauche) {
            x -= 5;
        }
        if (this.droite) {
            x += 5;
        }

        double largeurMonde = gestionnaire == null
                ? 960 : ConfigurationCarte.LARGEUR;
        if (x > largeurMonde - LARGEUR_AVATAR) {
            x = largeurMonde - LARGEUR_AVATAR;
        }
        if (x < 0) {
            x = 0;
        }

        // S permet de traverser les petites plateformes par le dessus.
        if (gestionnaire != null && bas && auSol && support != null
                && support.getType() == Plateforme.Type.TRAVERSABLE) {
            plateformeEnCoursDeTraverse = support;
            support = null;
            auSol = false;
            vy = Math.max(vy, 7.0);
        }

        if (this.bas && !this.auSol) {
            if (vy < 0) {
                vy = 0;
            }
            vy += 3;
        }

        if (gestionnaire == null) {
            // Comportement historique hors des cartes a plateformes.
            if (!this.auSol) {
                y += vy;
                vy += GRAVITE;
                if (y >= SOL) {
                    y = SOL;
                    vy = 0;
                    this.auSol = true;
                }
            }
            if (this.bas && !this.auSol) {
                vy += 2;
            }
            return;
        }

        Plateforme supportPrecedent = auSol ? support : null;
        if (supportPrecedent != null) {
            // Suivre la translation horizontale du support.
            x += supportPrecedent.getX() - dernierSupportX;
            x = Math.max(0,
                    Math.min(x, ConfigurationCarte.LARGEUR - LARGEUR_AVATAR));
        }

        double ySouhaite;
        if (auSol) {
            ySouhaite = supportPrecedent == null
                    ? y + 1
                    : supportPrecedent.getY() - HAUTEUR_AVATAR + 1;
        } else {
            ySouhaite = y + vy;
            vy += GRAVITE;
            if (bas) {
                vy += 2;
            }
        }

        if (plateformeEnCoursDeTraverse != null
                && ancienY + HAUTEUR_AVATAR
                > plateformeEnCoursDeTraverse.getY() + MARGE_TRAVERSEE) {
            plateformeEnCoursDeTraverse = null;
        }
        boolean ignorerTraversables = plateformeEnCoursDeTraverse != null;

        ResultatColision resultat = gestionnaire.resoudre(
                ancienX + DECALAGE_HITBOX_X,
                ancienY + DECALAGE_HITBOX_Y,
                x + DECALAGE_HITBOX_X,
                ySouhaite + DECALAGE_HITBOX_Y,
                LARGEUR_HITBOX,
                HAUTEUR_HITBOX,
                ignorerTraversables
        );

        x = resultat.getX() - DECALAGE_HITBOX_X;
        y = resultat.getY() - DECALAGE_HITBOX_Y;
        if (resultat.estAuSol()) {
            auSol = true;
            vy = 0;
            support = resultat.getSupport();
            plateformeEnCoursDeTraverse = null;
        } else if (supportPrecedent != null && supportPrecedent.estActive()
                && x + DECALAGE_HITBOX_X
                < supportPrecedent.getX() + supportPrecedent.getLargeur()
                && x + DECALAGE_HITBOX_X + LARGEUR_HITBOX
                > supportPrecedent.getX()) {
            // Conserver l'appui sur un ascenseur qui monte.
            y = supportPrecedent.getY() - HAUTEUR_AVATAR;
            vy = 0;
            auSol = true;
            support = supportPrecedent;
            plateformeEnCoursDeTraverse = null;
        } else {
            if (resultat.touchePlafond() && vy < 0) {
                vy = 0;
            }
            auSol = false;
            support = null;
        }
        if (support != null) {
            dernierSupportX = support.getX();
        }

        // Garde-fou de l'ancienne version (Jeu gere les cartes flottantes).
        if (y > LIMITE_CHUTE) {
            x = 600;
            y = 400;
            vy = 0;
            auSol = true;
            support = null;
            plateformeEnCoursDeTraverse = null;
        }
    }

    public void rendu(Graphics2D contexte) {
        if (sprite == null) {
            return;
        }
        if (regardeGauche) {
            contexte.drawImage(sprite, (int) x + LARGEUR_AVATAR, (int) y,
                    -LARGEUR_AVATAR, HAUTEUR_AVATAR, null);
        } else {
            contexte.drawImage(sprite, (int) x, (int) y, null);
        }
    }
}
