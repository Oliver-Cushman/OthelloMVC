package othellomvc;

import com.mrjaffesclass.apcs.messenger.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The model represents the data that the app uses.
 *
 * @author Roger Jaffe
 * @version 1.0
 */
public class Model implements MessageHandler {

    // Messaging system for the MVC
    private final Messenger mvcMessaging;

    private boolean whoseMove;
    private boolean gameOver;

    private int[][] board;
    
    private List<String> prevLegalMoves;
    
    private int p1Count;
    private int p2Count;

    /**
     * Model constructor: Create the data representation of the program
     *
     * @param messages Messaging class instantiated by the Controller for local
     * messages between Model, View, and controller
     */
    public Model(Messenger messages) {
        mvcMessaging = messages;
        newGame();
    }

    /**
     * Initialize the model here and subscribe to any required messages
     */
    public void init() {
        mvcMessaging.subscribe("playerMove", this);
        mvcMessaging.subscribe("newGame", this);
    }
    
    private void newGame() {
        board = new int[Constants.NUM_ROWS][Constants.NUM_COLS];
        board[3][3] = 1;
        board[3][4] = -1;
        board[4][3] = -1;
        board[4][4] = 1;
        whoseMove = ((int)(Math.random() * 2) == 1) ? true : false;
        gameOver = false;
        p1Count = 2;
        p2Count = 2;
        prevLegalMoves = legalMoves();
        mvcMessaging.notify("newLegalMoves", prevLegalMoves);
        mvcMessaging.notify("boardChange", board);
        mvcMessaging.notify("whoseMoveChange", whoseMove);
        mvcMessaging.notify("pieceCountChange", getCounts());
        
    }

    @Override
    public void messageHandler(String messageName, Object messagePayload) {
        if (messagePayload != null) {
            System.out.println("MSG: received by model: " + messageName + " | " + messagePayload.toString());
        } else {
            System.out.println("MSG: received by model: " + messageName + " | No data sent");
        }

        if (messageName.equals("playerMove")) {
            String position = (String) messagePayload;
            int row = Integer.parseInt(position.substring(0, 1));
            int col = Integer.parseInt(position.substring(1));
            if (tryMove(row, col, true) || prevLegalMoves.isEmpty() && !gameOver) {
                int totalCount = p1Count + p2Count;
                if (totalCount < Constants.NUM_COLS * Constants.NUM_ROWS) {
                    whoseMove = !whoseMove;
                    prevLegalMoves = legalMoves();
                    mvcMessaging.notify("newLegalMoves", prevLegalMoves);
                    mvcMessaging.notify("whoseMoveChange", whoseMove);
                } else {
                    boolean tie = p1Count == p2Count;
                    boolean p1Win = p1Count > p2Count;
                    if (tie) {
                        mvcMessaging.notify("tie");
                    } else {
                        mvcMessaging.notify("win", whoseMove);
                    }
                    gameOver = true;
                }
                mvcMessaging.notify("boardChange", board);
                mvcMessaging.notify("pieceCountChange", getCounts());
            }
        } else if (messageName.equals("newGame")) {
            newGame();
        }
    }
    
    private int[] getCounts() {
        int[] counts = { p1Count, p2Count };
        return counts;
    }

    private boolean tryMove(int row, int col, boolean makeMove) {
        String[] allDirections = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        boolean goodMove = false;
        for (String direction : allDirections) {
            if (moveInDirection(direction, row, col, makeMove)) {
                goodMove = true;
            }
        }
        if (goodMove && makeMove) {
            board[row][col] = whoseMove ? 1 : -1;
            if (whoseMove) {
                p1Count++;
            } else {
                p2Count++;
            }
        }
        return goodMove;
    }

    private boolean moveInDirection(String direction, int row, int col, boolean makeMove) {
        int ownPiece = whoseMove ? 1 : -1;
        if (board[row][col] == ownPiece || board[row][col] == ownPiece * - 1) return false;
        List<String> coordinates = new ArrayList<String>();
        int count = 0;
        boolean found = false;
        boolean valid = true;
        while (row >= 0 && row < board.length && col >= 0 && col < board[0].length && !found && valid) {
            if ((count < 2 && board[row][col] == ownPiece) || (count > 0 && board[row][col] == 0)) {
                valid = false;
            } else if (count >= 2 && board[row][col] == ownPiece) {
                found = true;
            } else if (count != 0) {
                coordinates.add(row + "" + col);
            }
            switch (direction) {
                case "N":
                    row--;
                    break;
                case "NE":
                    row--;
                    col++;
                    break;
                case "E":
                    col++;
                    break;
                case "SE":
                    row++;
                    col++;
                    break;
                case "S":
                    row++;
                    break;
                case "SW":
                    row++;
                    col--;
                    break;
                case "W":
                    col--;
                    break;
                case "NW":
                    row--;
                    col--;
                    break;
            }
            count++;
        }
        if (found && makeMove) {
            for (String coordinate : coordinates) {
                int rowTemp = Integer.parseInt(coordinate.substring(0, 1));
                int colTemp = Integer.parseInt(coordinate.substring(1));
                board[rowTemp][colTemp] = ownPiece;
                if (whoseMove) {
                    p1Count++;
                    p2Count--;
                } else {
                    p1Count--;
                    p2Count++;
                }
            }
        }
        return found;
    }
    
    private List<String> legalMoves() {
        List<String> legalMoves = new ArrayList<String>();
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (tryMove(i, j, false)) {
                    legalMoves.add(i + "" + j);
                }
            }
        }
        System.out.println(legalMoves);
        return legalMoves;
    }
}
