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
import java.util.function.Consumer;

public class Reseau {

    private static final String URL = "jdbc:mariadb://nemrod.ens2m.fr:3306/2026-2027_s1_vs2_tp2_brawlmula";
    private static final String USER = "etudiant";
    private static final String PASSWORD = "YTDTvj9TR3CDYCmP";

    private String monIdJoueur; // "P1", "P2", "P3" ou "P4"
    private int dernierIdLu = 0; // Commence à 0 pour relire tous les choix existants
    private boolean enEcoute = false;

    // Constructeur Hôte (Joueur 1)
    public Reseau(int port) {
        this.monIdJoueur = "P1";
        this.dernierIdLu = 0;
    }

    // Constructeur Client (Joueurs 2, 3, 4)
    public Reseau(String ip, int port) {
        this.monIdJoueur = "P2"; // Valeur temporaire avant attribution dans inscrireClientEtObtenirNumero
        this.dernierIdLu = 0;
    }

    private Connection getConnexion() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // L'hôte réinitialise la table au lancement de la session
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

    // Inscription du client et définition de son ID unique ("P2", "P3", "P4")
    public int inscrireClientEtObtenirNumero() {
        int numAttribue = 2;
        try (Connection c = getConnexion()) {

            // Compter le nombre de joueurs enregistrés
            PreparedStatement stCount = c.prepareStatement(
                "SELECT COUNT(*) FROM reseau_salon WHERE message LIKE 'PRESENCE_P%'"
            );
            ResultSet rs = stCount.executeQuery();
            if (rs.next()) {
                numAttribue = rs.getInt(1) + 1;
            }

            if (numAttribue > 4) numAttribue = 4;

            // Mettre à jour l'identifiant propre à cette instance
            this.monIdJoueur = "P" + numAttribue;

            // Enregistrer la présence dans la BDD
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

    // Envoi de message sur la BDD avec l'ID du joueur émetteur
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

    // Écoute continue et rejeu de l'historique
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

                            // Ignorer uniquement les messages qu'on a soi-même émis
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

    public void fermer() {
        enEcoute = false;
    }
}