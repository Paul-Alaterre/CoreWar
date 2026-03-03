package application;

import mars.Memory;
import mars.Warrior;
import redcode.Instruction;
import redcode.Opcode;
import game.CorewarGame;
import redcode.Mode;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import gui.MemoryPanel;

public class MyApp {

    public static void main(String[] args) {

        Memory memory = new Memory(1000);
        List<Instruction> codeInfini = new ArrayList<>();
        // MOV 0, 1  : Copie cette instruction (JMP) à la case suivante
        codeInfini.add(new Instruction(Opcode.MOV, Mode.DIRECT, 0, Mode.DIRECT, 1)); 
        // JMP 1     : Saute sur la copie qu'on vient de créer
        codeInfini.add(new Instruction(Opcode.JMP, Mode.DIRECT, 1, Mode.DIRECT, 0)); 

        Warrior walker = new Warrior(codeInfini, 1);
        CorewarGame game = new CorewarGame(memory, 2000);
        game.loadWarrior(walker, 0);

        // 2. Initialisation de l'interface
        JFrame frame = new JFrame("Corewar Test View");
        MemoryPanel view = new MemoryPanel(game);
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(view); // Ajoute le panel au centre
        frame.pack();    // Ajuste la taille de la fenêtre au panel (si preferredSize est défini)
        frame.setSize(600, 400); // Taille de secours
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        for(int i=0;i<1000;i++){
            try {
                // Pause de 2 secondes (2000 millisecondes)
                Thread.sleep(20);
            } catch (InterruptedException e) {
                // Gestion de l'erreur si la pause est interrompue
                e.printStackTrace();
            }
            game.step();
            view.repaint();
        }
    }   
}