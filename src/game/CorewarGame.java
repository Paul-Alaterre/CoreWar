package game;
import java.util.ArrayList;
import java.util.List;
import mars.Mars;
import mars.Memory;
import mars.Processus;
import mars.Warrior;
import redcode.Instruction;



public class CorewarGame {
    private Memory memory;
    private List<Warrior> warriors;
    private Mars mars; // Ton moteur d'exécution
    private int cycles;
    private int maxCycles;
    private List<Instruction> initialCode;
    private int initialWarriorId;
    private int initialPosition;


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

    public void loadWarrior(Warrior w, int address) {

    // Sauvegarde de l’état initial (une seule fois)
    if (warriors.isEmpty()) {
        initialCode = new ArrayList<>();
        for (Instruction instr : w.getInstructions()) {
            initialCode.add(instr.copy());
        }
        initialWarriorId = w.getId();
        initialPosition = address;
    }

    // Charger en mémoire
    int i = 0;
    for (Instruction instr : w.getInstructions()) {
        memory.write(Math.floorMod(address + i, memory.getSize()), instr.copy());
        i++;
    }

    // Créer le premier processus
    w.addProcess(new Processus(address));

    warriors.add(w);
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
    
    public void reset() {
    memory.clear();
    cycles = 0;

    // Effacer les warriors actuels
    warriors.clear();

    // Recréer le warrior initial
    Warrior w = new Warrior(new ArrayList<>(initialCode), initialWarriorId);
    w.setStartAddress(initialPosition);

    // Recréer le processus initial
    w.resetProcesses();

    // Ajouter le warrior à la liste
    warriors.add(w);

    // Recharger son code en mémoire
    loadWarrior(w, initialPosition);
}

