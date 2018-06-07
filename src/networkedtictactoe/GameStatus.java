package networkedtictactoe;

/**
 *
 * @author igor
 */
public enum GameStatus {
    WAINTING_FOR_OPPONENT("Waiting for opponent...") {
        @Override
        public boolean isGameOver() {
            return false;
        }
    },
    X_TO_PLAY("X player's turn") {
        @Override
        public boolean isGameOver() {
            return false;
        }
    },
    O_TO_PLAY("O player's turn") {
        @Override
        public boolean isGameOver() {
            return false;
        }
    },
    X_WON("X Player won!!!") {
        @Override
        public boolean isGameOver() {
            return true;
        }
    },
    O_WON("O player won!!!") {
        @Override
        public boolean isGameOver() {
            return true;
        }
    },
    TIE("It's tie!!!") {
        @Override
        public boolean isGameOver() {
            return true;
        }
    },
    OPPONENT_DISCONNECTED("Opponent disconnected") {
        @Override
        public boolean isGameOver() {
            return true;
        }
    };
    private String text;

    private GameStatus(String text) {
        this.text = text;
    }
    
    public String getDescription(){
        return this.text;
    }
    
    public abstract boolean isGameOver();
}
