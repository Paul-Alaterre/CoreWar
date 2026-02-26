package mars;

import java.util.LinkedList;
import java.util.Queue;
import redcode.Instruction;
import redcode.Mode;
import redcode.Opcode;

/**
 * Représente un warrior (programme) dans MARS.
 * Un warrior peut posséder plusieurs processus actifs.
 */

public class Warrior {

    /** File des processus actifs du warrior */
    private final Queue<Processus> processes;

    /**
     * Constructeur : initialise un warrior vide.
     */
    public Warrior() {
        this.processes = new LinkedList<>();
    }

    /**
     * Ajoute un nouveau processus au warrior.
     *
     * @param p processus à ajouter
     */
    public void addProcess(Processus p) {
        processes.add(p);
    }

    /**
     * Récupère et retire le prochain processus à exécuter
     * (gestion en mode FIFO).
     *
     * @return prochain processus
     */
    public Processus nextProcess() {
        return processes.poll();
    }

    /**
     * Replace un processus en fin de file
     * (si toujours vivant après exécution).
     *
     * @param p processus à remettre dans la file
     */
    public void requeueProcess(Processus p) {
        processes.add(p);
    }

    /**
     * Indique si le warrior a encore des processus actifs.
     *
     * @return true si au moins un processus est vivant
     */
    public boolean isAlive() {
        return !processes.isEmpty();
    }

    /**
     * Retourne le nombre de processus actifs.
     */
    public int getProcessCount() {
        return processes.size();
    }
}

