package simulation;

import mars.Mars;
import mars.Memory;
import mars.Processus;
import mars.Warrior;
import redcode.Instruction;
import redcode.Opcode;
import redcode.Mode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Demo {

    public static void main(String[] args) {

        Memory memory = new Memory(10);
        Mars mars = new Mars(memory);

        /*
         * =============================
         * Chargement Warrior 1 (à 0)
         * =============================
         */
        memory.write(0, new Instruction(
                Opcode.ADD,
                Mode.IMMEDIATE, 1,
                Mode.DIRECT, 1));

        memory.write(1, new Instruction(
                Opcode.JMP,
                Mode.DIRECT, -1,
                Mode.DIRECT, 0));


        /*
         * =============================
         * Chargement Warrior 2 (à 10)
         * =============================
         */

        memory.write(10, new Instruction(
                Opcode.MOV,
                Mode.IMMEDIATE, 7,
                Mode.DIRECT, 11));

        memory.write(11, new Instruction(
                Opcode.DAT,
                Mode.DIRECT, 0,
                Mode.DIRECT, 0));


        // Création du warrior
      List<Warrior> warriors = new ArrayList<>();

        Warrior w1 = new Warrior();
        w1.addProcess(new Processus(0));

        Warrior w2 = new Warrior();
        w2.addProcess(new Processus(10));

        Warrior w3 = new Warrior();
        w3.addProcess(new Processus(15));

        Warrior w4 = new Warrior();
        w4.addProcess(new Processus(10));

        warriors.add(w1);
        warriors.add(w2);

        warriors.add(w3);
        warriors.add(w4);



        // Simulation
        int maxCycles = 20;

        for (int cycle = 0; cycle < maxCycles; cycle++) {

            System.out.println("\n==============================");
            System.out.println("          CYCLE " + cycle);
            System.out.println("==============================");

            for (int i = 0; i < warriors.size(); i++) {

                Warrior warrior = warriors.get(i);

                if (!warrior.isAlive()) {
                    continue;
                }

                Processus p = warrior.nextProcess();

                if (p != null) {

                    boolean alive = mars.execute(p);

                    System.out.println("Warrior " + i + " PC = " + p.getPc());

                    if (alive) {
                        warrior.requeueProcess(p);
                    } else {
                        System.out.println("Warrior " + i + " a perdu un processus !");
                    }
                }
            }
            // Affichage état mémoire
            System.out.println("Cellule 1 B = " + memory.read(1).getB());
            System.out.println("Cellule 11 B = " + memory.read(11).getB());
            System.out.println("------------------------------");

            System.out.println("\nÉtat des Warriors :");
            for (int i = 0; i < warriors.size(); i++) {
                System.out.println("Warrior " + i + " : " +
                        (warriors.get(i).isAlive() ? "VIVANT" : "MORT"));
            }

            // Vérification fin
            if (countAlive(warriors) <= 1) {
                break;
            }

            int alive = countAlive(warriors);

            if (alive == 1) {
                for (int i = 0; i < warriors.size(); i++) {
                    if (warriors.get(i).isAlive()) {
                        System.out.println("\n🏆 Warrior " + i + " GAGNE la partie !");
                    }
                }
            } else {
                System.out.println("\nAucun gagnant.");
            }

        }
    }
    private static int countAlive(List<Warrior> warriors) {
        int count = 0;
        for (Warrior w : warriors) {
            if (w.isAlive()) {
                count++;
            }
        }
        return count;
    }
}
