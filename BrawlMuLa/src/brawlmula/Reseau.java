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

    private final String role; // "HOST" ou "CLIENT"
    private int dernierIdLu = 0;
    private boolean enEcoute = false;

    // Constructeur pour l'Hôte
    public Reseau(int port) {
        this.role = "HOST";
        viderSalon(); // Nettoie les anciens messages de la session précédente
    }

    // Constructeur pour le Client
    public Reseau(String ip, int port) {
        this.role = "CLIENT";
        // Synchroniser au dernier ID existant pour ne pas relire les anciens messages
        this.dernierIdLu = getDernierIdActuel();
    }

    private Connection getConnexion() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Nettoyer la table lors de la création d'une partie
    private void viderSalon() {
        try (Connection c = getConnexion(); PreparedStatement st = c.prepareStatement("DELETE FROM reseau_salon")) {
            st.executeUpdate();
            System.out.println("🧹 Salon réseau réinitialisé sur la BDD.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getDernierIdActuel() {
        try (Connection c = getConnexion(); PreparedStatement st = c.prepareStatement("SELECT MAX(id) FROM reseau_salon"); ResultSet rs = st.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Envoyer un message dans le salon SQL
    public void envoyerMessage(String msg) {
        new Thread(() -> {
            try (Connection c = getConnexion(); PreparedStatement st = c.prepareStatement("INSERT INTO reseau_salon (emetteur, message) VALUES (?, ?)")) {
                st.setString(1, role);
                st.setString(2, msg);
                st.executeUpdate();
            } catch (SQLException e) {
                System.err.println("❌ Erreur envoi réseau BDD : " + e.getMessage());
            }
        }).start();
    }

    // Attente simulée du client (pour garder la comptabilité avec Selection.java)
    public void attendreClient() {
        System.out.println("✅ Prêt à recevoir le Joueur 2 via BDD.");
    }

    // Boucle d'écoute des nouveaux messages (exécutée en tâche de fond)
    public void demarrerEcoute(Consumer<String> gestionnaireMessage) {
        enEcoute = true;
        new Thread(() -> {
            while (enEcoute) {
                try {
                    Thread.sleep(150); // Vérification toutes les 150ms

                    try (Connection c = getConnexion(); PreparedStatement st = c.prepareStatement("SELECT id, emetteur, message FROM reseau_salon WHERE id > ? ORDER BY id ASC")) {
                        st.setInt(1, dernierIdLu);
                        ResultSet rs = st.executeQuery();

                        while (rs.next()) {
                            int id = rs.getInt("id");
                            String emetteur = rs.getString("emetteur");
                            String message = rs.getString("message");

                            this.dernierIdLu = id;

                            // Ne traiter que les messages envoyés par L'AUTRE joueur
                            if (!role.equals(emetteur)) {
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

    public void enregistrerHote() {
        viderSalon();
        envoyerMessageDirect("PRESENCE_P1");
    }

// Le client s'inscrit et récupère automatiquement son numéro (2, 3 ou 4)
    public int inscrireClientEtObtenirNumero() {
        int numAttribue = 2; // Par défaut Joueur 2
        try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD)) {

            // 1. Compter combien de joueurs sont déjà inscrits dans la table
            PreparedStatement stCount = c.prepareStatement(
                    "SELECT COUNT(*) FROM reseau_salon WHERE message LIKE 'PRESENCE_P%'"
            );
            ResultSet rs = stCount.executeQuery();
            if (rs.next()) {
                int nbJoueursPresents = rs.getInt(1);
                numAttribue = nbJoueursPresents + 1; // Le nouveau est N+1
            }

            // Sécurité : ne pas dépasser 4 joueurs
            if (numAttribue > 4) {
                numAttribue = 4;
            }

            // 2. Enregistrer la présence du nouveau joueur dans la base
            PreparedStatement stIns = c.prepareStatement(
                    "INSERT INTO reseau_salon (emetteur, message) VALUES (?, ?)"
            );
            stIns.setString(1, "CLIENT");
            stIns.setString(2, "PRESENCE_P" + numAttribue);
            stIns.executeUpdate();

            System.out.println("✅ Inscription réussie : vous êtes le Joueur " + numAttribue);

        } catch (SQLException e) {
            System.err.println("Erreur attribution numéro joueur : " + e.getMessage());
        }
        return numAttribue;
    }

// Envoi direct sans thread pour l'initialisation
    private void envoyerMessageDirect(String msg) {
        try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD); PreparedStatement st = c.prepareStatement("INSERT INTO reseau_salon (emetteur, message) VALUES (?, ?)")) {
            st.setString(1, role);
            st.setString(2, msg);
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void fermer() {
        enEcoute = false;
    }
}
