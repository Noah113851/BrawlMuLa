package brawlmula;

import java.awt.Rectangle;
import java.awt.Graphics2D;
import static brawlmula.Avatar.HAUTEUR_AVATAR;
import static brawlmula.Avatar.LARGEUR_AVATAR;

/**
 * Physique de l'avatar local et rendu des combattants dans la boucle a 40 ms.
 */
public class Jeu {

    private static final double DISTANCE_TELEPORTATION = 180;

    public final Avatar avatar;
    private final PanneauCombat panneauCombat;
    private final Avatar[] avatars = new Avatar[4];
    private final boolean[] avatarsVisibles = new boolean[avatars.length];
    private final Reseau reseau;
    private final int indexLocal;
    private final JoueurSQL sql;
    private final ModeDeJeu mode;
    private long derniereChute = 0L;
    private long dernierSpawnVer = System.currentTimeMillis();
    private static final long DELAI_SPAWN_MS = 12000; // Spawn toutes les 12 secondes
    // Second bonus independant : environ toutes les 18 secondes.
    private static final long DELAI_PIMENT_MS = 18000;
    private long dernierSpawnPiment = System.currentTimeMillis();
    /**
     * Compatibilite : FenetreDeJeu() continue d'ouvrir le Chateau.
     */
    public Jeu() {
        this("/ressources/Maps/chateau.png");
    }

    /**
     * Une meme physique, mais un decor et des plateformes selon la carte.
     */
    public Jeu(String cheminCarte) {
        this("Carrot", null, null, null, cheminCarte, null, 1);
    }

    public Jeu(String j1, String j2, String j3, String j4,
            String cheminCarte, Reseau reseau, int monNumeroJoueur) {
        if (monNumeroJoueur < 1 || monNumeroJoueur > avatars.length) {
            throw new IllegalArgumentException("Numéro de joueur invalide : " + monNumeroJoueur);
        }
        this.reseau = reseau;
        indexLocal = monNumeroJoueur - 1;
        String[] personnages = {j1, j2, j3, j4};
        for (int index = 0; index < avatars.length; index++) {
            if (reseau != null || index == indexLocal) {
                avatars[index] = new Avatar(personnages[index]);
            }
        }
        avatarsVisibles[indexLocal] = true;
        panneauCombat = new PanneauCombat(cheminCarte, avatars, avatarsVisibles);
        panneauCombat.setSize(ConfigurationCarte.LARGEUR, ConfigurationCarte.HAUTEUR);
        panneauCombat.setAfficherHitboxes(false);
       sql = new JoueurSQL();
mode = ModeDeJeu.courant;
for (int i = 1; i <= mode.getNbJoueurs(); i++) {
    sql.initialiserPv(i);
}
panneauCombat.setHud(new BarreVieHUD(sql, mode));

        avatar = avatars[indexLocal];
        avatar.connecterPlateformes(panneauCombat.getGestionnairePlateformes());
        placerAvatarSurPlateformePrincipale();
        if (reseau != null) {
            reseau.demarrerSynchronisation(monNumeroJoueur, (int) avatar.x, (int) avatar.y);

            reseau.demarrerEcouteAttaques(numero -> {

                int index = numero - 1;

                if (index >= 0
                        && index < avatars.length
                        && index != indexLocal
                        && avatars[index] != null) {

                    avatars[index].afficherAttaqueDistante();
                }
            });
            reseau.demarrerEcouteVer(msg -> {
                String[] parts = msg.split(":");
                if (parts.length == 3 && "VER_SPAWN".equals(parts[0])) {
                    if (indexLocal != 0) {
                        int vx = Integer.parseInt(parts[1]);
                        int vy = Integer.parseInt(parts[2]);
                        panneauCombat.getVerDeTerre().synchroniser(
                                true, vx, vy, panneauCombat.getGestionnairePlateformes().getPlateformes());
                    }
                } else if (parts.length == 2 && "VER_PRIS".equals(parts[0])) {
                    if (indexLocal != 0) {
                        int idx = Integer.parseInt(parts[1]) - 1;
                        panneauCombat.getVerDeTerre().ramasser();
                        if (idx >= 0 && idx < avatars.length && avatars[idx] != null) {
                            avatars[idx].activerBouclier();
                        }
                    }
                } else if (parts.length == 3 && "VER_X2_SPAWN".equals(parts[0])) {
                    if (indexLocal != 0) {
                        try {
                            double bx = Double.parseDouble(parts[1]);
                            double by = Double.parseDouble(parts[2]);
                            panneauCombat.getBonusDoubleDegats().synchroniser(
                                    bx, by, panneauCombat.getGestionnairePlateformes().getPlateformes());
                        } catch (NumberFormatException ex) {
                            System.err.println("[BrawlMuLa][X2] Position invalide : " + msg);
                        }
                    }
                } else if (parts.length == 2 && "VER_X2_PRIS".equals(parts[0])) {
                    int idx = Integer.parseInt(parts[1]) - 1;
                    if (indexLocal != 0) {
                        panneauCombat.getBonusDoubleDegats().ramasser();
                        if (idx >= 0 && idx < avatars.length && avatars[idx] != null) {
                            avatars[idx].activerDoubleDegats();
                        }
                    }
                } else if (parts.length == 2 && "VER_X2_UTILISE".equals(parts[0])) {
                    int idx = Integer.parseInt(parts[1]) - 1;
                    if (idx >= 0 && idx < avatars.length && avatars[idx] != null) {
                        avatars[idx].setDoubleDegatsActif(false);
                    }
                } else if (parts.length == 2 && "VER_UTILISE".equals(parts[0])) {
                    int idx = Integer.parseInt(parts[1]) - 1;
                    if (idx >= 0 && idx < avatars.length && avatars[idx] != null) {
                        avatars[idx].setBouclierActif(false);
                    }
                }
            });
        }
    }

    /**
     * Apparition initiale sur le support principal.
     */
    private void placerAvatarSurPlateformePrincipale() {
        if (reseau != null || estCarteFlottante()) {
            Plateforme principale = chercherPlateformePrincipale();
            double fraction = reseau == null ? 0.5 : (indexLocal + 0.5) / avatars.length;
            avatar.x = principale.getX() + principale.getLargeur() * fraction - LARGEUR_AVATAR / 2.0;
            avatar.y = principale.getY() - HAUTEUR_AVATAR;
            return;
        }
        // Conserver l'apparition du Chateau et des autres cartes.
        double solLePlusHaut = Double.POSITIVE_INFINITY;
        double avatarX = avatar.x;
        for (Plateforme p : panneauCombat.getGestionnairePlateformes().getPlateformes()) {
            if (p.getType() == Plateforme.Type.SOLIDE
                    && avatarX + LARGEUR_AVATAR > p.getX()
                    && avatarX < p.getX() + p.getLargeur()) {
                solLePlusHaut = Math.min(solLePlusHaut, p.getY());
            }
        }
        if (Double.isFinite(solLePlusHaut)) {
            avatar.y = solLePlusHaut - HAUTEUR_AVATAR;
        }
    }

    /**
     * Le Marche, le Potager et la Colonie ont une plateforme principale
     * flottante.
     */
    private boolean estCarteFlottante() {
        String nom = panneauCombat.getConfiguration().getNom();
        return "MARCHE".equals(nom)
                || "POTAGER".equals(nom)
                || "COLONIE".equals(nom);
    }

    /**
     * Plus grande plateforme solide de la carte.
     */
    private Plateforme chercherPlateformePrincipale() {
        Plateforme principale = null;
        for (Plateforme p : panneauCombat.getGestionnairePlateformes().getPlateformes()) {
            if (p.getType() == Plateforme.Type.SOLIDE
                    && (principale == null || p.getLargeur() > principale.getLargeur())) {
                principale = p;
            }
        }
        if (principale == null) {
            throw new IllegalStateException(
                    "Aucune plateforme principale solide sur cette carte.");
        }
        return principale;
    }

    public void miseAJour() {
        // Un seul Timer pilote les plateformes et le personnage local (40 ms par tick).
        panneauCombat.getGestionnairePlateformes().miseAJour(0.040);
        panneauCombat.getGestionnairePlateformes().miseAJour(0.040);
        panneauCombat.getVerDeTerre().miseAJour();
        panneauCombat.getBonusDoubleDegats().miseAJour();
        avatar.miseAJour();

        // Sur les cartes flottantes, remettre l'avatar sur le plateau principal.
        if (avatar.consommerChute()) {
    long maintenant = System.currentTimeMillis();
    if (maintenant - derniereChute > 1500) {
        sql.retirerPv(indexLocal + 1, 15);
        derniereChute = maintenant;
    }
    Plateforme principale = chercherPlateformePrincipale();
    avatar.connecterPlateformes(panneauCombat.getGestionnairePlateformes());
    avatar.reapparaitreSur(
            principale.getX() + principale.getLargeur() / 2.0 - LARGEUR_AVATAR / 2.0,
            principale.getY() - HAUTEUR_AVATAR - 30);
}

        if (reseau != null) {
            reseau.envoyerMaPosition((int) avatar.x, (int) avatar.y);
            actualiserAvatarsDistants();
        }
        gererBonusDoubleDegats();
        VerDeTerre ver = panneauCombat.getVerDeTerre();
            if (reseau == null || indexLocal == 0) {
                // 1. L'hôte (J1) fait apparaître le ver toutes les 12s et envoie ses coordonnées
                if (!ver.isActif() && (System.currentTimeMillis() - dernierSpawnVer >= DELAI_SPAWN_MS)) {
                    ver.apparaitreAleatoirement(panneauCombat.getGestionnairePlateformes().getPlateformes());
                    dernierSpawnVer = System.currentTimeMillis();
                    if (reseau != null && ver.isActif()) {
                        reseau.envoyerEvenementVer("VER_SPAWN:"
                                + (int) Math.round(ver.getX()) + ":"
                                + (int) Math.round(ver.getY()));
                    }
                }

                // 2. L'hôte vérifie si un joueur ramasse le ver
                if (ver.isActif()) {
                    for (int i = 0; i < avatars.length; i++) {
                        Avatar joueur = avatars[i];
                        if (joueur != null && avatarsVisibles[i]) {
                            if (ver.getHitbox().intersects(joueur.getHitbox())) {
                                joueur.activerBouclier();
                                ver.ramasser();
                                dernierSpawnVer = System.currentTimeMillis();
                                if (reseau != null) {
                                    reseau.envoyerEvenementVer("VER_PRIS:" + (i + 1));
                                }
                                break;
                            }
                        }
                    }
                }
        }
    }

    /**
     * Le PC hote choisit le support et le joueur qui ramasse le piment.
     * Les clients appliquent uniquement les evenements diffuses par l'hote.
     */
    private void gererBonusDoubleDegats() {
        if (reseau != null && indexLocal != 0) return;
        BonusDoubleDegats bonus = panneauCombat.getBonusDoubleDegats();
        long maintenant = System.currentTimeMillis();
        if (!bonus.isActif() && maintenant - dernierSpawnPiment >= DELAI_PIMENT_MS) {
            bonus.apparaitreAleatoirement(
                    panneauCombat.getGestionnairePlateformes().getPlateformes());
            dernierSpawnPiment = maintenant;
            if (reseau != null && bonus.isActif()) {
                reseau.envoyerEvenementVer("VER_X2_SPAWN:"
                        + (int) Math.round(bonus.getX()) + ":"
                        + (int) Math.round(bonus.getY()));
            }
        }
        if (bonus.isActif()) {
            for (int i = 0; i < avatars.length; i++) {
                Avatar joueur = avatars[i];
                if (joueur != null && avatarsVisibles[i]
                        && bonus.getHitbox().intersects(joueur.getHitbox())) {
                    joueur.activerDoubleDegats();
                    bonus.ramasser();
                    dernierSpawnPiment = System.currentTimeMillis();
                    if (reseau != null) {
                        reseau.envoyerEvenementVer("VER_X2_PRIS:" + (i + 1));
                    }
                    break;
                }
            }
        }
    }

    private void actualiserAvatarsDistants() {

        for (Reseau.JoueurData etat
                : reseau.lireTousLesJoueurs()) {

            int index = etat.emplacement() - 1;

            if (index == indexLocal
                    || index < 0
                    || index >= avatars.length) {
                continue;
            }

            Avatar distant = avatars[index];

            if (distant == null) {
                continue;
            }

            if (!avatarsVisibles[index]) {

                distant.x = etat.x();
                distant.y = etat.y();

                distant.actualiserAnimationDistante(0, 0);

                avatarsVisibles[index] = true;
                continue;
            }

            double ancienX = distant.x;
            double ancienY = distant.y;

            double dx = etat.x() - distant.x;
            double dy = etat.y() - distant.y;

            double distance = Math.hypot(dx, dy);

            double fraction
                    = distance < 1
                    || distance >= DISTANCE_TELEPORTATION
                            ? 1.0 : 0.5;

            distant.x += fraction * dx;
            distant.y += fraction * dy;

            distant.actualiserAnimationDistante(
                    distant.x - ancienX,
                    distant.y - ancienY
            );
        }
    }

    public void rendu(Graphics2D contexte) {
        panneauCombat.paint(contexte);
    }

   public void fermer() {
    if (reseau != null) {
        reseau.fermer();
    }
    sql.closeTable();
}

    public PanneauCombat getPanneauCombat() {
        return panneauCombat;
    }

public void attaquer() {
        if (!avatar.attaquer()) return;

        // L'effet s'applique au prochain COUP REUSSI, pas au prochain clic.
        boolean bonusSurCeCoup = avatar.aDoubleDegats();
        boolean aInfligeDesDegats = false;
        int degatsBase = Math.max(0, sql.lireDegats(indexLocal + 1));

        // Les animations d'attaque continuent d'etre synchronisees.
        if (reseau != null) reseau.envoyerAttaque();
        Rectangle zone = avatar.getZoneAttaque();
        for (int i = 0; i < avatars.length; i++) {
            if (i == indexLocal || avatars[i] == null || !avatarsVisibles[i]) continue;
            if (mode.parEquipes() && mode.getEquipe(i) == mode.getEquipe(indexLocal)) {
                continue; // Ne jamais toucher un allie.
            }
            if (!zone.intersects(avatars[i].getHitbox())) continue;

            if (avatars[i].absorberAttaqueAvecBouclier()) {
                // Coup absorbe : bonus x2 conserve, aucun PV retire.
                if (reseau != null) {
                    reseau.envoyerEvenementVer("VER_UTILISE:" + (i + 1));
                }
            } else if (degatsBase > 0 && sql.lirePv(i + 1) > 0) {
                int montant = BonusDoubleDegats.calculerDegats(
                        degatsBase, bonusSurCeCoup);
                sql.retirerPv(i + 1, montant);
                aInfligeDesDegats = true;
            }
        }

        // Une seule charge consommee, meme si plusieurs ennemis ont ete touches.
        if (bonusSurCeCoup && aInfligeDesDegats) {
            avatar.consommerDoubleDegats();
            if (reseau != null) {
                reseau.envoyerEvenementVer("VER_X2_UTILISE:" + (indexLocal + 1));
            }
        }
    }

        public void gererToucheAppuyee(int codeTouche) {

        // Conserver uniquement les déplacements et le saut
        avatar.gererToucheAppuyee(codeTouche);
    }

    public void gererToucheRelachee(int codeTouche) {
        avatar.gererToucheRelachee(codeTouche);
    }
    
}


//package brawlmula;
//
//import java.awt.Color;
//import java.awt.Graphics2D;
//import java.awt.image.BufferedImage;
//import javax.imageio.ImageIO;
//
//public class Jeu {
//
//    public Avatar avatar;
//    private BufferedImage fond;
//
//    public Jeu(String nomPerso, String cheminCarte) {
//        try {
//            fond = ImageIO.read(getClass().getResource(cheminCarte));
//        } catch (Exception ex) {
//            System.err.println("Fond introuvable : " + cheminCarte);
//        }
//        avatar = new Avatar(nomPerso, 100);
//    }
//
//    public void miseAJour() {
//        avatar.miseAJour();
//    }
//
//    public void rendu(Graphics2D g) {
//        if (fond != null) {
//            g.drawImage(fond, 0, 0, 960, 540, null);
//        } else {
//            g.setColor(Color.BLACK);
//            g.fillRect(0, 0, 960, 540);
//        }
//        avatar.rendu(g);
//    }
//}