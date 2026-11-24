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
import java.awt.Rectangle;


/**
 * Personnage : touches, affichage, gravite, sauts et collisions. Compatible
 * avec les plateformes intermittentes de la Colonie.
 */
public class Avatar {

    private static final Map<String, BufferedImage> SPRITES = new ConcurrentHashMap<>();
    private final BufferedImage sprite;
    private static final Map<String, BufferedImage> SPRITES_ATTAQUE
            = new ConcurrentHashMap<>();

    private final BufferedImage spriteAttaque;
    protected double x, y;
    private boolean gauche, droite, haut, bas;
    private boolean regardeGauche = false;

    private double vy;
    private double reculX = 0;
    private boolean auSol;
    private static final double SOL = 420;
    private static final double GRAVITE = 15;
    private static final double VITESSE_SAUT = 70;
    static final int LARGEUR_AVATAR = 125;
    static final int HAUTEUR_AVATAR = 125;
    static final int LIMITE_CHUTE = ConfigurationCarte.HAUTEUR + 150;
    private static final int LARGEUR_HITBOX = 90;
    private static final int HAUTEUR_HITBOX = 110;
    private static final long DUREE_ATTAQUE = 300_000_000L;
    private static final long DELAI_ATTAQUE = 600_000_000L;

    private long debutAttaque = 0L;
    private long derniereAttaque = 0L;

// Attaque lourde : une spécialité différente pour chaque personnage
    private final AttaqueLourde attaqueLourde;
    private long debutAttaqueLourde = 0L;
    private long derniereAttaqueLourde = 0L;
    private static final double VITESSE_RUEE = 28;
    private static final double VITESSE_ECRASEMENT = 45;
    
    private boolean animationDistante = false;
    private long dernierMouvementDistant = 0L;
    private long dernierSautDistant = 0L;
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

        spriteAttaque = SPRITES_ATTAQUE.computeIfAbsent(
                personnage,
                Avatar::chargerSpriteAttaque
        );
        attaqueLourde = AttaqueLourde.pour(personnage);
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

    private static BufferedImage chargerSpriteAttaque(String personnage) {

        String chemin = "/ressources/avatars/"
                + personnage + "_attack.png";

        URL url = Avatar.class.getResource(chemin);

        if (url == null) {
            System.err.println("Image d'attaque introuvable : " + chemin);
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(url);

            if (image == null) {
                throw new IOException("Image d'attaque invalide");
            }

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

            return sprite;

        } catch (IOException e) {
            System.err.println(
                    "Erreur chargement attaque : " + e.getMessage()
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
        
        if (reculX != 0) {
            x += reculX;
            reculX *= 0.85;
            if (Math.abs(reculX) < 0.5) reculX = 0;
            if (x < 0) x = 0;
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
            aChute = true;          // signale la chute à Jeu
            x = 600;
            y = 400;
            vy = 0;
            auSol = true;
            support = null;
            plateformeEnCoursDeTraverse = null;
                }
    }

// Déclencher une attaque avec un délai entre deux coups
    public boolean attaquer() {
        long maintenant = System.nanoTime();

        if (derniereAttaque != 0L
                && maintenant - derniereAttaque < DELAI_ATTAQUE) {
            return false;
        }
                // Pas d'attaque légère pendant une attaque lourde
            if (attaqueLourdeEnCours(maintenant)) {
                return false;
        }

        debutAttaque = maintenant;
        derniereAttaque = maintenant;
        return true;
    }

// Afficher l'attaque d'un joueur distant
    public void afficherAttaqueDistante() {
        debutAttaque = System.nanoTime();
    }
// Déclencher l'attaque lourde (spécialité du personnage)
    public boolean attaquerLourd() {
        long maintenant = System.nanoTime();

        if (derniereAttaqueLourde != 0L
                && maintenant - derniereAttaqueLourde < attaqueLourde.getDelai()) {
            return false;
        }

        // Pas d'attaque lourde pendant une attaque légère
        if (debutAttaque != 0L && maintenant - debutAttaque < DUREE_ATTAQUE) {
            return false;
        }

        debutAttaqueLourde = maintenant;
        derniereAttaqueLourde = maintenant;

        double direction = regardeGauche ? -1.0 : 1.0;
        switch (attaqueLourde) {
            case NIKKU:
                // Ruée : le poireau se propulse vers l'avant
                reculX = direction * VITESSE_RUEE;
                break;
            case PATATE:
                // Écrasement : en l'air, la patate plonge vers le sol
                if (!auSol) {
                    vy = Math.max(vy, VITESSE_ECRASEMENT);
                }
                break;
            default:
                break;
        }
        return true;
    }

// Afficher l'attaque lourde d'un joueur distant
    public void afficherAttaqueLourdeDistante() {
        debutAttaqueLourde = System.nanoTime();
    }

    private boolean attaqueLourdeEnCours(long maintenant) {
        return debutAttaqueLourde != 0L
                && maintenant - debutAttaqueLourde < AttaqueLourde.DUREE;
    }

    public AttaqueLourde getAttaqueLourde() {
        return attaqueLourde;
    }
// Déduire les animations des déplacements réseau
    public void actualiserAnimationDistante(double dx, double dy) {
        animationDistante = true;

        long maintenant = System.nanoTime();

        if (Math.abs(dx) > 0.5) {
            dernierMouvementDistant = maintenant;
            regardeGauche = dx < 0;
        }

        if (Math.abs(dy) > 1.0) {
            dernierSautDistant = maintenant;
        }
    }

    public void rendu(Graphics2D contexte) {

        if (sprite == null) {
            return;
        }

        long maintenant = System.nanoTime();

        boolean attaqueLourdeActive = attaqueLourdeEnCours(maintenant);
        boolean attaque = !attaqueLourdeActive
                && debutAttaque != 0L
                && maintenant - debutAttaque < DUREE_ATTAQUE;

        // Effet visuel de l'attaque lourde, derrière le personnage
        if (attaqueLourdeActive) {
            dessinerEffetAttaqueLourde(contexte, maintenant);
        }

        // Choisir l'image suivant l'état du personnage
        BufferedImage imageAffichee
                = (attaque || attaqueLourdeActive) && spriteAttaque != null
                        ? spriteAttaque
                        : sprite;

        boolean saute = animationDistante
                ? maintenant - dernierSautDistant < 160_000_000L
                : !auSol;

        boolean marche = animationDistante
                ? maintenant - dernierMouvementDistant < 160_000_000L
                : (gauche != droite) && auSol;

        double temps = maintenant / 1_000_000_000.0;
        double oscillation = Math.sin(temps * 18.0);

        double echelleX = 1.0;
        double echelleY = 1.0;
        double inclinaison = 0.0;
        double avance = 0.0;
        double rebond = 0.0;

        if (attaqueLourdeActive) {

            double progression
                    = (maintenant - debutAttaqueLourde)
                    / (double) AttaqueLourde.DUREE;

            double mouvement = Math.sin(Math.PI * progression);

            // Mouvement plus ample que l'attaque légère
            switch (attaqueLourde) {
                case PATATE:
                    echelleX = 1.0 + 0.18 * mouvement;
                    echelleY = 1.0 - 0.12 * mouvement;
                    break;
                case TOMATE:
                    echelleX = 1.0 + 0.12 * mouvement;
                    echelleY = 1.0 + 0.12 * mouvement;
                    break;
                case NIKKU:
                    echelleX = 1.0 + 0.10 * mouvement;
                    inclinaison = 0.25 * mouvement;
                    break;
                default:
                    echelleX = 1.0 + 0.12 * mouvement;
                    avance = 20.0 * mouvement;
                    break;
            }

        } else if (attaque) {

            double progression
                    = (maintenant - debutAttaque)
                    / (double) DUREE_ATTAQUE;

            double mouvement = Math.sin(
                    Math.PI * progression
            );

            // Petit mouvement vers l'avant pendant l'attaque
            echelleX = 1.0 + 0.06 * mouvement;
            avance = 8.0 * mouvement;

        } else if (saute) {

            echelleX = 0.94;
            echelleY = 1.09;

        } else if (marche) {

            echelleX = 1.0 - 0.02 * oscillation;
            echelleY = 1.0 + 0.03 * oscillation;

            inclinaison = 0.055 * oscillation;
            rebond = 2.0 * Math.abs(oscillation);

        } else {

            echelleY = 1.0
                    + 0.008 * Math.sin(temps * 4.0);
        }
        Graphics2D g = (Graphics2D) contexte.create();

        try {

            double direction = regardeGauche ? -1.0 : 1.0;

            // Garder les pieds du personnage au même endroit
            g.translate(
                    x + LARGEUR_AVATAR / 2.0
                    + direction * avance,
                    y + HAUTEUR_AVATAR - rebond
            );

            // Retourner automatiquement l'image vers la gauche
            g.scale(direction * echelleX, echelleY);

            g.rotate(inclinaison);

            // Afficher l'image normale ou l'image d'attaque
            g.drawImage(
                    imageAffichee,
                    -LARGEUR_AVATAR / 2,
                    -HAUTEUR_AVATAR,
                    null
            );

        } finally {
            g.dispose();
        }

        // Aura rouge pulsante : visible jusqu'a la consommation du bonus x2.
        if (doubleDegatsActif) {
            Graphics2D aura = (Graphics2D) contexte.create();
            try {
                aura.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                double pulsation = 0.5 + 0.5 * Math.sin(temps * 6.0);
                int cx = (int) Math.round(x + LARGEUR_AVATAR / 2.0);
                int cy = (int) Math.round(y + HAUTEUR_AVATAR / 2.0);
                // Plusieurs contours translucides donnent un halo lumineux.
                for (int i = 12; i >= 0; i--) {
                    int marge = 3 + i * 2;
                    int opacite = (int) ((75 - i * 5) * (0.65 + 0.35 * pulsation));
                    aura.setColor(new java.awt.Color(255, 18, 30, opacite));
                    aura.setStroke(new java.awt.BasicStroke(3.5f));
                    aura.drawOval((int) x - marge, (int) y - marge,
                            LARGEUR_AVATAR + 2 * marge, HAUTEUR_AVATAR + 2 * marge);
                }
                // Lueur rouge douce, sans masquer le sprite.
                aura.setColor(new java.awt.Color(255, 0, 20, (int) (16 + 14 * pulsation)));
                aura.fillOval((int) x - 5, (int) y - 5,
                        LARGEUR_AVATAR + 10, HAUTEUR_AVATAR + 10);
                // Etincelles rouges qui remontent autour du combattant.
                for (int i = 0; i < 16; i++) {
                    double phase = (temps * 0.9 + i / 16.0) % 1.0;
                    double angle = i * 2.399963 + temps * 0.7;
                    int px = cx + (int) (Math.cos(angle) * (LARGEUR_AVATAR / 2.0 + 10));
                    int py = cy + (int) ((0.5 - phase) * (HAUTEUR_AVATAR + 32));
                    int alpha = (int) (210 * Math.sin(Math.PI * phase));
                    int taille = 3 + i % 4;
                    aura.setColor(new java.awt.Color(255, 45 + i % 3 * 25, 35, alpha));
                    aura.fillOval(px, py, taille, taille);
                }
                // Indicateur du bonus, conserve au-dessus du personnage.
                aura.setColor(new java.awt.Color(85, 5, 12, 225));
                aura.fillRoundRect(cx - 21, (int) y - 23, 42, 23, 10, 10);
                aura.setColor(new java.awt.Color(255, 110, 105));
                aura.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 18));
                aura.drawString("x2", cx - 12, (int) y - 6);
            } finally {
                aura.dispose();
            }
        }

        // Afficher la bulle de bouclier par-dessus le personnage
        if (bouclierActif) {
            Graphics2D gBouclier = (Graphics2D) contexte.create();
            try {
                gBouclier.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );
                gBouclier.setColor(new java.awt.Color(0, 200, 255, 80));
                gBouclier.fillOval((int) x + 5, (int) y, LARGEUR_AVATAR - 10, HAUTEUR_AVATAR);

                gBouclier.setStroke(new java.awt.BasicStroke(3f));
                gBouclier.setColor(java.awt.Color.CYAN);
                gBouclier.drawOval((int) x + 5, (int) y, LARGEUR_AVATAR - 10, HAUTEUR_AVATAR);
            } finally {
                gBouclier.dispose();
            }
        }
    }

    private boolean aChute = false;

    /** Vrai une seule fois après une chute dans le vide. */
    public boolean consommerChute() {
        boolean r = aChute;
        aChute = false;
        return r;
    }

    /** Réapparition propre : l'avatar tombe de quelques pixels sur la plateforme. */
    public void reapparaitreSur(double nx, double ny) {
        x = nx;
        y = ny;
        vy = 0;
        auSol = false;     // on laisse la gravité le poser normalement
        support = null;
        plateformeEnCoursDeTraverse = null;
    }

    /** Zone occupée par le personnage. */
    public Rectangle getHitbox() {
        return new Rectangle((int) x + DECALAGE_HITBOX_X,
                (int) y + DECALAGE_HITBOX_Y, LARGEUR_HITBOX, HAUTEUR_HITBOX);
    }

    /** Zone touchée par un coup : devant le personnage. */
    public Rectangle getZoneAttaque() {
        int portee = 80;
        int haut = (int) y + DECALAGE_HITBOX_Y;
        if (regardeGauche) {
            return new Rectangle((int) x + DECALAGE_HITBOX_X - portee, haut,
                    portee + LARGEUR_HITBOX / 2, HAUTEUR_HITBOX);
        }
        return new Rectangle((int) x + DECALAGE_HITBOX_X + LARGEUR_HITBOX / 2, haut,
                portee + LARGEUR_HITBOX / 2, HAUTEUR_HITBOX);
    }
    /** Dessine la zone touchée par l'attaque lourde (couleur du personnage). */
    private void dessinerEffetAttaqueLourde(Graphics2D contexte, long maintenant) {
        double progression = (maintenant - debutAttaqueLourde)
                / (double) AttaqueLourde.DUREE;
        int alpha = (int) (140 * (1.0 - progression));
        if (alpha <= 0) {
            return;
        }

        java.awt.Color base = attaqueLourde.getCouleur();
        java.awt.Color couleur = new java.awt.Color(
                base.getRed(), base.getGreen(), base.getBlue(), alpha);
        Rectangle zone = getZoneAttaqueLourde();

        Graphics2D g = (Graphics2D) contexte.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(couleur);
            g.fillRoundRect(zone.x, zone.y, zone.width, zone.height, 40, 40);
            g.setStroke(new java.awt.BasicStroke(3f));
            g.setColor(base.darker());
            g.drawRoundRect(zone.x, zone.y, zone.width, zone.height, 40, 40);
        } finally {
            g.dispose();
        }
    }

    /** Zone touchée par l'attaque lourde, différente selon le personnage. */
    public Rectangle getZoneAttaqueLourde() {
        int hitX = (int) x + DECALAGE_HITBOX_X;
        int hitY = (int) y + DECALAGE_HITBOX_Y;
        int portee;
        int haut = hitY;
        int hauteur = HAUTEUR_HITBOX;

        switch (attaqueLourde) {
            case TOMATE:
                // Éclaboussure des deux côtés du personnage
                portee = 110;
                return new Rectangle(hitX - portee, hitY - 20,
                        LARGEUR_HITBOX + 2 * portee, HAUTEUR_HITBOX + 20);
            case NIKKU:
                // Toute la longueur de la ruée
                portee = 200;
                break;
            case PATATE:
                // Coup court mais large en hauteur (onde de choc au sol)
                portee = 70;
                haut = hitY - 30;
                hauteur = HAUTEUR_HITBOX + 60;
                break;
            default:
                // Perforation : très longue portée
                portee = 170;
                haut = hitY + HAUTEUR_HITBOX / 4;
                hauteur = HAUTEUR_HITBOX / 2;
                break;
        }

        if (regardeGauche) {
            return new Rectangle(hitX - portee, haut,
                    portee + LARGEUR_HITBOX / 2, hauteur);
        }
        return new Rectangle(hitX + LARGEUR_HITBOX / 2, haut,
                portee + LARGEUR_HITBOX / 2, hauteur);
    }
        private boolean doubleDegatsActif = false;

    public void activerDoubleDegats() {
        this.doubleDegatsActif = true;
    }

    public boolean aDoubleDegats() {
        return doubleDegatsActif;
    }

    public void consommerDoubleDegats() {
        this.doubleDegatsActif = false;
    }

    public void setDoubleDegatsActif(boolean etat) {
        this.doubleDegatsActif = etat;
    }
    private boolean bouclierActif = false;

    public void activerBouclier() {
        this.bouclierActif = true;
    }

    public boolean hasBouclier() {
        return bouclierActif;
    }

    public void setBouclierActif(boolean etat) {
        this.bouclierActif = etat;
    }

    public boolean absorberAttaqueAvecBouclier() {
        if (bouclierActif) {
            bouclierActif = false;
            return true;
        }
        return false;
    }
    
    /** Pousse l'avatar : direction = 1 (vers la droite) ou -1 (vers la gauche). */
    public void recevoirRecul(double direction) {
        reculX = direction * 12;   // force horizontale
        vy = -12;                  // petit saut
        auSol = false;
        support = null;
        plateformeEnCoursDeTraverse = null;
        System.out.println("RECUL reçu : " + direction);
    }
}