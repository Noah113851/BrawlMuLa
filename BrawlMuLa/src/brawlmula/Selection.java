package brawlmula;

import java.awt.Color;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class Selection extends javax.swing.JFrame {

    private static final String URL_BDD = "jdbc:mariadb://nemrod.ens2m.fr:3306/2026-2027_s1_vs2_tp2_brawlmula";
    private static final String USER_BDD = "etudiant";
    private static final String PASSWORD_BDD = "YTDTvj9TR3CDYCmP";

    private static final String[] PERSONNAGES_PAR_DEFAUT = {"Carrot", "Nikku", "Tomato", "Potato"};

    private final int monNumeroJoueur;
    private final boolean isHost;
    private final Reseau reseau;
    private final JLabel[] nomsJoueurs;
    private final JLabel[] portraitsJoueurs;
    private boolean partieLancee = false;

    // Vrais emplacements occupes dans la partie (J1 a J4).
    // J1 est l'hote. Les autres deviennent presents avec PRESENCE_P2..P4.
    private final boolean[] joueursPresents = {true, false, false, false};

    private final int[] choixJoueurs = {-1, -1, -1, -1};
    private int choixCarte = 0;
    private int choixCarteReseau = 0;

    private List<String> personnages = new ArrayList<>();
    private List<String> cartes = new ArrayList<>();
    private List<String> imagesPersonnages = new ArrayList<>();
    private List<String> imagesCartes = new ArrayList<>();

    public Selection(int monNumeroJoueur, Reseau reseau) {
        initComponents();
        BrawlMuLa.appliquerPoliceFairfax(getContentPane());
        setSize(1280, 760);
        setLocationRelativeTo(null);

        this.monNumeroJoueur = monNumeroJoueur;
        if (monNumeroJoueur >= 1 && monNumeroJoueur <= 4) {
            joueursPresents[monNumeroJoueur - 1] = true;
        }
        this.isHost = (monNumeroJoueur == 1);
        this.reseau = reseau;
        nomsJoueurs = new JLabel[]{lblNomJ1, lblNomJ2, lblNomJ3, lblNomJ4};
        portraitsJoueurs = new JLabel[]{lblAvatarJ1, lblAvatarJ2, lblAvatarJ3, lblAvatarJ4};

        chargerAvatarsDepuisBDD();
        chargerCartesDepuisBDD();

        afficherCarteLocal(choixCarte);
        if (!isHost) {
            boutton_droite.setEnabled(false);
            boutton_gauche.setEnabled(false);
            btnCombat.setEnabled(false);
        }

        if (reseau != null) {
            reseau.demarrerEcoute(this::traiterMessageReseau);
            choisirPersonnage(PERSONNAGES_PAR_DEFAUT[monNumeroJoueur - 1]);
        }

        Musiques_sons.playLoop("/ressources/Musiques-sons/Welcome-Music-Brawlmula-Soundtrack.wav");
    }

    public Selection() {
        this(1, null);
    }

    private void traiterMessageReseau(String msg) {
        // Les evenements de presence n'ont pas de ':' : les intercepter
        // AVANT la logique des choix de personnages et de START_GAME.
        if (msg != null && msg.matches("PRESENCE_P[1-4]")) {
            int numero = msg.charAt(msg.length() - 1) - '1';
            joueursPresents[numero] = true;
            System.out.println("[BrawlMuLa] Joueur J" + (numero + 1) + " present.");
            return;
        }
        if (msg == null) {
            return;
        }

        if (Reseau.DEBUT_COMBAT.equals(msg)) {
            ouvrirFenetreDeJeu();
            return;
        }
        int separateur = msg.indexOf(':');
        if (separateur < 0) {
            return;
        }
        try {
            int index = Integer.parseInt(msg.substring(separateur + 1));
            if (msg.startsWith("CHOIX_J") && separateur == 8) {
                int joueur = msg.charAt(7) - '1';
                if (joueur >= 0 && joueur < choixJoueurs.length && index >= 0 && index < personnages.size()) {
                    // Tous les postes utilisent les choix lus avant START_GAME, y compris les leurs.
                    choixJoueurs[joueur] = index;
                    joueursPresents[joueur] = true; // Choix recu = joueur bien connecte.
                    afficherPersonnage(joueur, index);
                }
            } else if (msg.startsWith("MAP:") && index >= 0 && index < cartes.size()) {
                choixCarteReseau = index;
                if (!isHost) {
                    afficherCarteLocal(index);
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Index de sélection invalide dans le salon.");
        }
    }

    /**
     * Au moment de START_GAME, consulte les vraies PRESENCE_P1..P4.
     * Cela corrige le cas ou Selection n'a pas recu un message de presence.
     * En cas d'erreur SQL, les choix et messages deja recus restent utilises.
     */
    private boolean[] joueursPresentsAuDepart() {
        boolean[] resultat = joueursPresents.clone();
        resultat[0] = true;
        if (monNumeroJoueur >= 1 && monNumeroJoueur <= 4) {
            resultat[monNumeroJoueur - 1] = true;
        }
        for (int i = 0; i < choixJoueurs.length; i++) {
            if (choixJoueurs[i] >= 0) {
                resultat[i] = true;
            }
        }
        if (reseau != null) {
            String requete = "SELECT DISTINCT message FROM reseau_salon "
                    + "WHERE message IN ('PRESENCE_P1', 'PRESENCE_P2', "
                    + "'PRESENCE_P3', 'PRESENCE_P4')";
            try (Connection c = DriverManager.getConnection(
                        URL_BDD, USER_BDD, PASSWORD_BDD);
                 PreparedStatement st = c.prepareStatement(requete);
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    String message = rs.getString(1);
                    if (message != null && message.matches("PRESENCE_P[1-4]")) {
                        int numero = message.charAt(message.length() - 1) - '1';
                        resultat[numero] = true;
                    }
                }
            } catch (SQLException ex) {
                System.err.println("[BrawlMuLa][VS] Lecture des joueurs "
                        + "en base impossible : " + ex.getMessage());
            }
        }
        return resultat;
    }

    private void ouvrirFenetreDeJeu() {
        if (partieLancee) return;

        int carte = reseau == null ? choixCarte : choixCarteReseau;
        String cheminCarte = imagesCartes.isEmpty()
                ? "/ressources/Maps/chateau.png"
                : imagesCartes.get(carte);
        if (cheminCarte.contains("/cartes/")) {
            cheminCarte = cheminCarte.replace("/cartes/", "/Maps/");
        }

        String j1 = nomPersonnage(0);
        String j2 = nomPersonnage(1);
        String j3 = nomPersonnage(2);
        String j4 = nomPersonnage(3);
        boolean[] presents = reseau == null
                ? new boolean[]{true, true, true, true}
                : joueursPresentsAuDepart();

        StringBuilder joueurs = new StringBuilder();
        for (int i = 0; i < presents.length; i++) {
            if (presents[i]) joueurs.append(" J").append(i + 1);
        }
        System.out.println("[BrawlMuLa][Selection] START_GAME ; carte="
                + cheminCarte + " ; joueurs=" + joueurs);

        // Garder le reseau vivant lorsque Selection se ferme.
        partieLancee = true;
        Musiques_sons.stop();

        // Intro de la map -> VS adaptatif -> fenetre de combat.
        SequenceAvantCombat.lancer(j1, j2, j3, j4,
                cheminCarte, reseau, monNumeroJoueur, presents);
        this.dispose();
    }

    private String nomPersonnage(int joueur) {
        int index = choixJoueurs[joueur];
        return index < 0 ? PERSONNAGES_PAR_DEFAUT[joueur] : personnages.get(index);
    }

    @Override
    public void dispose() {
        if (reseau != null && !partieLancee) {
            reseau.fermer();
        }
        super.dispose();
    }

    private void chargerCartesDepuisBDD() {
        cartes.clear();
        imagesCartes.clear();
        try (Connection c = DriverManager.getConnection(URL_BDD, USER_BDD, PASSWORD_BDD);
             PreparedStatement r = c.prepareStatement("SELECT nom, image FROM Maps ORDER BY nom, image");
             ResultSet rs = r.executeQuery()) {
            while (rs.next()) {
                cartes.add(rs.getString("nom"));
                String img = rs.getString("image");
                if (!img.startsWith("/")) img = "/ressources/Maps/" + img;
                imagesCartes.add(img);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void chargerAvatarsDepuisBDD() {
        personnages.clear();
        imagesPersonnages.clear();
        try (Connection c = DriverManager.getConnection(URL_BDD, USER_BDD, PASSWORD_BDD);
             PreparedStatement r = c.prepareStatement("SELECT nom, avatars_image FROM Avatars ORDER BY nom, avatars_image");
             ResultSet rs = r.executeQuery()) {
            while (rs.next()) {
                personnages.add(rs.getString("nom"));
                String img = rs.getString("avatars_image");
                if (!img.startsWith("/")) img = "/ressources/avatars/" + img;
                imagesPersonnages.add(img);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void afficherCarteLocal(int index) {
        if (index >= 0 && index < cartes.size()) {
            lblNomCartes.setText(cartes.get(index));
            String chemin = imagesCartes.get(index);
            java.net.URL imgURL = getClass().getResource(chemin);
            if (imgURL != null) {
                javax.swing.ImageIcon originalIcon = new javax.swing.ImageIcon(imgURL);
                int w = lblCartes.getWidth() > 0 ? lblCartes.getWidth() : 350;
                int h = lblCartes.getHeight() > 0 ? lblCartes.getHeight() : 200;
                java.awt.Image img = originalIcon.getImage().getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH);
                lblCartes.setIcon(new javax.swing.ImageIcon(img));
            }
        }
    }

    private void choisirPersonnage(String nom) {
        for (int index = 0; index < personnages.size(); index++) {
            if (nom.equalsIgnoreCase(personnages.get(index).trim())) {
                int joueur = monNumeroJoueur - 1;
                if (reseau == null) {
                    choixJoueurs[joueur] = index;
                    afficherPersonnage(joueur, index);
                } else {
                    reseau.envoyerMessage("CHOIX_J" + monNumeroJoueur + ":" + index);
                }
                return;
            }
        }
    }

    private void afficherPersonnage(int joueur, int index) {
        nomsJoueurs[joueur].setText(personnages.get(index));
        afficherAvatarLabel(portraitsJoueurs[joueur], imagesPersonnages.get(index));
    }

    private void afficherAvatarLabel(javax.swing.JLabel label, String chemin) {
        java.net.URL imgURL = getClass().getResource(chemin);
        if (imgURL != null) {
            javax.swing.ImageIcon originalIcon = new javax.swing.ImageIcon(imgURL);
            java.awt.Image img = originalIcon.getImage().getScaledInstance(240, 240, java.awt.Image.SCALE_SMOOTH);
            label.setIcon(new javax.swing.ImageIcon(img));
            label.revalidate();
            label.repaint();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        carrot = new javax.swing.JButton();
        Tomato = new javax.swing.JButton();
        Potato = new javax.swing.JButton();
        Nikku = new javax.swing.JButton();
        boutton_gauche = new javax.swing.JButton();
        boutton_droite = new javax.swing.JButton();
        btnCombat = new javax.swing.JButton();
        btnMenu = new javax.swing.JButton();
        lblAvatarJ4 = new javax.swing.JLabel();
        lblAvatarJ3 = new javax.swing.JLabel();
        lblAvatarJ2 = new javax.swing.JLabel();
        lblAvatarJ1 = new javax.swing.JLabel();
        lblNomJ4 = new javax.swing.JLabel();
        lblNomJ3 = new javax.swing.JLabel();
        lblNomJ2 = new javax.swing.JLabel();
        lblNomJ1 = new javax.swing.JLabel();
        lblNomCartes = new javax.swing.JLabel();
        lblCartes = new javax.swing.JLabel();
        fond = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(1280, 760));
        setSize(new java.awt.Dimension(1280, 760));
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        carrot.setIcon(new javax.swing.ImageIcon(getClass().getResource("/brawlmula/INTERFACE/carrot.png"))); // NOI18N
        carrot.setContentAreaFilled(false);
        carrot.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        carrot.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                carrotMouseEntered(evt);
            }
        });
        carrot.addActionListener(this::carrotActionPerformed);
        getContentPane().add(carrot, new org.netbeans.lib.awtextra.AbsoluteConstraints(650, 440, 70, 70));

        Tomato.setIcon(new javax.swing.ImageIcon(getClass().getResource("/brawlmula/INTERFACE/tomato.png"))); // NOI18N
        Tomato.setContentAreaFilled(false);
        Tomato.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        Tomato.setFocusPainted(false);
        Tomato.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                TomatoMouseEntered(evt);
            }
        });
        Tomato.addActionListener(this::TomatoActionPerformed);
        getContentPane().add(Tomato, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 440, 70, 70));

        Potato.setIcon(new javax.swing.ImageIcon(getClass().getResource("/brawlmula/INTERFACE/potato.png"))); // NOI18N
        Potato.setContentAreaFilled(false);
        Potato.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        Potato.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                PotatoMouseEntered(evt);
            }
        });
        Potato.addActionListener(this::PotatoActionPerformed);
        getContentPane().add(Potato, new org.netbeans.lib.awtextra.AbsoluteConstraints(730, 440, 70, 70));

        Nikku.setIcon(new javax.swing.ImageIcon(getClass().getResource("/brawlmula/INTERFACE/nikku.png"))); // NOI18N
        Nikku.setContentAreaFilled(false);
        Nikku.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        Nikku.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                NikkuMouseEntered(evt);
            }
        });
        Nikku.addActionListener(this::NikkuActionPerformed);
        getContentPane().add(Nikku, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 440, 70, 70));

        boutton_gauche.setIcon(new javax.swing.ImageIcon(getClass().getResource("/brawlmula/INTERFACE/fleche gauche.png"))); // NOI18N
        boutton_gauche.setBorderPainted(false);
        boutton_gauche.setContentAreaFilled(false);
        boutton_gauche.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        boutton_gauche.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                boutton_gaucheMouseEntered(evt);
            }
        });
        boutton_gauche.addActionListener(this::boutton_gaucheActionPerformed);
        getContentPane().add(boutton_gauche, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 610, 70, 60));

        boutton_droite.setIcon(new javax.swing.ImageIcon(getClass().getResource("/brawlmula/INTERFACE/fleche droite.png"))); // NOI18N
        boutton_droite.setBorderPainted(false);
        boutton_droite.setContentAreaFilled(false);
        boutton_droite.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        boutton_droite.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                boutton_droiteMouseEntered(evt);
            }
        });
        boutton_droite.addActionListener(this::boutton_droiteActionPerformed);
        getContentPane().add(boutton_droite, new org.netbeans.lib.awtextra.AbsoluteConstraints(780, 610, -1, 60));

        btnCombat.setFont(new java.awt.Font("Fairfax", 1, 48)); // NOI18N
        btnCombat.setForeground(new java.awt.Color(51, 51, 51));
        btnCombat.setText("[ COMBAT ]");
        btnCombat.setBorderPainted(false);
        btnCombat.setContentAreaFilled(false);
        btnCombat.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnCombat.setFocusPainted(false);
        btnCombat.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnCombatMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnCombatMouseExited(evt);
            }
        });
        btnCombat.addActionListener(this::btnCombatActionPerformed);
        getContentPane().add(btnCombat, new org.netbeans.lib.awtextra.AbsoluteConstraints(312, 280, 300, -1));

        btnMenu.setFont(new java.awt.Font("Fairfax", 1, 48)); // NOI18N
        btnMenu.setForeground(new java.awt.Color(51, 51, 51));
        btnMenu.setText("[ MENU ]");
        btnMenu.setBorderPainted(false);
        btnMenu.setContentAreaFilled(false);
        btnMenu.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnMenu.setFocusPainted(false);
        btnMenu.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnMenuMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnMenuMouseExited(evt);
            }
        });
        btnMenu.addActionListener(this::btnMenuActionPerformed);
        getContentPane().add(btnMenu, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 350, -1, -1));

        lblAvatarJ4.setPreferredSize(new java.awt.Dimension(300, 300));
        getContentPane().add(lblAvatarJ4, new org.netbeans.lib.awtextra.AbsoluteConstraints(990, 450, 220, 220));

        lblAvatarJ3.setPreferredSize(new java.awt.Dimension(300, 300));
        getContentPane().add(lblAvatarJ3, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 450, 230, 220));

        lblAvatarJ2.setPreferredSize(new java.awt.Dimension(300, 300));
        getContentPane().add(lblAvatarJ2, new org.netbeans.lib.awtextra.AbsoluteConstraints(990, 110, 220, 230));

        lblAvatarJ1.setPreferredSize(new java.awt.Dimension(300, 300));
        getContentPane().add(lblAvatarJ1, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 110, 230, 230));

        lblNomJ4.setFont(new java.awt.Font("Fairfax", 0, 48)); // NOI18N
        lblNomJ4.setForeground(new java.awt.Color(255, 255, 255));
        lblNomJ4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        getContentPane().add(lblNomJ4, new org.netbeans.lib.awtextra.AbsoluteConstraints(990, 680, 210, 40));

        lblNomJ3.setFont(new java.awt.Font("Fairfax", 0, 48)); // NOI18N
        lblNomJ3.setForeground(new java.awt.Color(255, 255, 255));
        lblNomJ3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        getContentPane().add(lblNomJ3, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 680, 200, 40));

        lblNomJ2.setFont(new java.awt.Font("Fairfax", 0, 48)); // NOI18N
        lblNomJ2.setForeground(new java.awt.Color(255, 255, 255));
        lblNomJ2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        getContentPane().add(lblNomJ2, new org.netbeans.lib.awtextra.AbsoluteConstraints(1000, 340, 200, 40));

        lblNomJ1.setFont(new java.awt.Font("Fairfax", 0, 48)); // NOI18N
        lblNomJ1.setForeground(new java.awt.Color(255, 255, 255));
        lblNomJ1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        getContentPane().add(lblNomJ1, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 340, 190, 40));

        lblNomCartes.setFont(new java.awt.Font("Fairfax", 0, 36)); // NOI18N
        lblNomCartes.setForeground(new java.awt.Color(255, 255, 255));
        lblNomCartes.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        getContentPane().add(lblNomCartes, new org.netbeans.lib.awtextra.AbsoluteConstraints(518, 666, 260, 30));
        getContentPane().add(lblCartes, new org.netbeans.lib.awtextra.AbsoluteConstraints(520, 570, 260, 110));

        fond.setFont(new java.awt.Font("Fairfax", 0, 12)); // NOI18N
        fond.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        fond.setIcon(new javax.swing.ImageIcon(getClass().getResource("/brawlmula/INTERFACE/selection.png"))); // NOI18N
        fond.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        getContentPane().add(fond, new org.netbeans.lib.awtextra.AbsoluteConstraints(-40, 0, -1, -1));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void carrotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_carrotActionPerformed
        Musiques_sons.jouerSon("/ressources/Musiques-sons/selection_joueur.wav");
        choisirPersonnage("Carrot");
    }//GEN-LAST:event_carrotActionPerformed

    private void TomatoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TomatoActionPerformed
        Musiques_sons.jouerSon("/ressources/Musiques-sons/selection_joueur.wav");
        choisirPersonnage("Tomato");
    }//GEN-LAST:event_TomatoActionPerformed

    private void PotatoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PotatoActionPerformed
        Musiques_sons.jouerSon("/ressources/Musiques-sons/selection_joueur.wav");
        choisirPersonnage("Potato");
    }//GEN-LAST:event_PotatoActionPerformed

    private void NikkuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NikkuActionPerformed
        Musiques_sons.jouerSon("/ressources/Musiques-sons/selection_joueur.wav");
        choisirPersonnage("Nikku");
    }//GEN-LAST:event_NikkuActionPerformed

    private void carrotMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_carrotMouseEntered
        Musiques_sons.jouerSon("/ressources/Musiques-sons/click.wav");
    }//GEN-LAST:event_carrotMouseEntered

    private void NikkuMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_NikkuMouseEntered
        Musiques_sons.jouerSon("/ressources/Musiques-sons/click.wav");
    }//GEN-LAST:event_NikkuMouseEntered

    private void TomatoMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_TomatoMouseEntered
        Musiques_sons.jouerSon("/ressources/Musiques-sons/click.wav");
    }//GEN-LAST:event_TomatoMouseEntered

    private void PotatoMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_PotatoMouseEntered
        Musiques_sons.jouerSon("/ressources/Musiques-sons/click.wav");
    }//GEN-LAST:event_PotatoMouseEntered

    private void boutton_droiteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boutton_droiteActionPerformed
        Musiques_sons.jouerSon("/ressources/Musiques-sons/click.wav");
        if (!cartes.isEmpty()) {
            choixCarte = (choixCarte + 1) % cartes.size();
            afficherCarteLocal(choixCarte);

            // Envoyer le changement au joueur 2 si on est l'hôte
            if (isHost && reseau != null) {
                reseau.envoyerMessage("MAP:" + choixCarte);
            }
        }
    }//GEN-LAST:event_boutton_droiteActionPerformed

    private void boutton_gaucheActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boutton_gaucheActionPerformed
        Musiques_sons.jouerSon("/ressources/Musiques-sons/click.wav");
        if (!cartes.isEmpty()) {
            choixCarte = (choixCarte - 1 + cartes.size()) % cartes.size();
            afficherCarteLocal(choixCarte);

            // Envoyer le changement au joueur 2 si on est l'hôte
            if (isHost && reseau != null) {
                reseau.envoyerMessage("MAP:" + choixCarte);
            }
        }
    }//GEN-LAST:event_boutton_gaucheActionPerformed

    private void boutton_gaucheMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_boutton_gaucheMouseEntered
        Musiques_sons.jouerSon("/ressources/Musiques-sons/click.wav");
    }//GEN-LAST:event_boutton_gaucheMouseEntered

    private void boutton_droiteMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_boutton_droiteMouseEntered
        Musiques_sons.jouerSon("/ressources/Musiques-sons/click.wav");
    }//GEN-LAST:event_boutton_droiteMouseEntered

    private void btnCombatMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnCombatMouseEntered
        Musiques_sons.jouerSon("/ressources/Musiques-sons/click.wav");
        btnCombat.setOpaque(true);

        btnCombat.setBackground(new Color(255, 245, 180));
        btnCombat.setForeground(new Color(180, 40, 20));
    }//GEN-LAST:event_btnCombatMouseEntered

    private void btnCombatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCombatActionPerformed
        if (!isHost) {
            return;
        }

//        if (!j1Selectionne || !j2Selectionne || !j3Selectionne || !j4Selectionne) {
//            javax.swing.JOptionPane.showMessageDialog(
//                    this,
//                    "Les quatres joueurs doivent choisir un personnage avant de lancer le combat !",
//                    "Sélection incomplète",
//                    javax.swing.JOptionPane.WARNING_MESSAGE
//            );
//            return; // Interrompt le lancement
//        }
        Musiques_sons.stop();
        Musiques_sons.jouerSon("/ressources/Musiques-sons/selection.wav");

        if (reseau == null) {
            ouvrirFenetreDeJeu();
        } else {
            btnCombat.setEnabled(false);
            reseau.lancerCombat(erreur -> {
                btnCombat.setEnabled(true);
                JOptionPane.showMessageDialog(this, erreur, "Erreur réseau", JOptionPane.ERROR_MESSAGE);
            });
        }
    }//GEN-LAST:event_btnCombatActionPerformed

    private void btnMenuMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnMenuMouseEntered
        Musiques_sons.jouerSon("/ressources/Musiques-sons/click.wav");
        btnMenu.setOpaque(true);

        btnMenu.setBackground(new Color(255, 245, 180));
        btnMenu.setForeground(new Color(180, 40, 20));
    }//GEN-LAST:event_btnMenuMouseEntered

    private void btnMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMenuActionPerformed
        Musiques_sons.stop();
        Musiques_sons.jouerSon("/ressources/Musiques-sons/selection.wav");
        MainMenu Selection = new MainMenu();
        Selection.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnMenuActionPerformed

    private void btnCombatMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnCombatMouseExited
        btnCombat.setOpaque(false);
        btnCombat.setForeground(new Color(50, 50, 50));
    }//GEN-LAST:event_btnCombatMouseExited

    private void btnMenuMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnMenuMouseExited
        btnMenu.setOpaque(false);
        btnMenu.setForeground(new Color(50, 50, 50));
    }//GEN-LAST:event_btnMenuMouseExited

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new Selection().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Nikku;
    private javax.swing.JButton Potato;
    private javax.swing.JButton Tomato;
    private javax.swing.JButton boutton_droite;
    private javax.swing.JButton boutton_gauche;
    private javax.swing.JButton btnCombat;
    private javax.swing.JButton btnMenu;
    private javax.swing.JButton carrot;
    private javax.swing.JLabel fond;
    private javax.swing.JLabel lblAvatarJ1;
    private javax.swing.JLabel lblAvatarJ2;
    private javax.swing.JLabel lblAvatarJ3;
    private javax.swing.JLabel lblAvatarJ4;
    private javax.swing.JLabel lblCartes;
    private javax.swing.JLabel lblNomCartes;
    private javax.swing.JLabel lblNomJ1;
    private javax.swing.JLabel lblNomJ2;
    private javax.swing.JLabel lblNomJ3;
    private javax.swing.JLabel lblNomJ4;
    // End of variables declaration//GEN-END:variables
}