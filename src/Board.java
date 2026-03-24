import java.util.Set;

public class Board {
    private int rows;
    private int cols;
    private int totalMines;
    private Cell[][] grid;
    private boolean gameOver = false;
    private boolean firstClick = true; // NEW

    public Board(int rows, int cols, int totalMines) {
        this.rows = rows;
        this.cols = cols;
        this.totalMines = totalMines;
        this.grid = new Cell[rows][cols];
        initializeBoard();
        // NOTE: mines are no longer placed in the constructor
    }

    private void initializeBoard() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = new Cell();
            }
        }
    }

    // NEW: builds a set of excluded positions (clicked cell + its neighbors)
    private Set<Integer> getSafeZone(int row, int col) {
        java.util.Set<Integer> safe = new java.util.HashSet<>();
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nr = row + dr;
                int nc = col + dc;
                if (inBounds(nr, nc)) {
                    safe.add(nr * cols + nc); // encode (r,c) as single int
                }
            }
        }
        return safe;
    }

    // NEW: mine placement now accepts a safe zone to exclude
    private void placeMines(java.util.Set<Integer> safeZone) {
        int placed = 0;
        while (placed < totalMines) {
            int r = (int)(Math.random() * rows);
            int c = (int)(Math.random() * cols);
            if (!grid[r][c].isMine() && !safeZone.contains(r * cols + c)) {
                grid[r][c].setMine(true);
                placed++;
            }
        }
    }

    private void calculateNeighbors() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!grid[r][c].isMine()) {
                    grid[r][c].setNeighborMines(countMinesAround(r, c));
                }
            }
        }
    }

    private int countMinesAround(int row, int col) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = row + dr;
                int nc = col + dc;
                if (inBounds(nr, nc) && grid[nr][nc].isMine()) {
                    count++;
                }
            }
        }
        return count;
    }

    public void reveal(int row, int col) {
        if (gameOver) return;
        if (!inBounds(row, col)) return;

        // NEW: on first click, place mines avoiding the clicked area
        if (firstClick) {
            firstClick = false;
            placeMines(getSafeZone(row, col));
            calculateNeighbors();
        }

        Cell cell = grid[row][col];
        if (cell.isRevealed() || cell.isFlagged()) return;
        cell.reveal();

        if (cell.isMine()) {
            gameOver = true;
            revealAllMines();
            return;
        }

        if (cell.getNeighborMines() > 0) return;

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                reveal(row + dr, col + dc);
            }
        }
    }

    private void revealAllMines() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c].isMine()) grid[r][c].reveal();
            }
        }
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    public Cell getCell(int r, int c) {
        return grid[r][c];
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isWon() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = grid[r][c];
                if (!cell.isMine() && !cell.isRevealed()) return false;
            }
        }
        return !gameOver;
    }
}
