package redcode;

public class Instruction {

    private Opcode opcode;

    private Mode modeA;
    private int valueA;

    private Mode modeB;
    private int valueB;

    public Instruction(Opcode opcode,
                       Mode modeA, int valueA,
                       Mode modeB, int valueB) {
        this.opcode = opcode;
        this.modeA = modeA;
        this.valueA = valueA;
        this.modeB = modeB;
        this.valueB = valueB;
    }

    public int getA(){
        return this.valueA;
    }

    public int getB(){
        return this.valueB;
    }

    public Opcode getOpcode() {
    return opcode;
    }

    public Mode getModeA() {
        return modeA;
    }

    public Mode getModeB() {
        return modeB;
    }

    public void setB(int valueB) {
        this.valueB = valueB;
    }

    public Instruction copy() {
        return new Instruction(
            opcode,
            modeA, valueA,
            modeB, valueB
        );
    }

}
