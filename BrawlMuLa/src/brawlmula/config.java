/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;
import java.awt.event.KeyEvent;
import java.util.HashMap;

/**
 *
 * @author Shyturo
 */
public class config {
    public static int volume = 80;

    // Dictionnaire contenant les 10 actions et leurs touches associées
    public static HashMap<String, Integer> touches = new HashMap<>();

    static {
        // Touches par défaut
        touches.put("HAUT", KeyEvent.VK_Z);
        touches.put("BAS", KeyEvent.VK_S);
        touches.put("GAUCHE", KeyEvent.VK_Q);
        touches.put("DROITE", KeyEvent.VK_D);
        touches.put("SAUTER", KeyEvent.VK_SPACE);
        touches.put("ATTAQUE_LEGERE", KeyEvent.VK_J);
        touches.put("ATTAQUE_LOURDE", KeyEvent.VK_K);
        touches.put("PROTEGER", KeyEvent.VK_A);
        touches.put("CAPACITE", KeyEvent.VK_E);
        touches.put("ULTI", KeyEvent.VK_R);
    }
    
}
