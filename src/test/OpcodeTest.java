package test;

import mars.*;
import redcode.*;

public class OpcodeTest{

    public static void main(String[] args) {
        runTests();
    }
    
    public static void runTests() {
    Memory memory = new Memory(100);
    Mars mars = new Mars(memory);
    
    System.out.println("=== DÉBUT DES TESTS D'OPCODES ===");

    // --- TEST ADD ---
    // Case 20 contient DAT 0, 10. Instruction : ADD #5, 20
    // Résultat attendu : Case 20 devient DAT 0, 15
    setupMem(memory, 20, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, 10));
    executeSingle(mars, memory, 10, new Instruction(Opcode.ADD, Mode.IMMEDIATE, 5, Mode.DIRECT, 10));
    assertB(memory, 20, 15, "ADD (Immédiat)");

    // --- TEST SUB ---
    // Case 30 contient DAT 0, 10. Instruction : SUB #3, 30
    // Résultat attendu : Case 30 devient DAT 0, 7
    setupMem(memory, 30, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, 10));
    executeSingle(mars, memory, 11, new Instruction(Opcode.SUB, Mode.IMMEDIATE, 3, Mode.DIRECT, 19));
    assertB(memory, 30, 7, "SUB (Immédiat)");

    // --- TEST DJN (Decremente et Saute si non nul) ---
    // Case 40 contient DAT 0, 1. Instruction à l'adresse 50 : DJN -10, 40
    // 1. 40 devient 0. 2. Comme c'est 0, on ne saute pas. PC devient 51.
    setupMem(memory, 40, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, 1));
    Processus p1 = new Processus(50);
    Instruction djn = new Instruction(Opcode.DJN, Mode.DIRECT, -10, Mode.DIRECT, -10);
    memory.write(50, djn);
    mars.execute(p1);
    assertEquals(51, p1.getPc(), "DJN (Cas vers 0 : Pas de saut)");
    assertB(memory, 40, 0, "DJN (Décrémentation)");

    // --- TEST CMP (Compare et Saute l'instruction suivante si égal) ---
    // Instruction : CMP #10, 60. Case 60 contient DAT 0, 10.
    // Résultat : PC doit passer de 70 à 72 (skip).
    setupMem(memory, 60, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, 10));
    Processus p2 = new Processus(70);
    memory.write(70, new Instruction(Opcode.CMP, Mode.IMMEDIATE, 10, Mode.DIRECT, -10));
    mars.execute(p2);
    assertEquals(72, p2.getPc(), "CMP (Égalité : Skip)");

        // --- TEST JMZ (Jump if Zero) ---
    // Si la cible pointée par B est 0, on saute à l'adresse A.
    // Case 20: DAT 0, 0. Instruction à 10: JMZ 50, 20 (PC relatif 10+10)
    setupMem(memory, 20, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, 0));
    Processus p3 = new Processus(10);
    // JMZ vers 50 (A=40 car 10+40=50) si la case 20 (B=10 car 10+10=20) est à zéro
    Instruction jmz = new Instruction(Opcode.JMZ, Mode.DIRECT, 40, Mode.DIRECT, 10);
    memory.write(10, jmz);
    mars.execute(p3);
    assertEquals(50, p3.getPc(), "JMZ (Saut car B est zéro)");

    // --- TEST JMN (Jump if Not Zero) ---
    // Si la cible pointée par B n'est pas 0, on saute.
    // Case 30: DAT 0, 5. Instruction à 25: JMN 10, 30 (PC relatif 25+5)
    setupMem(memory, 30, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, 5));
    Processus p4 = new Processus(25);
    // JMN vers 10 (A=-15) si la case 30 (B=5) est non-nulle
    Instruction jmn = new Instruction(Opcode.JMN, Mode.DIRECT, -15, Mode.DIRECT, 5);
    memory.write(25, jmn);
    mars.execute(p4);
    assertEquals(10, p4.getPc(), "JMN (Saut car B est non-nul)");

    // --- TEST SLT (Skip if Less Than) ---
    // Saute l'instruction suivante (PC + 2) si A < B_de_cible.
    // Instruction à 60: SLT #10, 70. Case 70: DAT 0, 20.
    // Comme 10 < 20, le PC doit devenir 62.
    setupMem(memory, 70, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, 20));
    Processus p5 = new Processus(60);
    Instruction slt = new Instruction(Opcode.SLT, Mode.IMMEDIATE, 10, Mode.DIRECT, 10);
    memory.write(60, slt);
    mars.execute(p5);
    assertEquals(62, p5.getPc(), "SLT (Saut d'instruction car 10 < 20)");

    // Test SLT (Cas faux)
    // Même config mais A=30. 30 < 20 est faux. PC doit devenir 61.
    Processus p6 = new Processus(80);
    memory.write(80, new Instruction(Opcode.SLT, Mode.IMMEDIATE, 30, Mode.DIRECT, 10)); // Cible 90
    setupMem(memory, 90, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, 20));
    mars.execute(p6);
    assertEquals(81, p6.getPc(), "SLT (Pas de saut car 30 n'est pas < 20)");
        
    System.out.println("=== FIN DES TESTS D'OPCODES ===");
    }

    // Utilitaires de test
    private static void setupMem(Memory m, int addr, Instruction instr) {
        m.write(addr, instr);
    }

    private static void executeSingle(Mars mars, Memory mem, int pc, Instruction instr) {
        mem.write(pc, instr);
        mars.execute(new Processus(pc));
    }

    private static void assertB(Memory m, int addr, int expected, String label) {
        int actual = m.read(addr).getB();
        if (actual == expected) System.out.println(label + " : OK");
        else System.err.println(label + " : ÉCHEC (Attendu " + expected + ", reçu " + actual + ")");
    }

    private static void assertEquals(int exp, int act, String label) {
        if (exp == act) System.out.println(label + " : OK");
        else System.err.println(label + " : ÉCHEC (Attendu " + exp + ", reçu " + act + ")");
    }
}