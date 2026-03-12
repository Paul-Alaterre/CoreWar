package gui;

import game.CorewarGame;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import mars.Processus;
import mars.Warrior;

public class MemoryPanel extends JPanel{
    private CorewarGame game; // Pour accéder aux processus des Warriors
    private final int CELL_SIZE = 8; // Taille d'une case en pixels
    private final int COLUMNS = 80;
    private Set<Integer> visitedCells = new HashSet<>();

    public MemoryPanel(CorewarGame game){
        this.game = game;
        int rows = game.getMemory().getSize() / COLUMNS;
        setPreferredSize(new Dimension(COLUMNS * CELL_SIZE, rows * CELL_SIZE));
    }

    public void clearTraces() {
    visitedCells.clear();
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (Warrior w : game.getWarriors()) {
        g.setColor(getWarriorColor(w.getId())); // Une couleur unique par guerrier
        
            for (Processus p : w.getProcessQueue()) {
                int ip = p.getPc();
                int x = (ip % COLUMNS) * CELL_SIZE;
                int y = (ip / COLUMNS) * CELL_SIZE;

                // Dessiner un carré plein et brillant
                g.fillRect(x, y, CELL_SIZE - 1, CELL_SIZE - 1);
                
                // Optionnel : un petit contour blanc pour faire "briller" le processus
                g.setColor(Color.WHITE);
                g.drawRect(x, y, CELL_SIZE - 1, CELL_SIZE - 1);
                g.setColor(getWarriorColor(w.getId())); // On repasse à la couleur du guerrier
            }
        }

        for (Warrior w : game.getWarriors()) {
            for (Processus p : w.getProcessQueue()) {
                 visitedCells.add(p.getPc());

                g.setColor(new Color(120, 200, 255)); // bleu ciel
                 for (int addr : visitedCells) {
                    int x = (addr % COLUMNS) * CELL_SIZE;
                    int y = (addr / COLUMNS) * CELL_SIZE;
                    g.fillRect(x, y, CELL_SIZE - 1, CELL_SIZE - 1);
                }
 
            }
}

        
    }

    private Color getWarriorColor(int id) {
        switch(id) {
            case 0: return Color.GREEN;
            case 1: return Color.MAGENTA;
            case 2: return Color.CYAN;
            default: return Color.ORANGE;
        }
    }

    public void updateView() {
        repaint();
    }

}

