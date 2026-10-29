/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

/**
 *
 * @author Shyturo
 */
import javax.sound.sampled.*;
import java.io.File;
import java.net.URL;

public class Musiques_sons {
    private static Clip clip;

    public static void playLoop(String filepath) {
        try {
            // Arrête une musique déjà en cours si besoin
            stop();

            File musicFile = new File(filepath);
            if (musicFile.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicFile);
                clip = AudioSystem.getClip();
                clip.open(audioInput);
                clip.loop(Clip.LOOP_CONTINUOUSLY); // Joue en boucle infinie
                clip.start();
            } else {
                System.err.println("Fichier audio introuvable : " + filepath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
        }
    }
    public static void jouerSon(String chemin) {
        try {
            
            URL url = Musiques_sons.class.getResource(chemin);
            
            
            AudioInputStream audio = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            clip.start(); 
        } catch (Exception e) {
            System.out.println("Erreur : son introuvable");
        }
    }
}
    

