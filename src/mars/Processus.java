package mars;
import redcode.Instruction;

/**
* C'est la classe représentant un processus, c'est à dire un programme qui interagit 
* avec la mémoire en éxecutant les
* instructions contenus dans celle-ci.
*/

public class Processus {
    /**C'est le pointeur du processus, celui qui définit sa position dans la mémoire*/
    private int pc; //program counter

    /**Le constructeur qui prend un indice pour initialiser le pointeur*/
    public Processus(int pc) {
        this.pc = pc;
    }

    /**Un accesseur pour connaître la position du processus*/
    public int getPc(){
        return this.pc;
    }

    /**Une méthode pour redéfinir la position du processus*/
    public void setPc(int pc, int memSize){
        this.pc = Math.floorMod(pc, memSize);
    }

    /**Une méthode pour faire avancer le processus d'un certain 
    * nombre de cases (peu être à supprimer car on peut faire la même 
    * chose avec setPC
    */
    
    public void advance(int n, int memSize) {
        this.pc = Math.floorMod(this.pc + n, memSize);
    }


}
