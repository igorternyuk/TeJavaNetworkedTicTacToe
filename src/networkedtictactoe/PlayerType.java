package networkedtictactoe;

/**
 *
 * @author igor
 */
public enum PlayerType {
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
