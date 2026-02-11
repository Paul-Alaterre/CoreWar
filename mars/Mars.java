package mars;

import redcode.Instruction;
import redcode.Mode;
import redcode.Opcode;

public class Mars {

    private final Memory memory;

    public Mars(Memory memory) {
        this.memory = memory;
    }

    private int decodeA(Processus p, Instruction instr, Memory memory) {
        int memorySize = memory.getSize();
        switch(instr.getModeA()) {
            case IMMEDIATE:
                return p.getPc(); // ou exception si pas autorisé
            case DIRECT:
                return Math.floorMod(p.getPc() + instr.getA(), memorySize);

            case INDIRECT:
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction target = memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
        }
        return 1;
    }

    private int resolveValueA(Processus p, Instruction instr, Memory memory) {
        if(instr.getModeA() == Mode.IMMEDIATE)
            return instr.getA();
        else
            return memory.read(decodeA(p, instr, memory)).getB();
    }

    private int resolveValueB(Processus p, Instruction instr, Memory memory) {
        if(instr.getModeB() == Mode.IMMEDIATE)
            return instr.getB();
        else
            return memory.read(decodeB(p, instr, memory)).getB();
    }

    private int decodeB(Processus p, Instruction instr, Memory memory) {
        int memorySize = memory.getSize();
        switch(instr.getModeB()) {
            case IMMEDIATE:
                return p.getPc(); // ou exception si pas autorisé
            case DIRECT:
                return Math.floorMod(p.getPc() + instr.getB(), memorySize);

            case INDIRECT:
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction target = memory.read(addr);
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