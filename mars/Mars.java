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
    * après avoir été décodé. la méthode utilise le pointeur du processus, 
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
    * Cette fonction renvoie la valeur donnée par A après avoir trouvé l'indice de l'instruction vidée
    */
    private int resolveValueA(Processus p, Instruction instr) {
        if(instr.getModeA() == Mode.IMMEDIATE)
            return instr.getA();
        else
            return this.memory.read(decodeA(p, instr, this.memory)).getB();
    }

    private int resolveValueB(Processus p, Instruction instr) {
        if(instr.getModeB() == Mode.IMMEDIATE)
            return instr.getB();
        else
            return this.memory.read(decodeB(p, instr, this.memory)).getB();
    }

    private int decodeB(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeB()) {
            case IMMEDIATE:
                return p.getPc(); // ou exception si pas autorisé
            case DIRECT:
                return Math.floorMod(p.getPc() + instr.getB(), memorySize);

            case INDIRECT:
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
        }

        return 1;
    }

    public void execute(Processus p) {
        Instruction instr = memory.read(p.getPc());

        switch(instr.getOpcode()) {
            case DAT:
            // Le processus meurt → on ne le remet pas dans le scheduler
            break;

            case MOV: {
                int srcVal;
                int dstAddr = decodeB(p, instr, memory);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    srcVal = instr.getA(); // valeur immédiate
                    // créer une instruction DAT pour stocker la valeur
                    memory.write(dstAddr, new Instruction(Opcode.DAT, Mode.IMMEDIATE, srcVal, Mode.DIRECT, 0));
                } else {
                    int srcAddr = decodeA(p, instr, memory);
                    memory.write(dstAddr, memory.read(srcAddr).copy());
                }

                p.advance(1, memory.getSize());
                break;
            }

            case ADD: {
                int valToAdd = resolveValueA(p, instr, memory);
                int dstAddr = decodeB(p, instr, memory);
                Instruction dstInstr = memory.read(dstAddr).copy();
                dstInstr.setB(dstInstr.getB() + valToAdd);
                memory.write(dstAddr, dstInstr);
                p.advance(1, memory.getSize());
                break;
            }


            case JMP: {
                int newPc = decodeA(p, instr, memory);
                p.setPc(newPc, memory.getSize());
                break;
            }

            
        }
    }

}
