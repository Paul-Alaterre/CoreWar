package test;

import mars.*;
import redcode.*;

public class AdressageTest {

    public static void main(String[] args) {
        runTests();
    }

    public static void runTests() {
        int memSize = 100;
        Memory memory = new Memory(memSize);
        Mars mars = new Mars(memory);
        
        System.out.println("=== DÉBUT DES TESTS DÉCOUPLÉS (decodeA) ===");

        // --- CAS 1 : MODE DIRECT (Relatif simple) ---
        // PC = 10, Valeur A = 5 -> Cible attendue = 15
        testDecode(mars, 10, Opcode.MOV, Mode.DIRECT, 5, 15, "Direct Positif");

        // --- CAS 2 : MODE DIRECT (Négatif / Modulo) ---
        // PC = 10, Valeur A = -15 -> Cible attendue = 95 (10 - 15 = -5, -5 mod 100 = 95)
        testDecode(mars, 10, Opcode.MOV, Mode.DIRECT, -15, 95, "Direct Négatif (Modulo)");

        // --- CAS 3 : MODE INDIRECT ---
        // On place un pointeur à l'adresse 20 qui dit "va 5 cases plus loin"
        // PC = 15, Valeur A = 5 (pointe vers adresse 20). 
        // L'instruction à l'adresse 20 a un champ B = 10.
        // Résultat attendu : 20 + 10 = 30
        memory.write(20, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, 10));
        testDecode(mars, 15, Opcode.MOV, Mode.INDIRECT, 5, 30, "Indirect (@)");

        // --- CAS 4 : MODE PRÉ-DÉCRÉMENT INDIRECT ---
        // Même principe, mais le champ B de la cible doit baisser AVANT
        // PC = 40, Valeur A = 10 (pointe vers adresse 50).
        // L'instruction à l'adresse 50 a un champ B = 5.
        // Décrément : 5 -> 4. Résultat attendu : 50 + 4 = 54
        memory.write(50, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, 5));
        testDecode(mars, 40, Opcode.MOV, Mode.PREDECREMENT_INDIRECT, 10, 54, "Pré-décrément Indirect (<)");
        
        // Vérification de l'effet de bord (la mémoire a-t-elle bien été modifiée ?)
        if (memory.read(50).getB() == 4) {
            System.out.println("Effet de bord Pré-décrément : OK");
        } else {
            System.err.println("Effet de bord Pré-décrément : ÉCHEC (B vaut toujours " + memory.read(50).getB() + ")");
        }

        System.out.println("=== DÉBUT DES TESTS DÉCOUPLÉS (decodeB) ===");

        // --- CAS 1 : MODE DIRECT (Relatif simple) ---
        // PC = 50, Valeur B = 20 -> Cible attendue = 70
        testDecodeB(mars, 50, Opcode.MOV, Mode.DIRECT, 20, 70, "Direct Positif B");

        // --- CAS 2 : MODE DIRECT (Négatif avec passage du zéro) ---
        // PC = 5, Valeur B = -10 -> Cible attendue = 95 (5 - 10 = -5 -> 95)
        testDecodeB(mars, 5, Opcode.MOV, Mode.DIRECT, -10, 95, "Direct Négatif B");

        // --- CAS 3 : MODE INDIRECT (@) ---
        // PC = 30, Valeur B = 10 (pointe vers adresse 40).
        // L'instruction à l'adresse 40 a un champ B = -5.
        // Résultat attendu : 40 + (-5) = 35
        memory.write(40, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, -5));
        testDecodeB(mars, 30, Opcode.MOV, Mode.INDIRECT, 10, 35, "Indirect B (@)");

        // --- CAS 4 : MODE PRÉ-DÉCRÉMENT INDIRECT (<) ---
        // PC = 80, Valeur B = 15 (pointe vers adresse 95).
        // L'instruction à l'adresse 95 a un champ B = 0.
        // 1. Décrémentation de 95 : 0 -> -1 (donc 99 en modulo)
        // 2. Calcul adresse finale : 95 + 99 = 194 -> Modulo 100 = 94
        memory.write(95, new Instruction(Opcode.DAT, Mode.DIRECT, 0, Mode.DIRECT, 0));
        testDecodeB(mars, 80, Opcode.MOV, Mode.PREDECREMENT_INDIRECT, 15, 94, "Pré-décrément Indirect B (<)");

        // Vérification de l'effet de bord sur la case 95
        if (memory.read(95).getB() == 99 || memory.read(95).getB() == -1) {
            System.out.println("Effet de bord B : OK (Valeur décrémentée)");
        } else {
            System.err.println("Effet de bord B : ÉCHEC (Valeur actuelle: " + memory.read(95).getB() + ")");
        }

        System.out.println("=== FIN DES TESTS B ===");
    }

    private static void testDecode(Mars mars, int pc, Opcode op, Mode modeA, int valA, int expected, String label) {
        Processus p = new Processus(pc);
        Instruction instr = new Instruction(op, modeA, valA, Mode.DIRECT, 0);
        
        try {
            // Utilise la réflexion pour accéder à la méthode privée ou change-la en 'protected'
            // Pour le test, on assume que tu as une méthode accessible ou que tu testes via execute
            int result = mars.decodeA(p, instr); 
            
            if (result == expected) {
                System.out.println(" : Passé (" + result + ")");
            } else {
                System.err.println(" : ERREUR (Attendu: " + expected + ", Reçu: " + result + ")");
            }
        } catch (Exception e) {
            System.err.println(" : CRASH (" + e.getMessage() + ")");
        }
    }

    private static void testDecodeB(Mars mars, int pc, Opcode op, Mode modeB, int valB, int expected, String label) {
        Processus p = new Processus(pc);
        // On met le mode/valeur en B, le A importe peu ici
        Instruction instr = new Instruction(op, Mode.DIRECT, 0, modeB, valB);
        
        // On suppose que tu as rendu decodeB accessible pour le test
        int result = mars.decodeB(p, instr); 
        
        if (result == expected) {
            System.out.println(" : Passé (" + result + ")");
        } else {
            System.err.println(" : ERREUR (Attendu: " + expected + ", Reçu: " + result + ")");
        }
    }
}