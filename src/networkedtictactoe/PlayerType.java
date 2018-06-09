package networkedtictactoe;

import java.io.Serializable;

/**
 *
 * @author igor
 */
public enum PlayerType implements Serializable{
    Circle("O") {
        @Override
        public PlayerType getOpponent() {
            return Cross;
        }
    },
    Cross("X") {
        @Override
        public PlayerType getOpponent() {
            return Circle;
        }
    };
    
    private String moveSign;

    private PlayerType(String moveSign) {
        this.moveSign = moveSign;
    }

    public String getMoveSign() {
        return moveSign;
    }
    
    public abstract PlayerType getOpponent();
}
