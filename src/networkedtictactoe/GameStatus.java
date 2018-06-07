package networkedtictactoe;

/**
 *
 * @author igor
 */
public enum GameStatus {
    WAINTING_FOR_OPPONENT {
        @Override
        public boolean isGameOver() {
            return false;
        }
    },
    X_TO_PLAY {
        @Override
        public boolean isGameOver() {
            return false;
        }
    },
    O_TO_PLAY {
        @Override
        public boolean isGameOver() {
            return false;
        }
    },
    X_WON {
        @Override
        public boolean isGameOver() {
            return true;
        }
    },
    O_WON {
        @Override
        public boolean isGameOver() {
            return true;
        }
    },
    TIE {
        @Override
        public boolean isGameOver() {
            return true;
        }
    },
    OPPONENT_DISCONNECTED {
        @Override
        public boolean isGameOver() {
            return true;
        }
    };
    
    public abstract boolean isGameOver();
}
