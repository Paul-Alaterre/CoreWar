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
    private Instruction[] cells;

    /**
     * Constructeur : initialise la mémoire à une taille donnée.
     * Chaque cellule est initialisée avec l'instruction DAT 0,0 par défaut
     * (qui correspond à une instruction “neutre” qui tue le processus si exécutée).
     *
     * @param size taille de la mémoire
     */
    public Memory(int size) {
        cells = new Instruction[size];

        // Remplissage de la mémoire avec des instructions DAT 0,0
        for (int i = 0; i < size; i++) {
            cells[i] = new Instruction(
                Opcode.DAT,   // opcode DAT : instruction qui tue le processus
                Mode.DIRECT,  // mode d’adressage A
                0,            // opérande A
                Mode.DIRECT,  // mode d’adressage B
                0             // opérande B
            );
        }
    }

    /**
     * Retourne la taille de la mémoire.
     *
     * @return nombre de cellules
     */
    public int size() {
        return cells.length;
    }

    /**
     * Accès à une cellule mémoire avec **adressage circulaire**.
     * Corewar utilise une mémoire circulaire : après la dernière cellule,
     * on revient au début.
     *
     * @param index adresse demandée
     * @return instruction stockée à cette adresse
     */
    public Instruction get(int index) {
        return cells[Math.floorMod(index, cells.length)];
    }

    /**
     * Écriture d'une instruction dans une cellule mémoire avec
     * **adressage circulaire**.
     *
     * @param index adresse cible
     * @param inst instruction à écrire
     */
    
    public void set(int index, Instruction inst) {
        cells[Math.floorMod(index, cells.length)] = inst;
    }

    /**
     * Retourne le tableau complet de la mémoire.
     * Utile pour les tests, l'affichage ou les algorithmes évolutionnaires.
     *
     * @return tableau d'instructions
     */
    public Instruction[] getCells() {
        return cells;
    }

    /**
     * Remplace le tableau complet de la mémoire.
     * Utile pour cloner une VM ou réinitialiser la mémoire.
     *
     * @param cells nouveau tableau d'instructions
     */
    public void setCells(Instruction[] cells) {
        this.cells = cells;
        
    }
}
