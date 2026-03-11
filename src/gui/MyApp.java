package application;

import game.CorewarGame;
import gui.MemoryPanel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import mars.Memory;
import mars.Warrior;
import redcode.Instruction;
import redcode.Mode;
import redcode.Opcode;

public class MyApp {

    public static void main(String[] args) {

        Memory memory = new Memory(1000);
        List<Instruction> codeInfini = new ArrayList<>();

        codeInfini.add(new Instruction(Opcode.MOV, Mode.DIRECT, 0, Mode.DIRECT, 1));
        codeInfini.add(new Instruction(Opcode.JMP, Mode.DIRECT, 1, Mode.DIRECT, 0));

        Warrior walker = new Warrior(codeInfini, 1);
        CorewarGame game = new CorewarGame(memory, 2000);
        game.loadWarrior(walker, 0);

        // --- Fenêtre principale ---
        JFrame frame = new JFrame("Corewar Test View");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // --- Barre de boutons ---
        JPanel topBar = new JPanel();
        topBar.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        topBar.setBackground(new Color(245, 245, 245)); // gris clair moderne

        JButton playBtn = new JButton("▶/⏸");
        JButton stepBtn = new JButton("⏭");
        JButton resetBtn = new JButton("⟲");
        JSlider speedSlider = new JSlider(1, 60, 10);

        topBar.add(playBtn);
        topBar.add(stepBtn);
        topBar.add(resetBtn);
        topBar.add(speedSlider);
       

        // Ligne de séparation
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(200, 200, 200)));


       
        speedSlider.setPreferredSize(new Dimension(120, 40));
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);


        // --- Zone mémoire ---
        MemoryPanel view = new MemoryPanel(game);
        view.setBackground(new Color(20, 20, 10)); // fond sombre


        frame.setLayout(new BorderLayout());
        frame.add(topBar, BorderLayout.NORTH);
        frame.add(view, BorderLayout.CENTER);

        frame.pack();
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // --- Boucle d'exécution ---

        Timer timer = new Timer(20, e -> {
        game.step();
        view.repaint();
        });

        playBtn.addActionListener(e -> {
            if (timer.isRunning()) timer.stop();
            else timer.start();
        });

        stepBtn.addActionListener(e -> {
            timer.stop();
            game.step();
            view.repaint();
        });

        resetBtn.addActionListener(e -> {
            timer.stop();
            game.reset();   // À ajouter dans CorewarGame
            view.clearTraces();
            view.repaint();
        });

        speedSlider.addChangeListener(e -> {
            int speed = speedSlider.getValue();
            timer.setDelay(1000 / speed); // vitesse ajustable
        });

        


    }
}
