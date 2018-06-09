package networkedtictactoe;

import java.awt.Point;
import java.io.Serializable;

/**
 *
 * @author igor
 */
public class Board implements Serializable{
    private static final int DEFAULT_BOARD_SIZE = 3;
    private int boardSize;
    private String[][] board;
    private Point firstSpot, secondSpot;
    private GameResult result = GameResult.PLAYING;
    
    public Board(){
        this(DEFAULT_BOARD_SIZE);        
    }
    
    public Board(int size){
        this.boardSize = size;
        this.board = new String[size][size];
        for(int i = 0; i < boardSize; ++i){
            for(int j = 0; j < boardSize; ++j){
                this.board[i][j] = " ";
            }
        }
        this.firstSpot = new Point(-1,-1);
        this.secondSpot = new Point(-1,-1);
    }
    
    public Board(Board other){
        this.boardSize = other.boardSize;
        this.board = (String[][])other.board.clone();
        this.firstSpot = (Point)other.firstSpot.clone();
        this.secondSpot = (Point)other.secondSpot.clone();
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

    public GameResult getResult() {
        return result;
    }
    
    public String valueAt(int x, int y){
        return isCoordinatesValid(x, y) ? board[y][x] : "";
    }
    
    private boolean isCoordinatesValid(int x, int y){
        return x >= 0 && x < this.boardSize
                && y >= 0 && y < this.boardSize;
    }
    
    public boolean tryToMove(Point move, PlayerType type){
        if(isCoordinatesValid(move.x, move.y)
           && this.board[move.y][move.x].equals(" ")){
            makeMove(move, type);
            return true;
        }
        return false;
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
    
    public GameResult checkGameResult(){
        System.out.println("Checking game status");
        //Check rows
        outer:
        for(int i = 0; i < boardSize; ++i){
            String first = board[i][0];
            if(first.equals(" ")) continue;
            for(int j = 1; j < boardSize; ++j){
                if(!board[i][j].equalsIgnoreCase(first))
                    continue outer;
            }
            if(first.equalsIgnoreCase(PlayerType.Cross.getMoveSign())){
                result = GameResult.CROSSES_WON;
                System.out.println("Player X won!!!");
            } else if(first.equalsIgnoreCase(PlayerType.Circle.getMoveSign())){
                result = GameResult.CIRCLES_WON;
                System.out.println("Player O won!!!");
            }
            this.firstSpot.x = 0;
            this.firstSpot.y = i;
            this.secondSpot.x = boardSize - 1;
            this.secondSpot.y = this.firstSpot.y;
            return result;
        }
        
        //Check columns
        outer:
        for(int j = 0; j < this.boardSize; ++j){
            String first = board[0][j];
            if(first.equals(" ")) continue;
            for(int i = 1; i < this.boardSize; ++i){
                if(!board[i][j].equalsIgnoreCase(first))
                    continue outer;
            }
            if(first.equalsIgnoreCase(PlayerType.Cross.getMoveSign())){
                result = GameResult.CROSSES_WON;
            } else if(first.equalsIgnoreCase(PlayerType.Circle.getMoveSign())){
                result = GameResult.CIRCLES_WON;
            }
            this.firstSpot.x = j;
            this.firstSpot.y = 0;
            this.secondSpot.x = this.firstSpot.x;
            this.secondSpot.y = this.boardSize - 1;
            return result;
        }
        
        //Check main diagonal
        String first = board[0][0];
        boolean isMainDiagonalFilled = true;
        if(first.equals(" ")){
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
                result = GameResult.CROSSES_WON;
            } else if(first.equalsIgnoreCase(PlayerType.Circle.getMoveSign())){
                result = GameResult.CIRCLES_WON;
            }
            this.firstSpot.x = 0;
            this.firstSpot.y = 0;
            this.secondSpot.x = this.boardSize - 1;
            this.secondSpot.y = this.boardSize - 1;
            return result;
        }
        
        //Check secondary diagonal
        boolean isSecondaryDiagonalFilled = true;
        first = board[0][this.boardSize - 1];
        if(first.equals(" ")){
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
                result = GameResult.CROSSES_WON;
            } else if(first.equalsIgnoreCase(PlayerType.Circle.getMoveSign())){
                result = GameResult.CIRCLES_WON;
            }
            this.firstSpot.x = this.boardSize - 1;
            this.firstSpot.y = 0;
            this.secondSpot.x = 0;
            this.secondSpot.y = this.boardSize - 1;
            return result;
        }
        if(countFreeSpots() == 0){
            result = GameResult.TIE;
        }
        return result;
    }
    
    public void print(){
        System.out.println("------");
        for(int i = 0; i < boardSize; ++i){
            for(int j = 0; j < boardSize; ++j){
                System.out.print(board[i][j] + "|");
            }
            System.out.println("\n------");
        }
    }
}
