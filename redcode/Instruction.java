package redcode;

public class Instruction {

    /** La variable correspondant à l'Opcode */
    private Opcode opcode;

    /** La variable de correspondant au mode de A*/
    private Mode modeA;
    /** La variable de correspondant à la valeur de A*/
    private int valueA;

    /** La variable de correspondant au mode de B*/
    private Mode modeB;
    /** La variable de correspondant à la valeur de B*/
    private int valueB;

    /** Le constructeur qui prend les éléments necessaires à la construction d'une instruction*/
    public Instruction(Opcode opcode, Mode modeA, int valueA, Mode modeB, int valueB) {
        this.opcode = opcode;
        this.modeA = modeA;
        this.valueA = valueA;
        this.modeB = modeB;
        this.valueB = valueB;
    }

    /**Un accesseur pour la valeur de A*/
    public int getA(){
        return this.valueA;
    }

    /**Un accesseur pour la valeur de B*/
    public int getB(){
        return this.valueB;
    }
    
    /**Un accesseur pour l'Opcode*/
    public Opcode getOpcode() {
    return opcode;
    }

    /**Un accesseur pour le mode de A*/
    public Mode getModeA() {
        return modeA;
    }

    /**Un accesseur pour le mode de B*/
    public Mode getModeB() {
        return modeB;
    }

    /**Une méthode mour modifier la valeur de B*/
    public void setB(int valueB) {
        this.valueB = valueB;
    }

    /**Une méthode pour copier une instruction*/
    public Instruction copy() {
        return new Instruction(opcode, modeA, valueA, modeB, valueB);
    }

}

