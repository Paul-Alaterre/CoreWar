package simulation;
import mars.Mars;
import mars.Memory;
import mars.Processus;
import redcode.Instruction;
import redcode.Opcode;
import redcode.Mode;

public class Demo{

   public static void main(String[] args) {

        Memory memory = new Memory(10);

        // Charger programme
        memory.write(0, new Instruction(
                Opcode.MOV,
                Mode.IMMEDIATE, 5,
                Mode.DIRECT, 1));

        memory.write(1, new Instruction(
                Opcode.DAT,
                Mode.DIRECT, 0,
                Mode.DIRECT, 0));

        memory.write(2, new Instruction(
                Opcode.ADD,
                Mode.IMMEDIATE, 3,
                Mode.DIRECT, 1));

        memory.write(3, new Instruction(
                Opcode.JMP,
                Mode.DIRECT, -2,
                Mode.DIRECT, 0));

        Processus p = new Processus(0);
        Mars mars = new Mars(memory);

        // Simulation de 6 cycles
        for(int i = 0; i < 6; i++) {
            System.out.println("Cycle " + i + " PC=" + p.getPc());
            mars.execute(p);

            Instruction cell1 = memory.read(1);
            System.out.println("Cellule 1 B = " + cell1.getB());
            System.out.println("-------------------");
        }
    }
}