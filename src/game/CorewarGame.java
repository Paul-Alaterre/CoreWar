package game;
import mars.Mars;
import mars.Memory;
import mars.Warrior;
import redcode.Instruction;
import mars.Processus;
import java.util.List;
import java.util.ArrayList;



public class CorewarGame {
    private Memory memory;
    private List<Warrior> warriors;
    private Mars mars; // Ton moteur d'exécution
    private int cycles;
    private int maxCycles;

    public CorewarGame(Memory memory, int maxCycles) {
        this.memory = memory;
        this.warriors = new ArrayList<>();
        this.mars = new Mars(this.memory);
        this.maxCycles = maxCycles;
        this.cycles = 0;
    }
    
    public List<Warrior> getWarriors(){
        return warriors;
    }

    public Memory getMemory(){
        return memory;
    }

    // 1. Initialisation : Charger les guerriers en mémoire
    public void loadWarrior(Warrior w, int address) {
        // Copier les instructions du Warrior dans la mémoire à partir de 'address'
        int i = 0;
        for(Instruction instr : w.getInstructions()){
            memory.write(Math.floorMod(address+i, memory.getSize()),instr.copy());
            i++;
        }
        w.addProcess(new Processus(address));
        // Ajouter le warrior à la liste des participants
        warriors.add(w);
    }

    // 2. La boucle de jeu principale
    public void run() {
        while (checkVictory() && cycles < maxCycles) {
            step();
            cycles++;
        }
        //fin
    }

    // 3. Exécution d'un tour de table (Round Robin)
    public void step() {
        for (Warrior w : warriors) {
            if (w.isAlive()) {
                // On délègue l'exécution du processus suivant au moteur Mars
                mars.executeTurn(w);
                
                // On vérifie immédiatement s'il est mort après son tour
                if (w.getProcessCount() == 0) {
                    System.out.println(w.getId() + " est éliminé au cycle " + cycles);
                }
            }
        }
    }

    private boolean checkVictory() {
        // Compte combien de guerriers ont encore des processus
        long aliveCount = warriors.stream().filter(Warrior::isAlive).count();
        return aliveCount > 1; // Le jeu continue s'il reste au moins 2 joueurs
    }
}