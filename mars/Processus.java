package mars;
import redcode.Instruction;

public class Processus {

    private int pc; //program counter

    public Processus(int pc) {
        this.pc = pc;
    }

    public int getPc(){
        return this.pc;
    }
    
    public void setPc(int pc, int memSize){
        this.pc = Math.floorMod(pc, memSize);
    }

    public void advance(int n, int memSize) {
        this.pc = Math.floorMod(this.pc + n, memSize);
    }

}