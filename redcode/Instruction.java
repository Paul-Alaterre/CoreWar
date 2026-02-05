package corewar.redcode;

import corewar.mars.Mars;
import corewar.mars.Memory;
import corewar.mars.Process;

public class Instruction {
    public Opcode opcode;
    public Mode modeA;
    public Mode modeB;
    public int a;
    public int b;

    public Instruction(Opcode opcode, Mode modeA, int a, Mode modeB, int b) {
        this.opcode = opcode;
        this.modeA = modeA;
        this.a = a;
        this.modeB = modeB;
        this.b = b;
    }

    @Override
    public String toString() {
        return opcode + " " + modeA + " " + a + ", " + modeB + " " + b;
    }


                                               /* EXECUTE */

        public void execute(Mars mars, Process p) {
    Memory mem = mars.getMemory();

    switch (opcode) {

        case DAT ->                                    // Le process meurt immédiatement
            mars.killProcess(p);

        case JMP ->                                    // Saut direct : pc = pc + a
            p.pc = Math.floorMod(p.pc + a, mem.size());

        case MOV -> {
            // Copie l'instruction à l'adresse (pc + a) vers (pc + b)
            
            Instruction src = mem.get(p.pc + a);
            Instruction copy = new Instruction(
                    src.opcode, src.modeA, src.a, src.modeB, src.b
            );
            mem.set(p.pc + b, copy);
            p.pc = Math.floorMod(p.pc + 1, mem.size());
            }

        case ADD -> {
            // Ajoute la valeur A au champ B de l'instruction cible
            Instruction target = mem.get(p.pc + b);
            if (target == null) {
                mars.killProcess(p);
                return;
            }

            
            Instruction newInst = new Instruction(
                    target.opcode,
                    target.modeA,
                    target.a,
                    target.modeB,
                    target.b + this.a
            );
            mem.set(p.pc + b, newInst);
            p.pc = Math.floorMod(p.pc + 1, mem.size());
            }

        default -> // Pour l'instant, rien d'autre
            p.pc = Math.floorMod(p.pc + 1, mem.size());
    }

    if (opcode == null) {
        mars.killProcess(p);
    }
}

}
