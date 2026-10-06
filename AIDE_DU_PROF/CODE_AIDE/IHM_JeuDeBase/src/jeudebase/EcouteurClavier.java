/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jeudebase;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 *
 * @author manal.benaissa
 */
//Ok ! Ici c'est pour écouter votre clavier ! Quand une touche du clavier est appuyé, un "event" est émis. Vous pouvez traiter l'event en question comme suit :
public class EcouteurClavier implements KeyListener {
    
    private Jeu jeu;

    public EcouteurClavier(Jeu jeu) {
        this.jeu = jeu;
    }
    
    @Override
    public void keyPressed(KeyEvent evt) {
        if (evt.getKeyCode() == evt.VK_RIGHT) {
            this.jeu.avatar.setDroite(true);
        }
        if (evt.getKeyCode() == evt.VK_LEFT) {
            this.jeu.avatar.setGauche(true);
        }
    }

    @Override
    public void keyReleased(KeyEvent evt) {
        if (evt.getKeyCode() == evt.VK_RIGHT) {
            this.jeu.avatar.setDroite(false);
        }
        if (evt.getKeyCode() == evt.VK_LEFT) {
            this.jeu.avatar.setGauche(false);
        }
    }
    

    @Override
    public void keyTyped(KeyEvent event) {
        //Nothing to do here !
    }


}
