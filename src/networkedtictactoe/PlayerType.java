package networkedtictactoe;

import java.io.Serializable;

/**
 *
 * @author igor
 */
public enum PlayerType implements Serializable{
    Circle("O"),
    Cross("X");
    
    private String moveSign;

    private PlayerType(String moveSign) {
        this.moveSign = moveSign;
    }

    public String getMoveSign() {
        return moveSign;
    }
}
