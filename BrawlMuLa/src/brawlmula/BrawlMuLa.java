/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package brawlmula;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class BrawlMuLa {

    public static void main(String[] args) {
        try {
            chargerPoliceFairfax();
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();

            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(
                    null,
                    "Impossible de charger la police Fairfax.\n\n"
                    + e.getMessage()
                    + "\n\nVérifie la présence du fichier dans\n"
                    + "src/ressources/polices/Fairfax.zip\n"
                    + "puis effectue « Clean and Build ».",
                    "Chargement de Fairfax",
                    JOptionPane.ERROR_MESSAGE
                )
            );

            return;
        }

        SwingUtilities.invokeLater(() -> {
            MainMenu menu = new MainMenu();
            menu.setVisible(true);
        });
    }

    private static void chargerPoliceFairfax()
            throws IOException, FontFormatException {

        InputStream fichier = BrawlMuLa.class.getResourceAsStream(
                "/ressources/polices/Fairfax.zip"
        );

        if (fichier == null) {
            throw new IOException(
                    "L’archive Fairfax.zip est absente des ressources du jeu."
            );
        }

        GraphicsEnvironment environnement =
                GraphicsEnvironment.getLocalGraphicsEnvironment();

        boolean fairfaxChargee = false;

        try (ZipInputStream archive = new ZipInputStream(fichier)) {
            ZipEntry entree;

            while ((entree = archive.getNextEntry()) != null) {
                String nom = entree.getName().toLowerCase(Locale.ROOT);

                if (entree.isDirectory() || !nom.endsWith(".ttf")) {
                    continue;
                }

                byte[] donnees = archive.readAllBytes();

                Font police = Font.createFont(
                        Font.TRUETYPE_FONT,
                        new ByteArrayInputStream(donnees)
                );

                environnement.registerFont(police);

                if ("Fairfax".equals(police.getFamily(Locale.ROOT))) {
                    fairfaxChargee = true;
                }
            }
        }

        if (!fairfaxChargee) {
            throw new IOException(
                    "L’archive ne contient aucune police de la famille Fairfax."
            );
        }
    }
}