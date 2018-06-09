package networkedtictactoe;

/**
 *
 * @author igor
 */
public class Board {
    private static final int DEFAULT_BOARD_SIZE = 3;
    private int boardSize;
    private String[][] board;
    int x1, y1, x2, y2;
    
    public Board(){
        this(DEFAULT_BOARD_SIZE);
    }
    
    public Board(int size){
        this.board = new String[size][size];
        this.boardSize = size;
    }
    
    public void makeMove(int move, PlayerType type){
        System.out.println("Making move...");
        int y = move / boardSize;
        int x = move % boardSize;
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
    
    private void checkGameStatus(GameStatus gameStatus){
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
            if(first.equalsIgnoreCase("X")){
                gameStatus = GameStatus.X_WON;
            } else if(first.equalsIgnoreCase("O")){
                gameStatus = GameStatus.O_WON;
            }
            x1 = 0;
            y1 = i;
            x2 = boardSize - 1;
            y2 = y1;
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
            if(first.equalsIgnoreCase("X")){
                gameStatus = GameStatus.X_WON;
            } else if(first.equalsIgnoreCase("O")){
                gameStatus = GameStatus.O_WON;
            }
            x1 = j;
            y1 = 0;
            x2 = x1;
            y2 = this.boardSize - 1;
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
            if(first.equalsIgnoreCase("X")){
                gameStatus = GameStatus.X_WON;
            } else if(first.equalsIgnoreCase("O")){
                gameStatus = GameStatus.O_WON;
            }
            x1 = 0;
            y1 = 0;
            x2 = this.boardSize - 1;
            y2 = this.boardSize - 1;
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
            if(first.equalsIgnoreCase("X")){
                gameStatus = GameStatus.X_WON;
            } else if(first.equalsIgnoreCase("O")){
                gameStatus = GameStatus.O_WON;
            }
            x1 = this.boardSize - 1;
            y1 = 0;
            x2 = 0;
            y2 = this.boardSize - 1;
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
