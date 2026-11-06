/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Reseau {

    private static final String URL = "jdbc:mariadb://nemrod.ens2m.fr:3306/2026-2027_s1_vs2_tp2_brawlmula";
    private static final String USER = "etudiant";
    private static final String PASSWORD = "YTDTvj9TR3CDYCmP";

    private String monIdJoueur; // "P1", "P2", "P3", "P4"
    private int dernierIdLu = 0;
    private boolean enEcoute = false;

    // Constructeur Hôte (Joueur 1)
    public Reseau(int port) {
        this.monIdJoueur = "P1";
        this.dernierIdLu = 0;
    }

    // Constructeur Client (Joueurs 2, 3, 4)
    public Reseau(String ip, int port) {
        this.monIdJoueur = "P2";
        this.dernierIdLu = 0;
    }

    private Connection getConnexion() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Réinitialise le salon SQL au démarrage de l'Hôte
    public void enregistrerHote() {
        try (Connection c = getConnexion();
             PreparedStatement st = c.prepareStatement("DELETE FROM reseau_salon")) {
            st.executeUpdate();
            System.out.println("🧹 Salon réseau réinitialisé sur la BDD.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        envoyerMessageDirect("PRESENCE_P1");
    }

    // Attribue automatiquement le numéro de slot (2, 3 ou 4) au client
    public int inscrireClientEtObtenirNumero() {
        int numAttribue = 2;
        try (Connection c = getConnexion()) {
            PreparedStatement stCount = c.prepareStatement(
                "SELECT COUNT(*) FROM reseau_salon WHERE message LIKE 'PRESENCE_P%'"
            );
            ResultSet rs = stCount.executeQuery();
            if (rs.next()) {
                numAttribue = rs.getInt(1) + 1;
            }
            if (numAttribue > 4) numAttribue = 4;

            this.monIdJoueur = "P" + numAttribue;

            PreparedStatement stIns = c.prepareStatement(
                "INSERT INTO reseau_salon (emetteur, message) VALUES (?, ?)"
            );
            stIns.setString(1, this.monIdJoueur);
            stIns.setString(2, "PRESENCE_P" + numAttribue);
            stIns.executeUpdate();

            System.out.println("✅ Joueur inscrit sous l'identifiant : " + this.monIdJoueur);

        } catch (SQLException e) {
            System.err.println("❌ Erreur attribution numéro joueur : " + e.getMessage());
        }
        return numAttribue;
    }

    public void envoyerMessage(String msg) {
        new Thread(() -> envoyerMessageDirect(msg)).start();
    }

    private void envoyerMessageDirect(String msg) {
        try (Connection c = getConnexion();
             PreparedStatement st = c.prepareStatement("INSERT INTO reseau_salon (emetteur, message) VALUES (?, ?)")) {
            st.setString(1, monIdJoueur);
            st.setString(2, msg);
            st.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Erreur envoi réseau BDD : " + e.getMessage());
        }
    }

    // Écoute des événements du salon réseau
    public void demarrerEcoute(Consumer<String> gestionnaireMessage) {
        enEcoute = true;
        new Thread(() -> {
            while (enEcoute) {
                try {
                    Thread.sleep(150);
                    try (Connection c = getConnexion();
                         PreparedStatement st = c.prepareStatement(
                             "SELECT id, emetteur, message FROM reseau_salon WHERE id > ? ORDER BY id ASC"
                         )) {
                        st.setInt(1, dernierIdLu);
                        ResultSet rs = st.executeQuery();

                        while (rs.next()) {
                            int id = rs.getInt("id");
                            String emetteur = rs.getString("emetteur");
                            String message = rs.getString("message");

                            this.dernierIdLu = id;

                            if (!this.monIdJoueur.equals(emetteur)) {
                                java.awt.EventQueue.invokeLater(() -> gestionnaireMessage.accept(message));
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erreur écoute réseau BDD : " + e.getMessage());
                }
            }
        }).start();
    }

    // Met à jour les coordonnées (x, y) de la table Joueur (ChoixJ1..ChoixJ4)
    public void envoyerMaPosition(int monNumeroJoueur, int x, int y) {
        new Thread(() -> {
            String pseudo = "ChoixJ" + monNumeroJoueur;
            try (Connection c = getConnexion();
                 PreparedStatement st = c.prepareStatement(
                     "UPDATE Joueur SET x = ?, y = ? WHERE pseudo = ?"
                 )) {
                st.setInt(1, x);
                st.setInt(2, y);
                st.setString(3, pseudo);
                st.executeUpdate();
            } catch (SQLException e) {
                System.err.println("❌ Erreur synchro position : " + e.getMessage());
            }
        }).start();
    }

    // Réinitialise les positions initiales sur la carte
    public void reinitialiserPositionsBDD() {
        try (Connection c = getConnexion()) {
            PreparedStatement st = c.prepareStatement(
                "UPDATE Joueur SET x = ?, y = ? WHERE pseudo = ?"
            );
            int[][] positions = {{250, 450}, {950, 450}, {450, 450}, {750, 450}};
            for (int i = 1; i <= 4; i++) {
                st.setInt(1, positions[i - 1][0]);
                st.setInt(2, positions[i - 1][1]);
                st.setString(3, "ChoixJ" + i);
                st.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Récupère l'état des 4 combattants dans la BDD
    public List<JoueurData> lireTousLesJoueurs() {
        List<JoueurData> liste = new ArrayList<>();
        try (Connection c = getConnexion();
             PreparedStatement st = c.prepareStatement("SELECT pseudo, x, y, emplacement, idavatar FROM Joueur");
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                liste.add(new JoueurData(
                    rs.getString("pseudo"),
                    rs.getInt("x"),
                    rs.getInt("y"),
                    rs.getInt("emplacement"),
                    rs.getInt("idavatar")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return liste;
    }

    public void fermer() {
        enEcoute = false;
    }

    public static class JoueurData {
        public String pseudo;
        public int x, y;
        public int emplacement;
        public int idavatar;

        public JoueurData(String pseudo, int x, int y, int emplacement, int idavatar) {
            this.pseudo = pseudo;
            this.x = x;
            this.y = y;
            this.emplacement = emplacement;
            this.idavatar = idavatar;
        }
    }
}