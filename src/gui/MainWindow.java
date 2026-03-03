/* 

package gui;

import javax.swing.*;
import java.awt.*;

import mars.Mars;
import mars.Memory;
import mars.Processus;
import redcode.Instruction;
import redcode.Opcode;
import redcode.Mode;
import mars.Warrior;

public class MainWindow extends JFrame {

    private Mars mars;
    private Processus processus;
    private Memory memory;
    private JPanel memoryPanel;
    private JLabel[] cells;

    

    public MainWindow() {

        // === Reprendre ce que Demo faisait ===
memory = new Memory((10));

memory.write(0, new Instruction(
        Opcode.MOV,
        Mode.IMMEDIATE, 2,
        Mode.DIRECT, 2));

memory.write(1, new Instruction(
        Opcode.DAT,
        Mode.DIRECT, 2,
        Mode.DIRECT, 3));

memory.write(2, new Instruction(
        Opcode.ADD,
        Mode.IMMEDIATE, 1,
        Mode.DIRECT, 3));

memory.write(3, new Instruction(
        Opcode.JMP,
        Mode.DIRECT, -2,
        Mode.DIRECT, 1));


        processus = new Processus(0);
        mars = new Mars(memory);

        // === Configuration fenêtre ===
        setTitle("Corewar Simulator");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // === Zone mémoire ===
       memoryPanel = new JPanel();
       memoryPanel.setLayout(new GridLayout(2, 5)); // 2 lignes, 5 colonnes (pour 10 cases)

        cells = new JLabel[memory.getSize()];

        for (int i = 0; i < memory.getSize(); i++) {
        cells[i] = new JLabel("", SwingConstants.CENTER);
        cells[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
        cells[i].setOpaque(true);
        memoryPanel.add(cells[i]);

        }

add(memoryPanel, BorderLayout.CENTER);


        // === Bouton STEP ===
        JButton stepButton = new JButton("Step");
        stepButton.addActionListener(e -> {
        if (processus.isAlive()) {
        mars.execute(processus);
        refreshMemory();
        } 
        else {
        JOptionPane.showMessageDialog(this, "Processus terminé !");
        }
        });




        add(stepButton, BorderLayout.SOUTH);

        refreshMemory();
    }

private void refreshMemory() {

    for (int i = 0; i < memory.getSize(); i++) {

        Instruction instr = memory.read(i);

        cells[i].setText(instr.getOpcode().toString());

        if (i == processus.getPc()) {
            cells[i].setBackground(Color.YELLOW);
        } else {
            cells[i].setBackground(Color.WHITE);
        }
    }
    memoryPanel.revalidate();
    memoryPanel.repaint();

}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainWindow().setVisible(true);
        });
    }
}

*/