/* 
 * Copyright (C) 2017 Otso Nuortimo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jyendor;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;

/**
 *
 * @author otso
 */
class Painter extends JPanel
{

    private final String TITLE = "Draughts";
    private final String YOUR_TURN_TITLE = "Draughts - your turn";
    private final String OPPONENT_TURN_TITLE = "Draughts - opponent's turn";
    private JTextArea text = new JTextArea();
    private JScrollPane scrollPane = new JScrollPane(text);
    private JButton turnButton = new JButton("Confirm movement / End turn");
    public static final int CELL_SIZE = 80;
    private final int PIECE_SIZE = 60;
    private final int TEXT_AREA_HEIGHT = 80;
    private final int TURN_BUTTON_HEIGHT = 40;
    private final JFrame frame;
    private final int BOARD_SIZE;
    private int[] pieces;
    private boolean highlightedCells[];

    private final Color DARK_CELL_COLOR = Color.decode("#60341f");
    private final Color LIGHT_CELL_COLOR = Color.decode("#c9b898");
    private final Color PLAYER_ONE_COLOR = Color.decode("#d6b18b");
    private final Color PLAYER_TWO_COLOR = Color.decode("#281413");
    private final Color SELECTED_CELL_COLOR = Color.RED;
    private final Color PATH_COLOR = Color.YELLOW;

    private int selected_cell_border_width = 3;

    private final Font font = new Font("Verdana", Font.BOLD, 40);
    private final Font textAreaFont = new Font("Consolas", Font.PLAIN, 14);

    private String waitingString = "Waiting for another player";
    private String unableToCommunicateWithOpponentString = "Unable to communicate with opponent";
    private String wonString = "You won!";
    private String opponentWonString = "Opponent won!";
    private String tieString = "Game ended in a tie";

    private Game game;

    public Painter(int boardSize, int[] pieces, Game game)
    {
        this.BOARD_SIZE = boardSize;
        this.pieces = pieces;
        this.game = game;
        addMouseListener(game);
        highlightedCells = new boolean[BOARD_SIZE * BOARD_SIZE];
        this.setPreferredSize(new Dimension(boardSize * CELL_SIZE, boardSize * CELL_SIZE + 27));
        setFocusable(true);
        requestFocus();
        setBackground(LIGHT_CELL_COLOR);

        scrollPane.setPreferredSize(new Dimension(boardSize * CELL_SIZE, TEXT_AREA_HEIGHT));
        text.setLineWrap(true);
        text.setEditable(false);

        Border border = BorderFactory.createLineBorder(Color.BLACK);
        text.setBorder(border);
        text.setFont(textAreaFont);

        turnButton.setPreferredSize(new Dimension(boardSize * CELL_SIZE, TURN_BUTTON_HEIGHT));

        frame = new JFrame(TITLE);
        frame.setLayout(new BorderLayout());
        frame.add(this, BorderLayout.CENTER);
        frame.add(scrollPane, BorderLayout.NORTH);
        frame.add(turnButton, BorderLayout.SOUTH);
        //frame.setContentPane(contentPanel);
        frame.setSize(boardSize * CELL_SIZE, boardSize * CELL_SIZE + 27 + TEXT_AREA_HEIGHT + TURN_BUTTON_HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    @Override
    public void paintComponent(Graphics g)
    {

        g.clearRect(0, 0, getWidth(), getHeight());
        // Draw board
        for (int x = 0; x < BOARD_SIZE; x++)
        {
            for (int y = 0; y < BOARD_SIZE; y++)
            {
                if (x % 2 == 0 && y % 2 != 0 || x % 2 == 1 && y % 2 != 1)
                {
                    g.setColor(DARK_CELL_COLOR);
                    g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }
        if (game.isAccepted())
        {
            if (game.isYourTurn())
            {
                frame.setTitle(YOUR_TURN_TITLE);
                if (game.isPieceSelected())
                {
                    g.setColor(SELECTED_CELL_COLOR);
                    int x = game.getSelectedPiece() % BOARD_SIZE;
                    int y = (int) game.getSelectedPiece() / BOARD_SIZE;
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setStroke(new BasicStroke(selected_cell_border_width));
                    g.drawRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);

                    g.setColor(PATH_COLOR);
                    for (Integer cell : game.getPath())
                    {
                        if (cell != game.getSelectedPiece())
                        {
                            x = cell % BOARD_SIZE;
                            y = (int) cell / BOARD_SIZE;
                            g.drawRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                        }
                    }
                }
            } else
            {
                frame.setTitle(OPPONENT_TURN_TITLE);
            }
            // Draw pieces
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int x = 0; x < BOARD_SIZE; x++)
            {
                for (int y = 0; y < BOARD_SIZE; y++)
                {
                    if (pieces[y * BOARD_SIZE + x] == 1)
                    {
                        g.setColor(PLAYER_ONE_COLOR);
                        g.fillOval(x * CELL_SIZE + (CELL_SIZE - PIECE_SIZE) / 2, y * CELL_SIZE + (CELL_SIZE - PIECE_SIZE) / 2, PIECE_SIZE, PIECE_SIZE);
                    } else if (pieces[y * BOARD_SIZE + x] == 2)
                    {
                        g.setColor(PLAYER_TWO_COLOR);
                        g.fillOval(x * CELL_SIZE + (CELL_SIZE - PIECE_SIZE) / 2, y * CELL_SIZE + (CELL_SIZE - PIECE_SIZE) / 2, PIECE_SIZE, PIECE_SIZE);
                    }
                }
            }
        } else
        {
            g.setColor(Color.RED);
            g.setFont(font);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
            int stringWidth = g2.getFontMetrics().stringWidth(waitingString);
            g.drawString(waitingString, BOARD_SIZE * CELL_SIZE / 2 - stringWidth / 2, BOARD_SIZE * CELL_SIZE / 2);
        }
    }

    public void setPieces(int[] pieces)
    {
        this.pieces = pieces;
    }

    public JButton getTurnButton()
    {
        return turnButton;
    }

    public JTextArea getText()
    {
        return text;
    }
    
    

}
