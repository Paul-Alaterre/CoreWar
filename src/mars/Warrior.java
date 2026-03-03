package mars;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import redcode.Instruction;

/**
 * Représente un warrior (programme) dans MARS.
 * Un warrior peut posséder plusieurs processus actifs.
 */

public class Warrior {

    private List<Instruction> body;
    /** File des processus actifs du warrior */
    private final Queue<Processus> processes;

    private int id;

    /**
     * Constructeur : initialise un warrior vide.
     */
    public Warrior(List<Instruction> body, int id) {
        this.processes = new LinkedList<>();
        this.body = body;
        this.id = id;
    }

    public List<Instruction> getInstructions(){
        return body;
    }

    public int getId(){
        return id;
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
    public Processus getNextProcess() {
        return processes.poll();
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

    public Queue<Processus> getProcessQueue(){
        return processes;
    }
}

