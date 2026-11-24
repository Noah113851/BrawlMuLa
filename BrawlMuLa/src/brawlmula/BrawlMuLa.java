/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */

package brawlmula;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import javax.swing.SwingUtilities;

public class BrawlMuLa {

    private static Font policeFairfax;
    private static boolean chargementEffectue = false;

    public static void main(String[] args) {

        // Charger Fairfax avant de créer les fenêtres
        chargerPoliceFairfax();

        SwingUtilities.invokeLater(() -> {
            MainMenu menu = new MainMenu();
            menu.setVisible(true);
        });
    }

    public static synchronized void chargerPoliceFairfax() {

        if (chargementEffectue) {
            return;
        }

        chargementEffectue = true;

        String chemin = "/ressources/polices/Fairfax.ttf";

        try (InputStream fichier =
                BrawlMuLa.class.getResourceAsStream(chemin)) {

            if (fichier == null) {
                System.err.println(
                        "Police Fairfax introuvable : " + chemin);
                return;
            }

            policeFairfax = Font.createFont(
                    Font.TRUETYPE_FONT,
                    fichier
            );

            GraphicsEnvironment environnement =
                    GraphicsEnvironment
                            .getLocalGraphicsEnvironment();

            environnement.registerFont(policeFairfax);

            System.out.println(
                    "Police Fairfax chargée : "
                    + policeFairfax.getFamily()
            );

        } catch (Exception e) {
            System.err.println(
                    "Erreur de chargement Fairfax : "
                    + e.getMessage()
            );
        }
    }

    // Applique Fairfax aux composants Swing
    // qui utilisent la police Fairfax.
    public static void appliquerPoliceFairfax(Container parent) {

        chargerPoliceFairfax();

        if (policeFairfax == null) {
            return;
        }

        for (Component composant : parent.getComponents()) {

            Font anciennePolice = composant.getFont();

            if (anciennePolice != null
                    && anciennePolice.getName()
                            .equalsIgnoreCase("Fairfax")) {

                composant.setFont(
                        policeFairfax.deriveFont(
                                anciennePolice.getStyle(),
                                (float) anciennePolice.getSize()
                        )
                );
            }

            if (composant instanceof Container) {
                appliquerPoliceFairfax((Container) composant);
            }
        }
    }
}
