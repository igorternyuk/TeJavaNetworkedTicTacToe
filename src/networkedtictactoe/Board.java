package networkedtictactoe;

import java.awt.Point;

/**
 *
 * @author igor
 */
public class Board {
    private static final int DEFAULT_BOARD_SIZE = 3;
    private int boardSize;
    private String[][] board;
    int x1, y1, x2, y2;
    Point firstSpot, secondSpot; 
    
    public Board(){
        this(DEFAULT_BOARD_SIZE);
        this.firstSpot = new Point(-1,-1);
        this.secondSpot = new Point(-1,-1);
    }
    
    public Board(int size){
        this.board = new String[size][size];
        this.boardSize = size;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public Point getFirstSpot() {
        return firstSpot;
    }

    public Point getSecondSpot() {
        return secondSpot;
    }
    
    public boolean tryToMove(Point move, PlayerType type){
        if(move.x >= 0 && move.x < this.boardSize
            && move.y >= 0 && move.y < this.boardSize
            && this.board[move.y][move.x].isEmpty()){
            makeMove(move, type);
            return true;
        }
        return false;
    }
    
    private void makeMove(int move, PlayerType type){
        int y = move / boardSize;
        int x = move % boardSize;
        makeMove(x, y, type);
    }
    
    private void makeMove(Point move, PlayerType type){
        makeMove(move.x, move.y, type);
    }
    
    private void makeMove(int x, int y, PlayerType type){
        System.out.println("Making move...");
        System.out.println("x = " + x + " y = " + y);
        this.board[y][x] = type.getMoveSign();
    }
    
    private int countFreeSpots(){
        int count = 0;
        for(int i = 0; i < this.boardSize; ++i){
            for(int j = 0; j < this.boardSize; ++j){
                if(board[i][j].equals(" ")){
                    ++count;
                }
            }
        }
        return count;
    }
    
    public void checkGameStatus(GameStatus gameStatus){
        System.out.println("Checking game status");
        //Check rows
        outer:
        for(int i = 0; i < boardSize; ++i){
            String first = board[i][0];
            if(first.isEmpty()) continue;
            for(int j = 1; j < boardSize; ++j){
                if(!board[i][j].equalsIgnoreCase(first))
                    continue outer;
            }
            if(first.equalsIgnoreCase(PlayerType.Cross.getMoveSign())){
                gameStatus = GameStatus.X_WON;
            } else if(first.equalsIgnoreCase(PlayerType.Circle.getMoveSign())){
                gameStatus = GameStatus.O_WON;
            }
            this.firstSpot.x = 0;
            this.firstSpot.y = i;
            this.secondSpot.x = boardSize - 1;
            this.secondSpot.y = this.firstSpot.y;
            return;
        }
        
        //Check columns
        outer:
        for(int j = 0; j < this.boardSize; ++j){
            String first = board[0][j];
            if(first.isEmpty()) continue;
            for(int i = 1; i < this.boardSize; ++i){
                if(!board[i][j].equalsIgnoreCase(first))
                    continue outer;
            }
            if(first.equalsIgnoreCase(PlayerType.Cross.getMoveSign())){
                gameStatus = GameStatus.X_WON;
            } else if(first.equalsIgnoreCase(PlayerType.Circle.getMoveSign())){
                gameStatus = GameStatus.O_WON;
            }
            this.firstSpot.x = j;
            this.firstSpot.y = 0;
            this.secondSpot.x = this.firstSpot.x;
            this.secondSpot.y = this.boardSize - 1;
            return;
        }
        
        //Check main diagonal
        String first = board[0][0];
        boolean isMainDiagonalFilled = true;
        if(first.isEmpty()){
            isMainDiagonalFilled = false;
        } else {
            for(int i = 1; i < this.boardSize; ++i){
                first = board[0][0];
                if(!board[i][i].equalsIgnoreCase(first)){
                    isMainDiagonalFilled = false;
                    break;
                }
            }
        }
        
        if(isMainDiagonalFilled){
            if(first.equalsIgnoreCase(PlayerType.Cross.getMoveSign())){
                gameStatus = GameStatus.X_WON;
            } else if(first.equalsIgnoreCase(PlayerType.Circle.getMoveSign())){
                gameStatus = GameStatus.O_WON;
            }
            this.firstSpot.x = 0;
            this.firstSpot.y = 0;
            this.secondSpot.x = this.boardSize - 1;
            this.secondSpot.y = this.boardSize - 1;
            return;
        }
        
        //Check secondary diagonal
        boolean isSecondaryDiagonalFilled = true;
        first = board[0][this.boardSize - 1];
        if(first.isEmpty()){
            isSecondaryDiagonalFilled = false;
        } else {
            for(int i = 1; i < this.boardSize; ++i){
                if(!board[i][this.boardSize - 1 - i].equalsIgnoreCase(first)){
                    isSecondaryDiagonalFilled = false;
                    break;
                }
            }
        }
        
        if(isSecondaryDiagonalFilled){
            if(first.equalsIgnoreCase(PlayerType.Cross.getMoveSign())){
                gameStatus = GameStatus.X_WON;
            } else if(first.equalsIgnoreCase(PlayerType.Circle.getMoveSign())){
                gameStatus = GameStatus.O_WON;
            }
            this.firstSpot.x = this.boardSize - 1;
            this.firstSpot.y = 0;
            this.secondSpot.x = 0;
            this.secondSpot.y = this.boardSize - 1;
            return;
        }
        if(countFreeSpots() == 0)
            gameStatus = GameStatus.TIE;
    }
    
    public void print(){
        System.out.println("------");
        for(int i = 0; i < board.length; ++i){
            for(int j = 0; j < board[i].length; ++j){
                System.out.print(board[i][j] + "|");
            }
            System.out.println("\n------");
        }
    }
}
