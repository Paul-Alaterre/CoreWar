package mars;

import redcode.Instruction;
import redcode.Mode;
import redcode.Opcode;

/**
* C'est la classe de la machine virtuelle MARS. Elle fait le lien entre les
* les instructions, les processus
*/

public class Mars {

    /** C'est la mémoire associé à la machine virtuelle*/
    private final Memory memory;

    /** Le constructeur qui prend en entrée une mémoire*/
    public Mars(Memory memory) {
        this.memory = memory;
    }

    /** 
    * Cette méthode permet de retourner un indice dans le mémoire
    * Cet indice est celui donné par la première valeur de l'instruction (A)
    * après avoir été décodé. La méthode utilise le pointeur du processus, 
    * l'instruction donnée et la taille de la mémoire. Le décodage est nécessaire
    * en raison des différents modes d'adressages.
    */
    private int decodeA(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeA()) {
            case IMMEDIATE:
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            case DIRECT:
                return Math.floorMod(p.getPc() + instr.getA(), memorySize);

            case INDIRECT:
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
        }
        return 1;
    }

    /**
    * Cette méthode renvoie la valeur donnée par l'instruction visée par A. 
    * On récupère le B de cette instruction. Pour le mode immédiat on prend simplement
    * la valeur de A
    */
    private int resolveValueA(Processus p, Instruction instr) {
        if(instr.getModeA() == Mode.IMMEDIATE)
            return instr.getA();
        else
            return this.memory.read(decodeA(p, instr)).getB();
    }
    
    /** 
    * Cette méthode permet de retourner un indice dans le mémoire
    * Cet indice est celui donné par la deuxième valeur de l'instruction (B)
    * après avoir été décodé. La méthode utilise le pointeur du processus, 
    * l'instruction donnée et la taille de la mémoire. Le décodage est nécessaire
    * en raison des différents modes d'adressages.
    */
    private int decodeB(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeB()) {
            case IMMEDIATE:
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            case DIRECT:
                return Math.floorMod(p.getPc() + instr.getB(), memorySize);

            case INDIRECT:
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
        }
        return 1;
    }

    /**
    * Cette méthode renvoie la valeur donnée par l'instruction visée par B. 
    * On récupère le B de cette instruction. Pour le mode immédiat on prend simplement
    * la valeur de B de l'instruction initiale
    */

    private int resolveValueB(Processus p, Instruction instr) {
        if(instr.getModeB() == Mode.IMMEDIATE)
            return instr.getB();
        else
            return this.memory.read(decodeB(p, instr)).getB();
    }

    /**
    * Cette méthode permet l'éxécution de l'instruction sur 
    * laquelle pointe le pointeur du precessus. C'est à ce 
    * moment que les différents types d'instructions sont différenciés
    */

    public void execute(Processus p) {
        Instruction instr = this.memory.read(p.getPc());

        switch(instr.getOpcode()) {
            case DAT:
            // Le processus meurt
            break;

            /*
            Dans le MOV on copie 'instruction pointée par A dans à l'adresse donnée
            par B. Dans le cas ou A est immédiat, on cré un DAT contenant sa valeur pour
            remplacer l'instruction pointée par B.
            */

            case MOV: {
                int srcVal;
                int dstAddr = decodeB(p, instr);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    srcVal = instr.getA(); // valeur immédiate
                    // créer une instruction DAT pour stocker la valeur
                    memory.write(dstAddr, new Instruction(Opcode.DAT, Mode.IMMEDIATE, srcVal, Mode.DIRECT, 0));
                } else {
                    int srcAddr = decodeA(p, instr);
                    memory.write(dstAddr, memory.read(srcAddr).copy());
                }
                p.advance(1, memory.getSize());
                break;
            }

            /*
            Dans le CoreWar on ne doit jamais modifier les instructions directement. Il faut 
            en créer une nouvelle et la mettre à la place de l'ancienne
            Dans le ADD on va chercher la valeur donnée par A à ajouter 
            et on copie l'instruction de destination pour la modifier et la réinjecter ensuite
            */

            case ADD: {
                int valToAdd = resolveValueA(p, instr);
                int dstAddr = decodeB(p, instr);
                Instruction dstInstr = memory.read(dstAddr).copy();
                dstInstr.setB(dstInstr.getB() + valToAdd);
                memory.write(dstAddr, dstInstr);
                p.advance(1, memory.getSize());
                break;
            }

            /*
            Le JMP permet d'ajouter au pointeur du processus une valeur
            Il va cherher l'indice donné par A et place le pointeur à cette adresse.
            B n'a aucun effet dans cette instruction
            */

            case JMP: {
                int newPc = decodeA(p, instr);
                p.setPc(newPc, memory.getSize());
                break;
            }

            // A compléter ...
                
        }
    }

}




