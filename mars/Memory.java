package corewar.mars;

import corewar.redcode.Instruction;
import corewar.redcode.Mode;
import corewar.redcode.Opcode;

/**
 * Classe représentant la mémoire de la machine virtuelle MARS.
 * La mémoire est un tableau d'instructions Redcode, partagé entre tous les warriors.
 * Elle est circulaire, c’est-à-dire que les adresses dépassant la taille reviennent au début.
 */
public class Memory {

    /** Tableau d'instructions représentant chaque cellule mémoire */
    private int[] cells;
    private int size;

    /**
     * Constructeur : initialise la mémoire à une taille donnée.
     * Chaque cellule est initialisée avec l'instruction DAT 0,0 par défaut
     * (qui correspond à une instruction “neutre” qui tue le processus si exécutée).
     *
     * @param size taille de la mémoire
     */
    public Memory(int size) {
        this.cells = new int[size];
        this.size = size;

        // Remplissage de la mémoire avec des instructions DAT 0,0
        for (int i = 0; i < this.size; i++) {
            this.cells[i] = 0 ;       // On met toutes les valeurs à 0 pafin d'avoir que des instructions DAT 0, 0
        }
    }

    /**
     * Retourne la taille de la mémoire.
     *
     * @return nombre de cellules
     */
    public int size() {
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
    
    public void write(int index, int value) {
        cells[Math.floorMod(index, this.size)] = value;
    }

}

