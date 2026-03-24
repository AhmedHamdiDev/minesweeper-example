public class Cell {
    private boolean isMine;
    private boolean isRevealed;
    private boolean isFlagged;
    private int neighborMines;

    public Cell() {
        this.isMine = false;
        this.isRevealed = false;
        this.isFlagged = false;
        this.neighborMines = 0;
    }

    public boolean isMine() {
        return isMine;
    }

    public boolean isRevealed() {
        return isRevealed;
    }

    public boolean isFlagged() {
        return isFlagged;
    }

    public int getNeighborMines() {
        return neighborMines;
    }

    public void setMine(boolean isMine) {
        this.isMine = isMine;
    }

    public void setNeighborMines(int count) {
        this.neighborMines = count;
    }

    public void reveal() {
        if (!isFlagged) {
            this.isRevealed = true;
        }
    }

    public void toggleFlag() {
        if (!isRevealed) {
            this.isFlagged = !this.isFlagged;
        }
    }

    public boolean isEmpty() {
        return neighborMines == 0 && !isMine;
    }
}
