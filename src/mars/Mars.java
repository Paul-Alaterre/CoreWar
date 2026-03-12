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
    public int decodeA(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeA()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
            return Math.floorMod(p.getPc() + instr.getA(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }
    
    /** 
    * Cette méthode permet de retourner un indice dans le mémoire
    * Cet indice est celui donné par la deuxième valeur de l'instruction (B)
    * après avoir été décodé. La méthode utilise le pointeur du processus, 
    * l'instruction donnée et la taille de la mémoire. Le décodage est nécessaire
    * en raison des différents modes d'adressages.
    */
    public int decodeB(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeB()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
                return Math.floorMod(p.getPc() + instr.getB(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }

    /**
    * Cette méthode permet l'éxécution de l'instruction sur 
    * laquelle pointe le pointeur du precessus. C'est à ce 
    * moment que les différents types d'instructions sont différenciés
    */

    public void execute(Processus p) {
    Instruction instr = this.memory.read(p.getPc());

        switch(instr.getOpcode()) {
            /*
            Dans le MOV on copie 'instruction pointée par A dans à l'adresse donnée
            par B. Dans le cas ou A est immédiat, on cré un DAT contenant sa valeur pour
            remplacer l'instruction pointée par B.
            */

            case MOV: {
                int dstAddr = decodeB(p, instr);
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On crée un DAT qui transporte la valeur immédiate
                    memory.write(dstAddr, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, instr.getA()));
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
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex).copy();
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On ajoute A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() + instr.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else{
                    // On ajoute le a et le B de la source respectivement à A et B de la target
                    int sourceIndex = decodeA(p, instr);
                    Instruction source = memory.read(sourceIndex);
                    int resultA = Math.floorMod(target.getA() + source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() + source.getB(), memory.getSize());
                    target.setA(resultA);
                    target.setB(resultB);
                }
                memory.write(targetIndex, target);
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

            /*
            Si le mode de A est immédiat alors la valeur contenue dans le champ A est
            directement soustrait à la cible pointée par le champs B le A des la source
            Sinon on soustrait à la cible pointée par B le A et le B de source désignée
            */

            case SUB: {
                int sourceAddr = decodeA(p, instr);
                int targetIndex = decodeB(p, instr);
                
                Instruction source = memory.read(sourceAddr);
                Instruction target = memory.read(targetIndex).copy();

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On soustrait A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() - source.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else {
                    // Soustraction A-A et B-B
                    int resultA = Math.floorMod(target.getA() - source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() - source.getB(), memory.getSize());
                    
                    target.setA(resultA);
                    target.setB(resultB);
                }

                memory.write(targetIndex, target);
                p.advance(1, memory.getSize());
                break;
            }

            /*
            Cette instruction permet de sauter à l'adresse que pointe A si la target visée par B est nulle
            et avance juste de 1 sinon
            */
            case JMZ: {
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()==0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }

            /*
            Cette instruction est exactement le contraire de JMZ donc elle saute que si la target 
            visée par B est non nulle
            */

            case JMN:{
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()!=0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }
            /*
            Cette instruction permet de passer une instruction si un test est vérifié.
            Si le champs A est immédiat alors le test est l'égalité entre la valeur de A et la valeur du champs B pointé par B
            Sinon, le test repose sur l'égalité des valeurs et des modes entre les instruciton visée par la source et la target
            */

            case CMP:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // Comparaison valeur A vs Champ B de la cible
                    equal = (instr.getA() == target.getB());
                } 
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    
                    // Comparaison complète : Opcode + Modes + Valeurs
                    equal = (source.getOpcode() == target.getOpcode() &&
                            source.getModeA() == target.getModeA() &&
                            source.getA()     == target.getA()     &&
                            source.getModeB() == target.getModeB() &&
                            source.getB()     == target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }
            
            case SLT:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    equal = (instr.getA() < target.getB());
                }
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    equal = (source.getA() < target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }

            case DJN :{
                int targetAddr = decodeB(p, instr);
                Instruction target = memory.read(targetAddr);

                // 1. Décrémentation du champ B de la cible
                target.setB(target.getB() - 1);
                
                // Important : il faut réécrire en mémoire car on a modifié l'instruction
                memory.write(targetAddr, target);

                // 2. Test du résultat (Not Zero)
                if (target.getB() != 0) {
                    // 3. Saut vers A
                    int jumpAddr = decodeA(p, instr);
                    p.setPc(jumpAddr, memory.getSize());
                } 
                else {
                    // On continue normalement
                    p.advance(1, memory.getSize());
                }
                break;
            }

            //Dernière instriction SPL à coder en même temps que les warriors

            default:
                throw new IllegalStateException("Opcode non géré");
        }
    }


    public void executeTurn(Warrior warrior) {
        // 1. Sortir le processus de la file
        Processus p = warrior.getNextProcess();
        Instruction instr = memory.read(p.getPc());

        // 2. Vérifier si c'est un DAT
        if (instr.getOpcode() == Opcode.DAT) {
            // Le processus n'est PAS rajouté à la file. 
            // Il disparaît de la circulation.
            System.out.println("Processus mort à l'adresse " + p.getPc());
        } 
        else {
            // 3. Exécuter l'instruction (MOV, ADD, JMP, SPL, etc.)
            execute(p); 

            // 4. Le replacer en fin de file pour son prochain tour
            warrior.addProcess(p);
        }

    }
}package mars;

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
    public int decodeA(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeA()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
            return Math.floorMod(p.getPc() + instr.getA(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }
    
    /** 
    * Cette méthode permet de retourner un indice dans le mémoire
    * Cet indice est celui donné par la deuxième valeur de l'instruction (B)
    * après avoir été décodé. La méthode utilise le pointeur du processus, 
    * l'instruction donnée et la taille de la mémoire. Le décodage est nécessaire
    * en raison des différents modes d'adressages.
    */
    public int decodeB(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeB()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
                return Math.floorMod(p.getPc() + instr.getB(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }

    /**
    * Cette méthode permet l'éxécution de l'instruction sur 
    * laquelle pointe le pointeur du precessus. C'est à ce 
    * moment que les différents types d'instructions sont différenciés
    */

    public void execute(Processus p) {
    Instruction instr = this.memory.read(p.getPc());

        switch(instr.getOpcode()) {
            /*
            Dans le MOV on copie 'instruction pointée par A dans à l'adresse donnée
            par B. Dans le cas ou A est immédiat, on cré un DAT contenant sa valeur pour
            remplacer l'instruction pointée par B.
            */

            case MOV: {
                int dstAddr = decodeB(p, instr);
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On crée un DAT qui transporte la valeur immédiate
                    memory.write(dstAddr, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, instr.getA()));
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
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex).copy();
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On ajoute A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() + instr.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else{
                    // On ajoute le a et le B de la source respectivement à A et B de la target
                    int sourceIndex = decodeA(p, instr);
                    Instruction source = memory.read(sourceIndex);
                    int resultA = Math.floorMod(target.getA() + source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() + source.getB(), memory.getSize());
                    target.setA(resultA);
                    target.setB(resultB);
                }
                memory.write(targetIndex, target);
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

            /*
            Si le mode de A est immédiat alors la valeur contenue dans le champ A est
            directement soustrait à la cible pointée par le champs B le A des la source
            Sinon on soustrait à la cible pointée par B le A et le B de source désignée
            */

            case SUB: {
                int sourceAddr = decodeA(p, instr);
                int targetIndex = decodeB(p, instr);
                
                Instruction source = memory.read(sourceAddr);
                Instruction target = memory.read(targetIndex).copy();

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On soustrait A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() - source.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else {
                    // Soustraction A-A et B-B
                    int resultA = Math.floorMod(target.getA() - source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() - source.getB(), memory.getSize());
                    
                    target.setA(resultA);
                    target.setB(resultB);
                }

                memory.write(targetIndex, target);
                p.advance(1, memory.getSize());
                break;
            }

            /*
            Cette instruction permet de sauter à l'adresse que pointe A si la target visée par B est nulle
            et avance juste de 1 sinon
            */
            case JMZ: {
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()==0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }

            /*
            Cette instruction est exactement le contraire de JMZ donc elle saute que si la target 
            visée par B est non nulle
            */

            case JMN:{
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()!=0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }
            /*
            Cette instruction permet de passer une instruction si un test est vérifié.
            Si le champs A est immédiat alors le test est l'égalité entre la valeur de A et la valeur du champs B pointé par B
            Sinon, le test repose sur l'égalité des valeurs et des modes entre les instruciton visée par la source et la target
            */

            case CMP:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // Comparaison valeur A vs Champ B de la cible
                    equal = (instr.getA() == target.getB());
                } 
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    
                    // Comparaison complète : Opcode + Modes + Valeurs
                    equal = (source.getOpcode() == target.getOpcode() &&
                            source.getModeA() == target.getModeA() &&
                            source.getA()     == target.getA()     &&
                            source.getModeB() == target.getModeB() &&
                            source.getB()     == target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }
            
            case SLT:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    equal = (instr.getA() < target.getB());
                }
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    equal = (source.getA() < target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }

            case DJN :{
                int targetAddr = decodeB(p, instr);
                Instruction target = memory.read(targetAddr);

                // 1. Décrémentation du champ B de la cible
                target.setB(target.getB() - 1);
                
                // Important : il faut réécrire en mémoire car on a modifié l'instruction
                memory.write(targetAddr, target);

                // 2. Test du résultat (Not Zero)
                if (target.getB() != 0) {
                    // 3. Saut vers A
                    int jumpAddr = decodeA(p, instr);
                    p.setPc(jumpAddr, memory.getSize());
                } 
                else {
                    // On continue normalement
                    p.advance(1, memory.getSize());
                }
                break;
            }

            //Dernière instriction SPL à coder en même temps que les warriors

            default:
                throw new IllegalStateException("Opcode non géré");
        }
    }


    public void executeTurn(Warrior warrior) {
        // 1. Sortir le processus de la file
        Processus p = warrior.getNextProcess();
        Instruction instr = memory.read(p.getPc());

        // 2. Vérifier si c'est un DAT
        if (instr.getOpcode() == Opcode.DAT) {
            // Le processus n'est PAS rajouté à la file. 
            // Il disparaît de la circulation.
            System.out.println("Processus mort à l'adresse " + p.getPc());
        } 
        else {
            // 3. Exécuter l'instruction (MOV, ADD, JMP, SPL, etc.)
            execute(p); 

            // 4. Le replacer en fin de file pour son prochain tour
            warrior.addProcess(p);
        }

    }
}package mars;

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
    public int decodeA(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeA()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
            return Math.floorMod(p.getPc() + instr.getA(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }
    
    /** 
    * Cette méthode permet de retourner un indice dans le mémoire
    * Cet indice est celui donné par la deuxième valeur de l'instruction (B)
    * après avoir été décodé. La méthode utilise le pointeur du processus, 
    * l'instruction donnée et la taille de la mémoire. Le décodage est nécessaire
    * en raison des différents modes d'adressages.
    */
    public int decodeB(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeB()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
                return Math.floorMod(p.getPc() + instr.getB(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }

    /**
    * Cette méthode permet l'éxécution de l'instruction sur 
    * laquelle pointe le pointeur du precessus. C'est à ce 
    * moment que les différents types d'instructions sont différenciés
    */

    public void execute(Processus p) {
    Instruction instr = this.memory.read(p.getPc());

        switch(instr.getOpcode()) {
            /*
            Dans le MOV on copie 'instruction pointée par A dans à l'adresse donnée
            par B. Dans le cas ou A est immédiat, on cré un DAT contenant sa valeur pour
            remplacer l'instruction pointée par B.
            */

            case MOV: {
                int dstAddr = decodeB(p, instr);
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On crée un DAT qui transporte la valeur immédiate
                    memory.write(dstAddr, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, instr.getA()));
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
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex).copy();
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On ajoute A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() + instr.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else{
                    // On ajoute le a et le B de la source respectivement à A et B de la target
                    int sourceIndex = decodeA(p, instr);
                    Instruction source = memory.read(sourceIndex);
                    int resultA = Math.floorMod(target.getA() + source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() + source.getB(), memory.getSize());
                    target.setA(resultA);
                    target.setB(resultB);
                }
                memory.write(targetIndex, target);
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

            /*
            Si le mode de A est immédiat alors la valeur contenue dans le champ A est
            directement soustrait à la cible pointée par le champs B le A des la source
            Sinon on soustrait à la cible pointée par B le A et le B de source désignée
            */

            case SUB: {
                int sourceAddr = decodeA(p, instr);
                int targetIndex = decodeB(p, instr);
                
                Instruction source = memory.read(sourceAddr);
                Instruction target = memory.read(targetIndex).copy();

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On soustrait A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() - source.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else {
                    // Soustraction A-A et B-B
                    int resultA = Math.floorMod(target.getA() - source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() - source.getB(), memory.getSize());
                    
                    target.setA(resultA);
                    target.setB(resultB);
                }

                memory.write(targetIndex, target);
                p.advance(1, memory.getSize());
                break;
            }

            /*
            Cette instruction permet de sauter à l'adresse que pointe A si la target visée par B est nulle
            et avance juste de 1 sinon
            */
            case JMZ: {
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()==0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }

            /*
            Cette instruction est exactement le contraire de JMZ donc elle saute que si la target 
            visée par B est non nulle
            */

            case JMN:{
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()!=0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }
            /*
            Cette instruction permet de passer une instruction si un test est vérifié.
            Si le champs A est immédiat alors le test est l'égalité entre la valeur de A et la valeur du champs B pointé par B
            Sinon, le test repose sur l'égalité des valeurs et des modes entre les instruciton visée par la source et la target
            */

            case CMP:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // Comparaison valeur A vs Champ B de la cible
                    equal = (instr.getA() == target.getB());
                } 
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    
                    // Comparaison complète : Opcode + Modes + Valeurs
                    equal = (source.getOpcode() == target.getOpcode() &&
                            source.getModeA() == target.getModeA() &&
                            source.getA()     == target.getA()     &&
                            source.getModeB() == target.getModeB() &&
                            source.getB()     == target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }
            
            case SLT:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    equal = (instr.getA() < target.getB());
                }
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    equal = (source.getA() < target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }

            case DJN :{
                int targetAddr = decodeB(p, instr);
                Instruction target = memory.read(targetAddr);

                // 1. Décrémentation du champ B de la cible
                target.setB(target.getB() - 1);
                
                // Important : il faut réécrire en mémoire car on a modifié l'instruction
                memory.write(targetAddr, target);

                // 2. Test du résultat (Not Zero)
                if (target.getB() != 0) {
                    // 3. Saut vers A
                    int jumpAddr = decodeA(p, instr);
                    p.setPc(jumpAddr, memory.getSize());
                } 
                else {
                    // On continue normalement
                    p.advance(1, memory.getSize());
                }
                break;
            }

            //Dernière instriction SPL à coder en même temps que les warriors

            default:
                throw new IllegalStateException("Opcode non géré");
        }
    }


    public void executeTurn(Warrior warrior) {
        // 1. Sortir le processus de la file
        Processus p = warrior.getNextProcess();
        Instruction instr = memory.read(p.getPc());

        // 2. Vérifier si c'est un DAT
        if (instr.getOpcode() == Opcode.DAT) {
            // Le processus n'est PAS rajouté à la file. 
            // Il disparaît de la circulation.
            System.out.println("Processus mort à l'adresse " + p.getPc());
        } 
        else {
            // 3. Exécuter l'instruction (MOV, ADD, JMP, SPL, etc.)
            execute(p); 

            // 4. Le replacer en fin de file pour son prochain tour
            warrior.addProcess(p);
        }

    }
}package mars;

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
    public int decodeA(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeA()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
            return Math.floorMod(p.getPc() + instr.getA(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }
    
    /** 
    * Cette méthode permet de retourner un indice dans le mémoire
    * Cet indice est celui donné par la deuxième valeur de l'instruction (B)
    * après avoir été décodé. La méthode utilise le pointeur du processus, 
    * l'instruction donnée et la taille de la mémoire. Le décodage est nécessaire
    * en raison des différents modes d'adressages.
    */
    public int decodeB(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeB()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
                return Math.floorMod(p.getPc() + instr.getB(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }

    /**
    * Cette méthode permet l'éxécution de l'instruction sur 
    * laquelle pointe le pointeur du precessus. C'est à ce 
    * moment que les différents types d'instructions sont différenciés
    */

    public void execute(Processus p) {
    Instruction instr = this.memory.read(p.getPc());

        switch(instr.getOpcode()) {
            /*
            Dans le MOV on copie 'instruction pointée par A dans à l'adresse donnée
            par B. Dans le cas ou A est immédiat, on cré un DAT contenant sa valeur pour
            remplacer l'instruction pointée par B.
            */

            case MOV: {
                int dstAddr = decodeB(p, instr);
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On crée un DAT qui transporte la valeur immédiate
                    memory.write(dstAddr, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, instr.getA()));
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
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex).copy();
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On ajoute A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() + instr.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else{
                    // On ajoute le a et le B de la source respectivement à A et B de la target
                    int sourceIndex = decodeA(p, instr);
                    Instruction source = memory.read(sourceIndex);
                    int resultA = Math.floorMod(target.getA() + source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() + source.getB(), memory.getSize());
                    target.setA(resultA);
                    target.setB(resultB);
                }
                memory.write(targetIndex, target);
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

            /*
            Si le mode de A est immédiat alors la valeur contenue dans le champ A est
            directement soustrait à la cible pointée par le champs B le A des la source
            Sinon on soustrait à la cible pointée par B le A et le B de source désignée
            */

            case SUB: {
                int sourceAddr = decodeA(p, instr);
                int targetIndex = decodeB(p, instr);
                
                Instruction source = memory.read(sourceAddr);
                Instruction target = memory.read(targetIndex).copy();

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On soustrait A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() - source.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else {
                    // Soustraction A-A et B-B
                    int resultA = Math.floorMod(target.getA() - source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() - source.getB(), memory.getSize());
                    
                    target.setA(resultA);
                    target.setB(resultB);
                }

                memory.write(targetIndex, target);
                p.advance(1, memory.getSize());
                break;
            }

            /*
            Cette instruction permet de sauter à l'adresse que pointe A si la target visée par B est nulle
            et avance juste de 1 sinon
            */
            case JMZ: {
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()==0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }

            /*
            Cette instruction est exactement le contraire de JMZ donc elle saute que si la target 
            visée par B est non nulle
            */

            case JMN:{
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()!=0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }
            /*
            Cette instruction permet de passer une instruction si un test est vérifié.
            Si le champs A est immédiat alors le test est l'égalité entre la valeur de A et la valeur du champs B pointé par B
            Sinon, le test repose sur l'égalité des valeurs et des modes entre les instruciton visée par la source et la target
            */

            case CMP:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // Comparaison valeur A vs Champ B de la cible
                    equal = (instr.getA() == target.getB());
                } 
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    
                    // Comparaison complète : Opcode + Modes + Valeurs
                    equal = (source.getOpcode() == target.getOpcode() &&
                            source.getModeA() == target.getModeA() &&
                            source.getA()     == target.getA()     &&
                            source.getModeB() == target.getModeB() &&
                            source.getB()     == target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }
            
            case SLT:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    equal = (instr.getA() < target.getB());
                }
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    equal = (source.getA() < target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }

            case DJN :{
                int targetAddr = decodeB(p, instr);
                Instruction target = memory.read(targetAddr);

                // 1. Décrémentation du champ B de la cible
                target.setB(target.getB() - 1);
                
                // Important : il faut réécrire en mémoire car on a modifié l'instruction
                memory.write(targetAddr, target);

                // 2. Test du résultat (Not Zero)
                if (target.getB() != 0) {
                    // 3. Saut vers A
                    int jumpAddr = decodeA(p, instr);
                    p.setPc(jumpAddr, memory.getSize());
                } 
                else {
                    // On continue normalement
                    p.advance(1, memory.getSize());
                }
                break;
            }

            //Dernière instriction SPL à coder en même temps que les warriors

            default:
                throw new IllegalStateException("Opcode non géré");
        }
    }


    public void executeTurn(Warrior warrior) {
        // 1. Sortir le processus de la file
        Processus p = warrior.getNextProcess();
        Instruction instr = memory.read(p.getPc());

        // 2. Vérifier si c'est un DAT
        if (instr.getOpcode() == Opcode.DAT) {
            // Le processus n'est PAS rajouté à la file. 
            // Il disparaît de la circulation.
            System.out.println("Processus mort à l'adresse " + p.getPc());
        } 
        else {
            // 3. Exécuter l'instruction (MOV, ADD, JMP, SPL, etc.)
            execute(p); 

            // 4. Le replacer en fin de file pour son prochain tour
            warrior.addProcess(p);
        }

    }
}package mars;

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
    public int decodeA(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeA()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
            return Math.floorMod(p.getPc() + instr.getA(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }
    
    /** 
    * Cette méthode permet de retourner un indice dans le mémoire
    * Cet indice est celui donné par la deuxième valeur de l'instruction (B)
    * après avoir été décodé. La méthode utilise le pointeur du processus, 
    * l'instruction donnée et la taille de la mémoire. Le décodage est nécessaire
    * en raison des différents modes d'adressages.
    */
    public int decodeB(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeB()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
                return Math.floorMod(p.getPc() + instr.getB(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }

    /**
    * Cette méthode permet l'éxécution de l'instruction sur 
    * laquelle pointe le pointeur du precessus. C'est à ce 
    * moment que les différents types d'instructions sont différenciés
    */

    public void execute(Processus p) {
    Instruction instr = this.memory.read(p.getPc());

        switch(instr.getOpcode()) {
            /*
            Dans le MOV on copie 'instruction pointée par A dans à l'adresse donnée
            par B. Dans le cas ou A est immédiat, on cré un DAT contenant sa valeur pour
            remplacer l'instruction pointée par B.
            */

            case MOV: {
                int dstAddr = decodeB(p, instr);
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On crée un DAT qui transporte la valeur immédiate
                    memory.write(dstAddr, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, instr.getA()));
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
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex).copy();
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On ajoute A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() + instr.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else{
                    // On ajoute le a et le B de la source respectivement à A et B de la target
                    int sourceIndex = decodeA(p, instr);
                    Instruction source = memory.read(sourceIndex);
                    int resultA = Math.floorMod(target.getA() + source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() + source.getB(), memory.getSize());
                    target.setA(resultA);
                    target.setB(resultB);
                }
                memory.write(targetIndex, target);
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

            /*
            Si le mode de A est immédiat alors la valeur contenue dans le champ A est
            directement soustrait à la cible pointée par le champs B le A des la source
            Sinon on soustrait à la cible pointée par B le A et le B de source désignée
            */

            case SUB: {
                int sourceAddr = decodeA(p, instr);
                int targetIndex = decodeB(p, instr);
                
                Instruction source = memory.read(sourceAddr);
                Instruction target = memory.read(targetIndex).copy();

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On soustrait A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() - source.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else {
                    // Soustraction A-A et B-B
                    int resultA = Math.floorMod(target.getA() - source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() - source.getB(), memory.getSize());
                    
                    target.setA(resultA);
                    target.setB(resultB);
                }

                memory.write(targetIndex, target);
                p.advance(1, memory.getSize());
                break;
            }

            /*
            Cette instruction permet de sauter à l'adresse que pointe A si la target visée par B est nulle
            et avance juste de 1 sinon
            */
            case JMZ: {
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()==0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }

            /*
            Cette instruction est exactement le contraire de JMZ donc elle saute que si la target 
            visée par B est non nulle
            */

            case JMN:{
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()!=0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }
            /*
            Cette instruction permet de passer une instruction si un test est vérifié.
            Si le champs A est immédiat alors le test est l'égalité entre la valeur de A et la valeur du champs B pointé par B
            Sinon, le test repose sur l'égalité des valeurs et des modes entre les instruciton visée par la source et la target
            */

            case CMP:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // Comparaison valeur A vs Champ B de la cible
                    equal = (instr.getA() == target.getB());
                } 
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    
                    // Comparaison complète : Opcode + Modes + Valeurs
                    equal = (source.getOpcode() == target.getOpcode() &&
                            source.getModeA() == target.getModeA() &&
                            source.getA()     == target.getA()     &&
                            source.getModeB() == target.getModeB() &&
                            source.getB()     == target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }
            
            case SLT:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    equal = (instr.getA() < target.getB());
                }
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    equal = (source.getA() < target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }

            case DJN :{
                int targetAddr = decodeB(p, instr);
                Instruction target = memory.read(targetAddr);

                // 1. Décrémentation du champ B de la cible
                target.setB(target.getB() - 1);
                
                // Important : il faut réécrire en mémoire car on a modifié l'instruction
                memory.write(targetAddr, target);

                // 2. Test du résultat (Not Zero)
                if (target.getB() != 0) {
                    // 3. Saut vers A
                    int jumpAddr = decodeA(p, instr);
                    p.setPc(jumpAddr, memory.getSize());
                } 
                else {
                    // On continue normalement
                    p.advance(1, memory.getSize());
                }
                break;
            }

            //Dernière instriction SPL à coder en même temps que les warriors

            default:
                throw new IllegalStateException("Opcode non géré");
        }
    }


    public void executeTurn(Warrior warrior) {
        // 1. Sortir le processus de la file
        Processus p = warrior.getNextProcess();
        Instruction instr = memory.read(p.getPc());

        // 2. Vérifier si c'est un DAT
        if (instr.getOpcode() == Opcode.DAT) {
            // Le processus n'est PAS rajouté à la file. 
            // Il disparaît de la circulation.
            System.out.println("Processus mort à l'adresse " + p.getPc());
        } 
        else {
            // 3. Exécuter l'instruction (MOV, ADD, JMP, SPL, etc.)
            execute(p); 

            // 4. Le replacer en fin de file pour son prochain tour
            warrior.addProcess(p);
        }

    }
}package mars;

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
    public int decodeA(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeA()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
            return Math.floorMod(p.getPc() + instr.getA(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }
    
    /** 
    * Cette méthode permet de retourner un indice dans le mémoire
    * Cet indice est celui donné par la deuxième valeur de l'instruction (B)
    * après avoir été décodé. La méthode utilise le pointeur du processus, 
    * l'instruction donnée et la taille de la mémoire. Le décodage est nécessaire
    * en raison des différents modes d'adressages.
    */
    public int decodeB(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeB()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
                return Math.floorMod(p.getPc() + instr.getB(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }

    /**
    * Cette méthode permet l'éxécution de l'instruction sur 
    * laquelle pointe le pointeur du precessus. C'est à ce 
    * moment que les différents types d'instructions sont différenciés
    */

    public void execute(Processus p) {
    Instruction instr = this.memory.read(p.getPc());

        switch(instr.getOpcode()) {
            /*
            Dans le MOV on copie 'instruction pointée par A dans à l'adresse donnée
            par B. Dans le cas ou A est immédiat, on cré un DAT contenant sa valeur pour
            remplacer l'instruction pointée par B.
            */

            case MOV: {
                int dstAddr = decodeB(p, instr);
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On crée un DAT qui transporte la valeur immédiate
                    memory.write(dstAddr, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, instr.getA()));
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
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex).copy();
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On ajoute A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() + instr.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else{
                    // On ajoute le a et le B de la source respectivement à A et B de la target
                    int sourceIndex = decodeA(p, instr);
                    Instruction source = memory.read(sourceIndex);
                    int resultA = Math.floorMod(target.getA() + source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() + source.getB(), memory.getSize());
                    target.setA(resultA);
                    target.setB(resultB);
                }
                memory.write(targetIndex, target);
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

            /*
            Si le mode de A est immédiat alors la valeur contenue dans le champ A est
            directement soustrait à la cible pointée par le champs B le A des la source
            Sinon on soustrait à la cible pointée par B le A et le B de source désignée
            */

            case SUB: {
                int sourceAddr = decodeA(p, instr);
                int targetIndex = decodeB(p, instr);
                
                Instruction source = memory.read(sourceAddr);
                Instruction target = memory.read(targetIndex).copy();

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On soustrait A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() - source.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else {
                    // Soustraction A-A et B-B
                    int resultA = Math.floorMod(target.getA() - source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() - source.getB(), memory.getSize());
                    
                    target.setA(resultA);
                    target.setB(resultB);
                }

                memory.write(targetIndex, target);
                p.advance(1, memory.getSize());
                break;
            }

            /*
            Cette instruction permet de sauter à l'adresse que pointe A si la target visée par B est nulle
            et avance juste de 1 sinon
            */
            case JMZ: {
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()==0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }

            /*
            Cette instruction est exactement le contraire de JMZ donc elle saute que si la target 
            visée par B est non nulle
            */

            case JMN:{
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()!=0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }
            /*
            Cette instruction permet de passer une instruction si un test est vérifié.
            Si le champs A est immédiat alors le test est l'égalité entre la valeur de A et la valeur du champs B pointé par B
            Sinon, le test repose sur l'égalité des valeurs et des modes entre les instruciton visée par la source et la target
            */

            case CMP:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // Comparaison valeur A vs Champ B de la cible
                    equal = (instr.getA() == target.getB());
                } 
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    
                    // Comparaison complète : Opcode + Modes + Valeurs
                    equal = (source.getOpcode() == target.getOpcode() &&
                            source.getModeA() == target.getModeA() &&
                            source.getA()     == target.getA()     &&
                            source.getModeB() == target.getModeB() &&
                            source.getB()     == target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }
            
            case SLT:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    equal = (instr.getA() < target.getB());
                }
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    equal = (source.getA() < target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }

            case DJN :{
                int targetAddr = decodeB(p, instr);
                Instruction target = memory.read(targetAddr);

                // 1. Décrémentation du champ B de la cible
                target.setB(target.getB() - 1);
                
                // Important : il faut réécrire en mémoire car on a modifié l'instruction
                memory.write(targetAddr, target);

                // 2. Test du résultat (Not Zero)
                if (target.getB() != 0) {
                    // 3. Saut vers A
                    int jumpAddr = decodeA(p, instr);
                    p.setPc(jumpAddr, memory.getSize());
                } 
                else {
                    // On continue normalement
                    p.advance(1, memory.getSize());
                }
                break;
            }

            //Dernière instriction SPL à coder en même temps que les warriors

            default:
                throw new IllegalStateException("Opcode non géré");
        }
    }


    public void executeTurn(Warrior warrior) {
        // 1. Sortir le processus de la file
        Processus p = warrior.getNextProcess();
        Instruction instr = memory.read(p.getPc());

        // 2. Vérifier si c'est un DAT
        if (instr.getOpcode() == Opcode.DAT) {
            // Le processus n'est PAS rajouté à la file. 
            // Il disparaît de la circulation.
            System.out.println("Processus mort à l'adresse " + p.getPc());
        } 
        else {
            // 3. Exécuter l'instruction (MOV, ADD, JMP, SPL, etc.)
            execute(p); 

            // 4. Le replacer en fin de file pour son prochain tour
            warrior.addProcess(p);
        }

    }
}package mars;

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
    public int decodeA(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeA()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
            return Math.floorMod(p.getPc() + instr.getA(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }
    
    /** 
    * Cette méthode permet de retourner un indice dans le mémoire
    * Cet indice est celui donné par la deuxième valeur de l'instruction (B)
    * après avoir été décodé. La méthode utilise le pointeur du processus, 
    * l'instruction donnée et la taille de la mémoire. Le décodage est nécessaire
    * en raison des différents modes d'adressages.
    */
    public int decodeB(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeB()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
                return Math.floorMod(p.getPc() + instr.getB(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }

    /**
    * Cette méthode permet l'éxécution de l'instruction sur 
    * laquelle pointe le pointeur du precessus. C'est à ce 
    * moment que les différents types d'instructions sont différenciés
    */

    public void execute(Processus p) {
    Instruction instr = this.memory.read(p.getPc());

        switch(instr.getOpcode()) {
            /*
            Dans le MOV on copie 'instruction pointée par A dans à l'adresse donnée
            par B. Dans le cas ou A est immédiat, on cré un DAT contenant sa valeur pour
            remplacer l'instruction pointée par B.
            */

            case MOV: {
                int dstAddr = decodeB(p, instr);
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On crée un DAT qui transporte la valeur immédiate
                    memory.write(dstAddr, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, instr.getA()));
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
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex).copy();
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On ajoute A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() + instr.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else{
                    // On ajoute le a et le B de la source respectivement à A et B de la target
                    int sourceIndex = decodeA(p, instr);
                    Instruction source = memory.read(sourceIndex);
                    int resultA = Math.floorMod(target.getA() + source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() + source.getB(), memory.getSize());
                    target.setA(resultA);
                    target.setB(resultB);
                }
                memory.write(targetIndex, target);
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

            /*
            Si le mode de A est immédiat alors la valeur contenue dans le champ A est
            directement soustrait à la cible pointée par le champs B le A des la source
            Sinon on soustrait à la cible pointée par B le A et le B de source désignée
            */

            case SUB: {
                int sourceAddr = decodeA(p, instr);
                int targetIndex = decodeB(p, instr);
                
                Instruction source = memory.read(sourceAddr);
                Instruction target = memory.read(targetIndex).copy();

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On soustrait A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() - source.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else {
                    // Soustraction A-A et B-B
                    int resultA = Math.floorMod(target.getA() - source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() - source.getB(), memory.getSize());
                    
                    target.setA(resultA);
                    target.setB(resultB);
                }

                memory.write(targetIndex, target);
                p.advance(1, memory.getSize());
                break;
            }

            /*
            Cette instruction permet de sauter à l'adresse que pointe A si la target visée par B est nulle
            et avance juste de 1 sinon
            */
            case JMZ: {
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()==0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }

            /*
            Cette instruction est exactement le contraire de JMZ donc elle saute que si la target 
            visée par B est non nulle
            */

            case JMN:{
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()!=0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }
            /*
            Cette instruction permet de passer une instruction si un test est vérifié.
            Si le champs A est immédiat alors le test est l'égalité entre la valeur de A et la valeur du champs B pointé par B
            Sinon, le test repose sur l'égalité des valeurs et des modes entre les instruciton visée par la source et la target
            */

            case CMP:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // Comparaison valeur A vs Champ B de la cible
                    equal = (instr.getA() == target.getB());
                } 
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    
                    // Comparaison complète : Opcode + Modes + Valeurs
                    equal = (source.getOpcode() == target.getOpcode() &&
                            source.getModeA() == target.getModeA() &&
                            source.getA()     == target.getA()     &&
                            source.getModeB() == target.getModeB() &&
                            source.getB()     == target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }
            
            case SLT:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    equal = (instr.getA() < target.getB());
                }
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    equal = (source.getA() < target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }

            case DJN :{
                int targetAddr = decodeB(p, instr);
                Instruction target = memory.read(targetAddr);

                // 1. Décrémentation du champ B de la cible
                target.setB(target.getB() - 1);
                
                // Important : il faut réécrire en mémoire car on a modifié l'instruction
                memory.write(targetAddr, target);

                // 2. Test du résultat (Not Zero)
                if (target.getB() != 0) {
                    // 3. Saut vers A
                    int jumpAddr = decodeA(p, instr);
                    p.setPc(jumpAddr, memory.getSize());
                } 
                else {
                    // On continue normalement
                    p.advance(1, memory.getSize());
                }
                break;
            }

            //Dernière instriction SPL à coder en même temps que les warriors

            default:
                throw new IllegalStateException("Opcode non géré");
        }
    }


    public void executeTurn(Warrior warrior) {
        // 1. Sortir le processus de la file
        Processus p = warrior.getNextProcess();
        Instruction instr = memory.read(p.getPc());

        // 2. Vérifier si c'est un DAT
        if (instr.getOpcode() == Opcode.DAT) {
            // Le processus n'est PAS rajouté à la file. 
            // Il disparaît de la circulation.
            System.out.println("Processus mort à l'adresse " + p.getPc());
        } 
        else {
            // 3. Exécuter l'instruction (MOV, ADD, JMP, SPL, etc.)
            execute(p); 

            // 4. Le replacer en fin de file pour son prochain tour
            warrior.addProcess(p);
        }

    }
}package mars;

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
    public int decodeA(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeA()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
            return Math.floorMod(p.getPc() + instr.getA(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }
    
    /** 
    * Cette méthode permet de retourner un indice dans le mémoire
    * Cet indice est celui donné par la deuxième valeur de l'instruction (B)
    * après avoir été décodé. La méthode utilise le pointeur du processus, 
    * l'instruction donnée et la taille de la mémoire. Le décodage est nécessaire
    * en raison des différents modes d'adressages.
    */
    public int decodeB(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeB()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
                return Math.floorMod(p.getPc() + instr.getB(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }

    /**
    * Cette méthode permet l'éxécution de l'instruction sur 
    * laquelle pointe le pointeur du precessus. C'est à ce 
    * moment que les différents types d'instructions sont différenciés
    */

    public void execute(Processus p) {
    Instruction instr = this.memory.read(p.getPc());

        switch(instr.getOpcode()) {
            /*
            Dans le MOV on copie 'instruction pointée par A dans à l'adresse donnée
            par B. Dans le cas ou A est immédiat, on cré un DAT contenant sa valeur pour
            remplacer l'instruction pointée par B.
            */

            case MOV: {
                int dstAddr = decodeB(p, instr);
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On crée un DAT qui transporte la valeur immédiate
                    memory.write(dstAddr, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, instr.getA()));
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
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex).copy();
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On ajoute A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() + instr.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else{
                    // On ajoute le a et le B de la source respectivement à A et B de la target
                    int sourceIndex = decodeA(p, instr);
                    Instruction source = memory.read(sourceIndex);
                    int resultA = Math.floorMod(target.getA() + source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() + source.getB(), memory.getSize());
                    target.setA(resultA);
                    target.setB(resultB);
                }
                memory.write(targetIndex, target);
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

            /*
            Si le mode de A est immédiat alors la valeur contenue dans le champ A est
            directement soustrait à la cible pointée par le champs B le A des la source
            Sinon on soustrait à la cible pointée par B le A et le B de source désignée
            */

            case SUB: {
                int sourceAddr = decodeA(p, instr);
                int targetIndex = decodeB(p, instr);
                
                Instruction source = memory.read(sourceAddr);
                Instruction target = memory.read(targetIndex).copy();

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On soustrait A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() - source.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else {
                    // Soustraction A-A et B-B
                    int resultA = Math.floorMod(target.getA() - source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() - source.getB(), memory.getSize());
                    
                    target.setA(resultA);
                    target.setB(resultB);
                }

                memory.write(targetIndex, target);
                p.advance(1, memory.getSize());
                break;
            }

            /*
            Cette instruction permet de sauter à l'adresse que pointe A si la target visée par B est nulle
            et avance juste de 1 sinon
            */
            case JMZ: {
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()==0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }

            /*
            Cette instruction est exactement le contraire de JMZ donc elle saute que si la target 
            visée par B est non nulle
            */

            case JMN:{
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()!=0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }
            /*
            Cette instruction permet de passer une instruction si un test est vérifié.
            Si le champs A est immédiat alors le test est l'égalité entre la valeur de A et la valeur du champs B pointé par B
            Sinon, le test repose sur l'égalité des valeurs et des modes entre les instruciton visée par la source et la target
            */

            case CMP:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // Comparaison valeur A vs Champ B de la cible
                    equal = (instr.getA() == target.getB());
                } 
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    
                    // Comparaison complète : Opcode + Modes + Valeurs
                    equal = (source.getOpcode() == target.getOpcode() &&
                            source.getModeA() == target.getModeA() &&
                            source.getA()     == target.getA()     &&
                            source.getModeB() == target.getModeB() &&
                            source.getB()     == target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }
            
            case SLT:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    equal = (instr.getA() < target.getB());
                }
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    equal = (source.getA() < target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }

            case DJN :{
                int targetAddr = decodeB(p, instr);
                Instruction target = memory.read(targetAddr);

                // 1. Décrémentation du champ B de la cible
                target.setB(target.getB() - 1);
                
                // Important : il faut réécrire en mémoire car on a modifié l'instruction
                memory.write(targetAddr, target);

                // 2. Test du résultat (Not Zero)
                if (target.getB() != 0) {
                    // 3. Saut vers A
                    int jumpAddr = decodeA(p, instr);
                    p.setPc(jumpAddr, memory.getSize());
                } 
                else {
                    // On continue normalement
                    p.advance(1, memory.getSize());
                }
                break;
            }

            //Dernière instriction SPL à coder en même temps que les warriors

            default:
                throw new IllegalStateException("Opcode non géré");
        }
    }


    public void executeTurn(Warrior warrior) {
        // 1. Sortir le processus de la file
        Processus p = warrior.getNextProcess();
        Instruction instr = memory.read(p.getPc());

        // 2. Vérifier si c'est un DAT
        if (instr.getOpcode() == Opcode.DAT) {
            // Le processus n'est PAS rajouté à la file. 
            // Il disparaît de la circulation.
            System.out.println("Processus mort à l'adresse " + p.getPc());
        } 
        else {
            // 3. Exécuter l'instruction (MOV, ADD, JMP, SPL, etc.)
            execute(p); 

            // 4. Le replacer en fin de file pour son prochain tour
            warrior.addProcess(p);
        }

    }
}package mars;

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
    public int decodeA(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeA()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
            return Math.floorMod(p.getPc() + instr.getA(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }
    
    /** 
    * Cette méthode permet de retourner un indice dans le mémoire
    * Cet indice est celui donné par la deuxième valeur de l'instruction (B)
    * après avoir été décodé. La méthode utilise le pointeur du processus, 
    * l'instruction donnée et la taille de la mémoire. Le décodage est nécessaire
    * en raison des différents modes d'adressages.
    */
    public int decodeB(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeB()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
                return Math.floorMod(p.getPc() + instr.getB(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }

    /**
    * Cette méthode permet l'éxécution de l'instruction sur 
    * laquelle pointe le pointeur du precessus. C'est à ce 
    * moment que les différents types d'instructions sont différenciés
    */

    public void execute(Processus p) {
    Instruction instr = this.memory.read(p.getPc());

        switch(instr.getOpcode()) {
            /*
            Dans le MOV on copie 'instruction pointée par A dans à l'adresse donnée
            par B. Dans le cas ou A est immédiat, on cré un DAT contenant sa valeur pour
            remplacer l'instruction pointée par B.
            */

            case MOV: {
                int dstAddr = decodeB(p, instr);
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On crée un DAT qui transporte la valeur immédiate
                    memory.write(dstAddr, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, instr.getA()));
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
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex).copy();
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On ajoute A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() + instr.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else{
                    // On ajoute le a et le B de la source respectivement à A et B de la target
                    int sourceIndex = decodeA(p, instr);
                    Instruction source = memory.read(sourceIndex);
                    int resultA = Math.floorMod(target.getA() + source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() + source.getB(), memory.getSize());
                    target.setA(resultA);
                    target.setB(resultB);
                }
                memory.write(targetIndex, target);
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

            /*
            Si le mode de A est immédiat alors la valeur contenue dans le champ A est
            directement soustrait à la cible pointée par le champs B le A des la source
            Sinon on soustrait à la cible pointée par B le A et le B de source désignée
            */

            case SUB: {
                int sourceAddr = decodeA(p, instr);
                int targetIndex = decodeB(p, instr);
                
                Instruction source = memory.read(sourceAddr);
                Instruction target = memory.read(targetIndex).copy();

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On soustrait A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() - source.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else {
                    // Soustraction A-A et B-B
                    int resultA = Math.floorMod(target.getA() - source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() - source.getB(), memory.getSize());
                    
                    target.setA(resultA);
                    target.setB(resultB);
                }

                memory.write(targetIndex, target);
                p.advance(1, memory.getSize());
                break;
            }

            /*
            Cette instruction permet de sauter à l'adresse que pointe A si la target visée par B est nulle
            et avance juste de 1 sinon
            */
            case JMZ: {
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()==0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }

            /*
            Cette instruction est exactement le contraire de JMZ donc elle saute que si la target 
            visée par B est non nulle
            */

            case JMN:{
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()!=0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }
            /*
            Cette instruction permet de passer une instruction si un test est vérifié.
            Si le champs A est immédiat alors le test est l'égalité entre la valeur de A et la valeur du champs B pointé par B
            Sinon, le test repose sur l'égalité des valeurs et des modes entre les instruciton visée par la source et la target
            */

            case CMP:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // Comparaison valeur A vs Champ B de la cible
                    equal = (instr.getA() == target.getB());
                } 
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    
                    // Comparaison complète : Opcode + Modes + Valeurs
                    equal = (source.getOpcode() == target.getOpcode() &&
                            source.getModeA() == target.getModeA() &&
                            source.getA()     == target.getA()     &&
                            source.getModeB() == target.getModeB() &&
                            source.getB()     == target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }
            
            case SLT:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    equal = (instr.getA() < target.getB());
                }
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    equal = (source.getA() < target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }

            case DJN :{
                int targetAddr = decodeB(p, instr);
                Instruction target = memory.read(targetAddr);

                // 1. Décrémentation du champ B de la cible
                target.setB(target.getB() - 1);
                
                // Important : il faut réécrire en mémoire car on a modifié l'instruction
                memory.write(targetAddr, target);

                // 2. Test du résultat (Not Zero)
                if (target.getB() != 0) {
                    // 3. Saut vers A
                    int jumpAddr = decodeA(p, instr);
                    p.setPc(jumpAddr, memory.getSize());
                } 
                else {
                    // On continue normalement
                    p.advance(1, memory.getSize());
                }
                break;
            }

            //Dernière instriction SPL à coder en même temps que les warriors

            default:
                throw new IllegalStateException("Opcode non géré");
        }
    }


    public void executeTurn(Warrior warrior) {
        // 1. Sortir le processus de la file
        Processus p = warrior.getNextProcess();
        Instruction instr = memory.read(p.getPc());

        // 2. Vérifier si c'est un DAT
        if (instr.getOpcode() == Opcode.DAT) {
            // Le processus n'est PAS rajouté à la file. 
            // Il disparaît de la circulation.
            System.out.println("Processus mort à l'adresse " + p.getPc());
        } 
        else {
            // 3. Exécuter l'instruction (MOV, ADD, JMP, SPL, etc.)
            execute(p); 

            // 4. Le replacer en fin de file pour son prochain tour
            warrior.addProcess(p);
        }

    }
}package mars;

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
    public int decodeA(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeA()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
            return Math.floorMod(p.getPc() + instr.getA(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }
    
    /** 
    * Cette méthode permet de retourner un indice dans le mémoire
    * Cet indice est celui donné par la deuxième valeur de l'instruction (B)
    * après avoir été décodé. La méthode utilise le pointeur du processus, 
    * l'instruction donnée et la taille de la mémoire. Le décodage est nécessaire
    * en raison des différents modes d'adressages.
    */
    public int decodeB(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeB()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
                return Math.floorMod(p.getPc() + instr.getB(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }

    /**
    * Cette méthode permet l'éxécution de l'instruction sur 
    * laquelle pointe le pointeur du precessus. C'est à ce 
    * moment que les différents types d'instructions sont différenciés
    */

    public void execute(Processus p) {
    Instruction instr = this.memory.read(p.getPc());

        switch(instr.getOpcode()) {
            /*
            Dans le MOV on copie 'instruction pointée par A dans à l'adresse donnée
            par B. Dans le cas ou A est immédiat, on cré un DAT contenant sa valeur pour
            remplacer l'instruction pointée par B.
            */

            case MOV: {
                int dstAddr = decodeB(p, instr);
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On crée un DAT qui transporte la valeur immédiate
                    memory.write(dstAddr, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, instr.getA()));
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
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex).copy();
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On ajoute A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() + instr.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else{
                    // On ajoute le a et le B de la source respectivement à A et B de la target
                    int sourceIndex = decodeA(p, instr);
                    Instruction source = memory.read(sourceIndex);
                    int resultA = Math.floorMod(target.getA() + source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() + source.getB(), memory.getSize());
                    target.setA(resultA);
                    target.setB(resultB);
                }
                memory.write(targetIndex, target);
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

            /*
            Si le mode de A est immédiat alors la valeur contenue dans le champ A est
            directement soustrait à la cible pointée par le champs B le A des la source
            Sinon on soustrait à la cible pointée par B le A et le B de source désignée
            */

            case SUB: {
                int sourceAddr = decodeA(p, instr);
                int targetIndex = decodeB(p, instr);
                
                Instruction source = memory.read(sourceAddr);
                Instruction target = memory.read(targetIndex).copy();

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On soustrait A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() - source.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else {
                    // Soustraction A-A et B-B
                    int resultA = Math.floorMod(target.getA() - source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() - source.getB(), memory.getSize());
                    
                    target.setA(resultA);
                    target.setB(resultB);
                }

                memory.write(targetIndex, target);
                p.advance(1, memory.getSize());
                break;
            }

            /*
            Cette instruction permet de sauter à l'adresse que pointe A si la target visée par B est nulle
            et avance juste de 1 sinon
            */
            case JMZ: {
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()==0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }

            /*
            Cette instruction est exactement le contraire de JMZ donc elle saute que si la target 
            visée par B est non nulle
            */

            case JMN:{
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()!=0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }
            /*
            Cette instruction permet de passer une instruction si un test est vérifié.
            Si le champs A est immédiat alors le test est l'égalité entre la valeur de A et la valeur du champs B pointé par B
            Sinon, le test repose sur l'égalité des valeurs et des modes entre les instruciton visée par la source et la target
            */

            case CMP:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // Comparaison valeur A vs Champ B de la cible
                    equal = (instr.getA() == target.getB());
                } 
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    
                    // Comparaison complète : Opcode + Modes + Valeurs
                    equal = (source.getOpcode() == target.getOpcode() &&
                            source.getModeA() == target.getModeA() &&
                            source.getA()     == target.getA()     &&
                            source.getModeB() == target.getModeB() &&
                            source.getB()     == target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }
            
            case SLT:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    equal = (instr.getA() < target.getB());
                }
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    equal = (source.getA() < target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }

            case DJN :{
                int targetAddr = decodeB(p, instr);
                Instruction target = memory.read(targetAddr);

                // 1. Décrémentation du champ B de la cible
                target.setB(target.getB() - 1);
                
                // Important : il faut réécrire en mémoire car on a modifié l'instruction
                memory.write(targetAddr, target);

                // 2. Test du résultat (Not Zero)
                if (target.getB() != 0) {
                    // 3. Saut vers A
                    int jumpAddr = decodeA(p, instr);
                    p.setPc(jumpAddr, memory.getSize());
                } 
                else {
                    // On continue normalement
                    p.advance(1, memory.getSize());
                }
                break;
            }

            //Dernière instriction SPL à coder en même temps que les warriors

            default:
                throw new IllegalStateException("Opcode non géré");
        }
    }


    public void executeTurn(Warrior warrior) {
        // 1. Sortir le processus de la file
        Processus p = warrior.getNextProcess();
        Instruction instr = memory.read(p.getPc());

        // 2. Vérifier si c'est un DAT
        if (instr.getOpcode() == Opcode.DAT) {
            // Le processus n'est PAS rajouté à la file. 
            // Il disparaît de la circulation.
            System.out.println("Processus mort à l'adresse " + p.getPc());
        } 
        else {
            // 3. Exécuter l'instruction (MOV, ADD, JMP, SPL, etc.)
            execute(p); 

            // 4. Le replacer en fin de file pour son prochain tour
            warrior.addProcess(p);
        }

    }
}package mars;

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
    public int decodeA(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeA()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
            return Math.floorMod(p.getPc() + instr.getA(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }
    
    /** 
    * Cette méthode permet de retourner un indice dans le mémoire
    * Cet indice est celui donné par la deuxième valeur de l'instruction (B)
    * après avoir été décodé. La méthode utilise le pointeur du processus, 
    * l'instruction donnée et la taille de la mémoire. Le décodage est nécessaire
    * en raison des différents modes d'adressages.
    */
    public int decodeB(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeB()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
                return Math.floorMod(p.getPc() + instr.getB(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }

    /**
    * Cette méthode permet l'éxécution de l'instruction sur 
    * laquelle pointe le pointeur du precessus. C'est à ce 
    * moment que les différents types d'instructions sont différenciés
    */

    public void execute(Processus p) {
    Instruction instr = this.memory.read(p.getPc());

        switch(instr.getOpcode()) {
            /*
            Dans le MOV on copie 'instruction pointée par A dans à l'adresse donnée
            par B. Dans le cas ou A est immédiat, on cré un DAT contenant sa valeur pour
            remplacer l'instruction pointée par B.
            */

            case MOV: {
                int dstAddr = decodeB(p, instr);
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On crée un DAT qui transporte la valeur immédiate
                    memory.write(dstAddr, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, instr.getA()));
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
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex).copy();
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On ajoute A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() + instr.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else{
                    // On ajoute le a et le B de la source respectivement à A et B de la target
                    int sourceIndex = decodeA(p, instr);
                    Instruction source = memory.read(sourceIndex);
                    int resultA = Math.floorMod(target.getA() + source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() + source.getB(), memory.getSize());
                    target.setA(resultA);
                    target.setB(resultB);
                }
                memory.write(targetIndex, target);
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

            /*
            Si le mode de A est immédiat alors la valeur contenue dans le champ A est
            directement soustrait à la cible pointée par le champs B le A des la source
            Sinon on soustrait à la cible pointée par B le A et le B de source désignée
            */

            case SUB: {
                int sourceAddr = decodeA(p, instr);
                int targetIndex = decodeB(p, instr);
                
                Instruction source = memory.read(sourceAddr);
                Instruction target = memory.read(targetIndex).copy();

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On soustrait A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() - source.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else {
                    // Soustraction A-A et B-B
                    int resultA = Math.floorMod(target.getA() - source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() - source.getB(), memory.getSize());
                    
                    target.setA(resultA);
                    target.setB(resultB);
                }

                memory.write(targetIndex, target);
                p.advance(1, memory.getSize());
                break;
            }

            /*
            Cette instruction permet de sauter à l'adresse que pointe A si la target visée par B est nulle
            et avance juste de 1 sinon
            */
            case JMZ: {
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()==0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }

            /*
            Cette instruction est exactement le contraire de JMZ donc elle saute que si la target 
            visée par B est non nulle
            */

            case JMN:{
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()!=0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }
            /*
            Cette instruction permet de passer une instruction si un test est vérifié.
            Si le champs A est immédiat alors le test est l'égalité entre la valeur de A et la valeur du champs B pointé par B
            Sinon, le test repose sur l'égalité des valeurs et des modes entre les instruciton visée par la source et la target
            */

            case CMP:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // Comparaison valeur A vs Champ B de la cible
                    equal = (instr.getA() == target.getB());
                } 
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    
                    // Comparaison complète : Opcode + Modes + Valeurs
                    equal = (source.getOpcode() == target.getOpcode() &&
                            source.getModeA() == target.getModeA() &&
                            source.getA()     == target.getA()     &&
                            source.getModeB() == target.getModeB() &&
                            source.getB()     == target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }
            
            case SLT:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    equal = (instr.getA() < target.getB());
                }
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    equal = (source.getA() < target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }

            case DJN :{
                int targetAddr = decodeB(p, instr);
                Instruction target = memory.read(targetAddr);

                // 1. Décrémentation du champ B de la cible
                target.setB(target.getB() - 1);
                
                // Important : il faut réécrire en mémoire car on a modifié l'instruction
                memory.write(targetAddr, target);

                // 2. Test du résultat (Not Zero)
                if (target.getB() != 0) {
                    // 3. Saut vers A
                    int jumpAddr = decodeA(p, instr);
                    p.setPc(jumpAddr, memory.getSize());
                } 
                else {
                    // On continue normalement
                    p.advance(1, memory.getSize());
                }
                break;
            }

            //Dernière instriction SPL à coder en même temps que les warriors

            default:
                throw new IllegalStateException("Opcode non géré");
        }
    }


    public void executeTurn(Warrior warrior) {
        // 1. Sortir le processus de la file
        Processus p = warrior.getNextProcess();
        Instruction instr = memory.read(p.getPc());

        // 2. Vérifier si c'est un DAT
        if (instr.getOpcode() == Opcode.DAT) {
            // Le processus n'est PAS rajouté à la file. 
            // Il disparaît de la circulation.
            System.out.println("Processus mort à l'adresse " + p.getPc());
        } 
        else {
            // 3. Exécuter l'instruction (MOV, ADD, JMP, SPL, etc.)
            execute(p); 

            // 4. Le replacer en fin de file pour son prochain tour
            warrior.addProcess(p);
        }

    }
}package mars;

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
    public int decodeA(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeA()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
            return Math.floorMod(p.getPc() + instr.getA(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getA(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }
    
    /** 
    * Cette méthode permet de retourner un indice dans le mémoire
    * Cet indice est celui donné par la deuxième valeur de l'instruction (B)
    * après avoir été décodé. La méthode utilise le pointeur du processus, 
    * l'instruction donnée et la taille de la mémoire. Le décodage est nécessaire
    * en raison des différents modes d'adressages.
    */
    public int decodeB(Processus p, Instruction instr) {
        int memorySize = this.memory.getSize();
        switch(instr.getModeB()) {
            case IMMEDIATE:{
                return p.getPc(); // ici il faut mettre une exeption à la place car on y entre jamais
            }
            case DIRECT:{
                return Math.floorMod(p.getPc() + instr.getB(), memorySize);
            }
            case INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction target = this.memory.read(addr);
                return Math.floorMod(addr + target.getB(), memorySize);
            }
            case PREDECREMENT_INDIRECT:{
                int addr = Math.floorMod(p.getPc() + instr.getB(), memorySize);
                Instruction copy = this.memory.read(addr).copy();
                copy.setB(Math.floorMod(copy.getB()-1, memorySize));
                memory.write(addr, copy);
                return Math.floorMod(addr + copy.getB(), memorySize);
            }
        }
        return 1;
    }

    /**
    * Cette méthode permet l'éxécution de l'instruction sur 
    * laquelle pointe le pointeur du precessus. C'est à ce 
    * moment que les différents types d'instructions sont différenciés
    */

    public void execute(Processus p) {
    Instruction instr = this.memory.read(p.getPc());

        switch(instr.getOpcode()) {
            /*
            Dans le MOV on copie 'instruction pointée par A dans à l'adresse donnée
            par B. Dans le cas ou A est immédiat, on cré un DAT contenant sa valeur pour
            remplacer l'instruction pointée par B.
            */

            case MOV: {
                int dstAddr = decodeB(p, instr);
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On crée un DAT qui transporte la valeur immédiate
                    memory.write(dstAddr, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, instr.getA()));
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
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex).copy();
                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On ajoute A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() + instr.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else{
                    // On ajoute le a et le B de la source respectivement à A et B de la target
                    int sourceIndex = decodeA(p, instr);
                    Instruction source = memory.read(sourceIndex);
                    int resultA = Math.floorMod(target.getA() + source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() + source.getB(), memory.getSize());
                    target.setA(resultA);
                    target.setB(resultB);
                }
                memory.write(targetIndex, target);
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

            /*
            Si le mode de A est immédiat alors la valeur contenue dans le champ A est
            directement soustrait à la cible pointée par le champs B le A des la source
            Sinon on soustrait à la cible pointée par B le A et le B de source désignée
            */

            case SUB: {
                int sourceAddr = decodeA(p, instr);
                int targetIndex = decodeB(p, instr);
                
                Instruction source = memory.read(sourceAddr);
                Instruction target = memory.read(targetIndex).copy();

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // On soustrait A de la source au champ B de la cible
                    int resultB = Math.floorMod(target.getB() - source.getA(), memory.getSize());
                    target.setB(resultB);
                } 
                else {
                    // Soustraction A-A et B-B
                    int resultA = Math.floorMod(target.getA() - source.getA(), memory.getSize());
                    int resultB = Math.floorMod(target.getB() - source.getB(), memory.getSize());
                    
                    target.setA(resultA);
                    target.setB(resultB);
                }

                memory.write(targetIndex, target);
                p.advance(1, memory.getSize());
                break;
            }

            /*
            Cette instruction permet de sauter à l'adresse que pointe A si la target visée par B est nulle
            et avance juste de 1 sinon
            */
            case JMZ: {
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()==0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }

            /*
            Cette instruction est exactement le contraire de JMZ donc elle saute que si la target 
            visée par B est non nulle
            */

            case JMN:{
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);
                if(target.getB()!=0){
                    int sourceAddr = decodeA(p, instr);
                    p.setPc(sourceAddr,memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break;
            }
            /*
            Cette instruction permet de passer une instruction si un test est vérifié.
            Si le champs A est immédiat alors le test est l'égalité entre la valeur de A et la valeur du champs B pointé par B
            Sinon, le test repose sur l'égalité des valeurs et des modes entre les instruciton visée par la source et la target
            */

            case CMP:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    // Comparaison valeur A vs Champ B de la cible
                    equal = (instr.getA() == target.getB());
                } 
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    
                    // Comparaison complète : Opcode + Modes + Valeurs
                    equal = (source.getOpcode() == target.getOpcode() &&
                            source.getModeA() == target.getModeA() &&
                            source.getA()     == target.getA()     &&
                            source.getModeB() == target.getModeB() &&
                            source.getB()     == target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }
            
            case SLT:{
                boolean equal = false;
                int targetIndex = decodeB(p, instr);
                Instruction target = memory.read(targetIndex);

                if (instr.getModeA() == Mode.IMMEDIATE) {
                    equal = (instr.getA() < target.getB());
                }
                else {
                    int sourceAddr = decodeA(p, instr);
                    Instruction source = memory.read(sourceAddr);
                    equal = (source.getA() < target.getB());
                }
                if(equal){
                    p.advance(2, memory.getSize());
                }
                else{
                    p.advance(1, memory.getSize());
                }
                break; //important dans la fin d'un switch
            }

            case DJN :{
                int targetAddr = decodeB(p, instr);
                Instruction target = memory.read(targetAddr);

                // 1. Décrémentation du champ B de la cible
                target.setB(target.getB() - 1);
                
                // Important : il faut réécrire en mémoire car on a modifié l'instruction
                memory.write(targetAddr, target);

                // 2. Test du résultat (Not Zero)
                if (target.getB() != 0) {
                    // 3. Saut vers A
                    int jumpAddr = decodeA(p, instr);
                    p.setPc(jumpAddr, memory.getSize());
                } 
                else {
                    // On continue normalement
                    p.advance(1, memory.getSize());
                }
                break;
            }

            //Dernière instriction SPL à coder en même temps que les warriors

            default:
                throw new IllegalStateException("Opcode non géré");
        }
    }


    public void executeTurn(Warrior warrior) {
        // 1. Sortir le processus de la file
        Processus p = warrior.getNextProcess();
        Instruction instr = memory.read(p.getPc());

        // 2. Vérifier si c'est un DAT
        if (instr.getOpcode() == Opcode.DAT) {
            // Le processus n'est PAS rajouté à la file. 
            // Il disparaît de la circulation.
            System.out.println("Processus mort à l'adresse " + p.getPc());
        } 
        else {
            // 3. Exécuter l'instruction (MOV, ADD, JMP, SPL, etc.)
            execute(p); 

            // 4. Le replacer en fin de file pour son prochain tour
            warrior.addProcess(p);
        }

    }
}
