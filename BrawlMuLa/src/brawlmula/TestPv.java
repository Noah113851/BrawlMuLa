/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package brawlmula;

/**
 *
 * @author hakhtar
 */
public class TestPv {
   
    public static void main(String[] args) {
        JoueurSQL sql = new JoueurSQL();
sql.initialiserPv(1);
System.out.println("Début : " + sql.lirePv(1));
System.out.println("Après un coup de 10 : " + sql.retirerPv(1, 10));
System.out.println("Après un coup de 10 : " + sql.retirerPv(1, 10));
sql.closeTable();
    }
}

