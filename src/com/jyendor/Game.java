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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.Thread.sleep;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;

/**
 *
 * @author otso
 */
final class Game implements Runnable, MouseListener
{

    private final Painter painter;
    private final int BOARD_SIZE = 8;
    private int[] pieces = new int[BOARD_SIZE * BOARD_SIZE];
    private boolean[] crowned = new boolean[BOARD_SIZE * BOARD_SIZE];
    private final Thread thread;

    private String ip = "localhost";
    private int port = 55554;

    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private ServerSocket serverSocket;

    private boolean yourTurn = false;
    private boolean won = false;
    private boolean opponentWon = false;
    private boolean tie = false;
    private boolean unableToCommunicateWithOpponent = false;
    private boolean accepted = false;
    private boolean player_one = false;

    private int selectedPiece = -1;
    private boolean pieceSelected = false;
    private boolean canMoveWithoutEating = false;
    private boolean canEat = false;

    private int errors = 0;
    private ArrayList<Integer> path = new ArrayList<>();
    private ArrayList<Integer> piecesToEat = new ArrayList<>();

    public Game()
    {
        thread = new Thread(this, "Draughts");
        painter = new Painter(BOARD_SIZE, pieces, this);
        addTurnButtonListener(painter.getTurnButton());
    }

    public void startGame(String ip, int port)
    {
        this.ip = ip;
        this.port = port;
        if (!connect())
        {
            initializeServer();
        }
        painter.repaint();
        thread.start();
    }

    private void initializePieces()
    {
        int pieceRows = BOARD_SIZE / 2 - 1;
        for (int x = 0; x < BOARD_SIZE; x++)
        {
            for (int y = 0; y < BOARD_SIZE; y++)
            {
                if (x % 2 == 0 && y % 2 != 0 || x % 2 == 1 && y % 2 != 1)
                {
                    if (y < pieceRows)
                    {
                        pieces[y * BOARD_SIZE + x] = 2;
                    } else if (y >= BOARD_SIZE - pieceRows)
                    {
                        pieces[y * BOARD_SIZE + x] = 1;
                    }
                }
            }
        }
    }

    @Override
    public void run()
    {
        initializePieces();

        while (true)
        {
            tick();
            painter.repaint();

            // If you are the server, listen if anyone is trying to join the server
            if (player_one && !accepted)
            {
                listenForServerRequest();
            }
            try {    
                sleep(17);
            } catch (InterruptedException ex) {
                Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void tick()
    {
        if (errors >= 10)
        {
            unableToCommunicateWithOpponent = true;
            return;
        }
        if (!yourTurn && !unableToCommunicateWithOpponent)
        {
            try
            {
                try
                {
                    //int[] updatedPieces = (int[]) ois.readObject();
                    //System.arraycopy(updatedPieces, 0, pieces, 0, pieces.length);

                    pieces = (int[]) ois.readObject();
                    painter.setPieces(pieces);
                } catch (ClassNotFoundException ex)
                {
                    ex.printStackTrace();
                }
                checkForOpponentWin();
                checkForTie();
                yourTurn = true;
            } catch (IOException e)
            {
                e.printStackTrace();
                errors++;
            }
        }
    }

    private void checkForWin()
    {
        int opponentPieces = 0;
        int opponent = player_one ? 2 : 1;
        for (int piece : pieces)
        {
            if (piece == opponent)
            {
                opponentPieces++;
            }
        }
        if (opponentPieces == 0)
        {
            won = true;
        }
    }

    private void checkForOpponentWin()
    {
        int ownPieces = 0;
        int playerNumber = player_one ? 1 : 2;
        for (int piece : pieces)
        {
            if (piece == playerNumber)
            {
                ownPieces++;
            }
        }
        if (ownPieces == 0)
        {
            opponentWon = true;
        }
    }

    private void checkForTie()
    {
        // Not yet implemented
    }

    private boolean connect()
    {
        try
        {
            socket = new Socket(ip, port);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
            accepted = true;

        } catch (IOException e)
        {
            System.out.println("Unable to connect to address: " + ip + ":" + port + ". Starting a server.");
            return false;
        }
        System.out.println("Succesfully connected to the server.");
        return true;
    }

    private void initializeServer()
    {
        try
        {
            serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        player_one = true;
        yourTurn = true;
    }

    private void listenForServerRequest()
    {
        Socket socket = null;
        try
        {
            socket = serverSocket.accept();
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
            accepted = true;
            System.out.println("Client has requested to join and we have accepted.");
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void addTurnButtonListener(JButton button)
    {
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (yourTurn)
                {
                    endTurnButtonPressed();
                }
            }
        });
    }

    private void endTurnButtonPressed()
    {
        if (yourTurn && !unableToCommunicateWithOpponent && !won && !opponentWon)
        {
            move();
            pieceSelected = false;
            selectedPiece = -1;
            yourTurn = false;
            painter.repaint();
            Toolkit.getDefaultToolkit().sync();
            try
            {
                oos.writeObject(pieces);
                oos.flush();
            } catch (IOException e1)
            {
                errors++;
                e1.printStackTrace();
            }
            path.clear();
            checkForWin();
            checkForTie();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if (accepted)
        {
            if (yourTurn && !unableToCommunicateWithOpponent && !won && !opponentWon)
            {
                int x, y;
                if (player_one)
                {
                    x = e.getX() / Painter.CELL_SIZE;
                    y = e.getY() / Painter.CELL_SIZE;
                } else
                {
                    x = (Painter.CELL_SIZE * (BOARD_SIZE) - e.getX()) / Painter.CELL_SIZE;
                    y = (Painter.CELL_SIZE * (BOARD_SIZE) - e.getY()) / Painter.CELL_SIZE;
                }

                if (!pieceSelected)
                {
                    if (isSelectablePiece(x, y))
                    {
                        selectedPiece = y * BOARD_SIZE + x;
                        pieceSelected = true;
                        path.clear();
                        piecesToEat.clear();
                        path.add(selectedPiece);
                        canMoveWithoutEating = true;
                        canEat = true;
                    }
                } else
                {
                    if (!tryToaddToPath(x, y))
                    {
                        addMessage("Selected piece cannot move there. Resetting path and selection.");
                        pieceSelected = false;
                        selectedPiece = -1;
                        path.clear();
                    }
                }
            }
        }
    }

    private void addMessage(String text)
    {
        painter.getText().append(text + "\n");
    }

    private void addPath(int x, int y)
    {
        canMoveWithoutEating = false;
        path.add(y * BOARD_SIZE + x);
    }

    private void move()
    {
        if (pieceSelected && !path.isEmpty())
        {
            pieces[selectedPiece] = 0;
            int playerNumber = player_one ? 1 : 2;
            pieces[path.get(path.size() - 1)] = playerNumber;
            int x1 = selectedPiece % BOARD_SIZE;
            int y1 = (int) selectedPiece / BOARD_SIZE;
            int x2 = path.get(path.size() - 1) % BOARD_SIZE;
            int y2 = (int) path.get(path.size() - 1) / BOARD_SIZE;

            addMessage(String.format("Piece x: %d y: %d moving to x: %d y: %d", x1, y1, x2, y2));
            if (crowned[selectedPiece])
            {
                crowned[selectedPiece] = false;
                crowned[path.get(path.size() - 1)] = true;
            }
            if (player_one)
            {
                if ((int) path.get(path.size() - 1) / BOARD_SIZE == 0)
                {
                    addMessage("The piece was crowned.");
                    crowned[path.get(path.size() - 1)] = true;
                }
            } else
            {
                if ((int) path.get(path.size() - 1) / BOARD_SIZE == BOARD_SIZE - 1)
                {
                    if(!crowned[path.get(path.size() - 1)])
                        addMessage("The piece was crowned.");
                    crowned[path.get(path.size() - 1)] = true;
                }
            }
            if (!piecesToEat.isEmpty())
            {
                for (int pieceToEat : piecesToEat)
                {
                    addMessage("Ate a piece.");
                    pieces[pieceToEat] = 0;
                }
                piecesToEat.clear();
            }
        }
    }

    private boolean isSelectablePiece(int x, int y)
    {
        int cell = y * BOARD_SIZE + x;
        if (player_one)
        {
            return (pieces[cell] == 1);

        } else
        {
            return (pieces[cell] == 2);
        }
    }

    private boolean tryToaddToPath(int targetX, int targetY)
    {
        int startX = path.get(path.size() - 1) % BOARD_SIZE;
        int startY = (int) path.get(path.size() - 1) / BOARD_SIZE;
        int targetID = targetY * BOARD_SIZE + targetX;

        if (pieces[targetID] == 0)
        {
            if (canMoveWithoutEating)
            {
                if (player_one)
                {
                    // no eating
                    if (targetY + 1 == startY && (targetX - 1 == startX || targetX + 1 == startX))
                    {
                        canEat = false;
                        addPath(targetX, targetY);
                        return true;
                    } else if (crowned[selectedPiece])
                    {
                        if (targetY - 1 == startY && (targetX - 1 == startX || targetX + 1 == startX))
                        {
                            canEat = false;
                            addPath(targetX, targetY);
                            return true;
                        }
                    }
                } else
                {
                    // no eating
                    if (targetY - 1 == startY && (targetX - 1 == startX || targetX + 1 == startX))
                    {
                        canEat = false;
                        addPath(targetX, targetY);
                        return true;
                    } else if (crowned[selectedPiece])
                    {
                        if (targetY + 1 == startY && (targetX - 1 == startX || targetX + 1 == startX))
                        {
                            canEat = false;
                            addPath(targetX, targetY);
                            return true;
                        }
                    }
                }
            }
            // eating
            //up-right
            if (canEat)
            {
                int opponent = player_one ? 2 : 1;
                if (targetY + 2 == startY && targetX + 2 == startX && pieces[(targetY + 1) * BOARD_SIZE + targetX + 1] == opponent)
                {
                    addPath(targetX, targetY);
                    piecesToEat.add((targetY + 1) * BOARD_SIZE + targetX + 1);
                    return true;
                } //up-left
                else if (targetY + 2 == startY && targetX - 2 == startX && pieces[(targetY + 1) * BOARD_SIZE + targetX - 1] == opponent)
                {
                    addPath(targetX, targetY);
                    piecesToEat.add((targetY + 1) * BOARD_SIZE + targetX - 1);
                    return true;
                } //down-right
                else if (targetY - 2 == startY && targetX + 2 == startX && pieces[(targetY - 1) * BOARD_SIZE + targetX + 1] == opponent)
                {
                    addPath(targetX, targetY);
                    piecesToEat.add((targetY - 1) * BOARD_SIZE + targetX + 1);
                    return true;
                } //down-left
                else if (targetY - 2 == startY && targetX - 2 == startX && pieces[(targetY - 1) * BOARD_SIZE + targetX - 1] == opponent)
                {
                    addPath(targetX, targetY);
                    piecesToEat.add((targetY - 1) * BOARD_SIZE + targetX - 1);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }

    public boolean isYourTurn()
    {
        return yourTurn;
    }

    public boolean isWon()
    {
        return won;
    }

    public boolean isOpponentWon()
    {
        return opponentWon;
    }

    public boolean isTie()
    {
        return tie;
    }

    public boolean isUnableToCommunicateWithOpponent()
    {
        return unableToCommunicateWithOpponent;
    }

    public boolean isAccepted()
    {
        return accepted;
    }

    public int getSelectedPiece()
    {
        return selectedPiece;
    }

    public boolean isPieceSelected()
    {
        return pieceSelected;
    }

    public ArrayList<Integer> getPath()
    {
        return path;
    }

    public boolean isPlayer_one()
    {
        return player_one;
    }

    public boolean[] getCrowned()
    {
        return crowned;
    }
}
