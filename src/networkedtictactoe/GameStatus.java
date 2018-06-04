package networkedtictactoe;

/**
 *
 * @author igor
 */
public enum GameStatus {
    X_WON("X"),
    O_WON("O"),
    TIE("_"),
    PLAY(" ");

    private String text;

    private GameStatus(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }
}
