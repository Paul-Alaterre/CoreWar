package mars;

import redcode.Instruction;
import redcode.Mode;
import redcode.Opcode;

/**
 * Classe représentant la mémoire de la machine virtuelle MARS.
 * La mémoire est un tableau d'instructions Redcode, partagé entre tous les warriors.
 * Elle est circulaire, c’est-à-dire que les adresses dépassant la taille reviennent au début.
 */
public class Memory {

    /** Tableau d'instructions représentant chaque cellule mémoire ainsi que sa taille */
    private Instruction[] cells;
    private int size;

    /**
     * Constructeur : initialise la mémoire à une taille donnée.
     * Chaque cellule est initialisée avec l'instruction DAT 0,0 par défaut
     * (qui correspond à une instruction “neutre” qui tue le processus si exécutée).
     *
     * @param size taille de la mémoire
     */
    public Memory(int size) {
        this.size = size;
        this.cells = new Instruction[size];

        Instruction dat0 = new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, 0);

        for (int i = 0; i < size; i++) {
            cells[i] = dat0.copy(); //Initialisation de toutes les cases avec Dat 0 0
        }
    }
    /**
     * Retourne la taille de la mémoire.
     *
     * @return nombre de cellules
     */
    public int getSize() {
        return this.size;
    }

    /**
     * Accès à une cellule mémoire avec **adressage circulaire**.
     * Corewar utilise une mémoire circulaire : après la dernière cellule,
     * on revient au début.
     *
     * @param index adresse demandée
     * @return instruction stockée à cette adresse
     */
    public Instruction read(int index) {
        return cells[Math.floorMod(index, this.size)];
    }

    /**
     * Écriture d'une instruction dans une cellule mémoire avec
     * **adressage circulaire**.
     *
     * @param index adresse cible
     * @param inst instruction à écrire
     */
    
    public void write(int index, Instruction instruction) {
        cells[Math.floorMod(index, this.size)] = instruction;
    }

}



