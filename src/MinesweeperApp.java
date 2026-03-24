import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MinesweeperApp extends Application {
    private static final int R = 10, C = 10, M = 10;

    private Board board;
    private Button[][] buttons;
    private Label mineCountLabel;
    private Label timerLabel;
    private Label statusLabel;
    private Button resetBtn;
    private Button themeBtn;
    private Timeline timer;
    private int secondsElapsed;
    private int flagsPlaced;
    private boolean isDarkMode = true;
    private BorderPane root;
    private GridPane grid;
    private Scene scene;

    // ── Dark theme ────────────────────────────────────────────────
    private static final String D_BG           = "#1e1e2e";
    private static final String D_UNREVEALED   = "#454158";
    private static final String D_REVEALED     = "#11111b";
    private static final String D_FLAGGED      = "#524f6e";
    private static final String D_MINE         = "#f38ba8";
    private static final String D_BORDER       = "#6c7086";
    private static final String D_TEXT         = "#cdd6f4";
    private static final String D_TEXT_DIM     = "#6c7086";
    private static final String D_TOPBAR       = "#181825";

    // ── Light theme ───────────────────────────────────────────────
    private static final String L_BG           = "#e8eaf0";
    private static final String L_UNREVEALED   = "#c0c8d8";
    private static final String L_REVEALED     = "#f4f4f4";
    private static final String L_FLAGGED      = "#b0b8cc";
    private static final String L_MINE         = "#e57373";
    private static final String L_BORDER       = "#9aa0b0";
    private static final String L_TEXT         = "#1e1e2e";
    private static final String L_TEXT_DIM     = "#9aa0b0";
    private static final String L_TOPBAR       = "#d0d4de";

    // ── Number colors ─────────────────────────────────────────────
    private static final String[] DARK_NUMBER_COLORS = {
        "", "#89b4fa", "#a6e3a1", "#f38ba8",
        "#74c7ec", "#fab387", "#89dceb", "#b4befe", "#cdd6f4"
    };
    private static final String[] LIGHT_NUMBER_COLORS = {
        "", "#1565c0", "#2e7d32", "#c62828",
        "#0277bd", "#e65100", "#00838f", "#6a1b9a", "#37474f"
    };

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        root.setPadding(new Insets(16));

        buildTopBar();
        buildGrid();
        applyTheme();
        startTimer();
        updateUI();

        scene = new Scene(root, 600, 680);

        // Rebind cell sizes whenever the window is resized
        scene.widthProperty().addListener((obs, o, n) -> rebindCellSizes());
        scene.heightProperty().addListener((obs, o, n) -> rebindCellSizes());

        stage.setScene(scene);
        stage.setTitle("Minesweeper");
        stage.setResizable(true); // re-enabled
        stage.show();

        rebindCellSizes(); // initial bind after layout is ready
    }

    // ── Dynamic cell sizing ───────────────────────────────────────

    private void rebindCellSizes() {
        if (buttons == null) return;

        // Reserve space for top bar (~80px) and padding (~32px) and grid gaps (~20px)
        double availableW = scene.getWidth()  - 52;
        double availableH = scene.getHeight() - 120;

        // Cell size is the smaller of width-fit or height-fit to keep cells square
        double cellW = Math.floor(availableW / C);
        double cellH = Math.floor(availableH / R);
        double cellSize = Math.max(32, Math.min(cellW, cellH)); // min 32px

        // Font scales with cell size
        double fontSize = Math.max(10, cellSize * 0.28);

        for (int r = 0; r < R; r++) {
            for (int c = 0; c < C; c++) {
                Button btn = buttons[r][c];
                btn.setPrefSize(cellSize, cellSize);
                btn.setMinSize(cellSize, cellSize);
                btn.setMaxSize(cellSize, cellSize);

                // Re-apply style with updated font size to keep numbers legible
                Cell cell = board.getCell(r, c);
                if (cell.isRevealed() && !cell.isMine()) {
                    String[] numColors = isDarkMode ? DARK_NUMBER_COLORS : LIGHT_NUMBER_COLORS;
                    int n = cell.getNeighborMines();
                    String color = (n > 0 && n <= 8)
                            ? numColors[n]
                            : (isDarkMode ? D_TEXT_DIM : L_TEXT_DIM);
                    btn.setStyle(revealedStyle(n, numColors, fontSize));
                } else if (!cell.isRevealed() && !cell.isFlagged()) {
                    btn.setStyle(unrevealedStyle());
                }
            }
        }
    }

    // ── Top bar ───────────────────────────────────────────────────

    private void buildTopBar() {
        mineCountLabel = new Label("💣 " + M);
        mineCountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        resetBtn = new Button("😊");
        resetBtn.setStyle(
            "-fx-font-size: 18px; -fx-padding: 4 12 4 12;" +
            "-fx-cursor: hand; -fx-border-radius: 6; -fx-background-radius: 6;"
        );
        resetBtn.setOnAction(e -> resetGame());

        timerLabel = new Label("⏱ 0s");
        timerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        themeBtn = new Button(isDarkMode ? "☀ Light" : "★ Dark");
        themeBtn.setStyle(
            "-fx-font-size: 13px; -fx-padding: 4 10 4 10;" +
            "-fx-cursor: hand; -fx-border-radius: 6; -fx-background-radius: 6;"
        );
        themeBtn.setOnAction(e -> toggleTheme());

        HBox topBar = new HBox(mineCountLabel, resetBtn, timerLabel, statusLabel, themeBtn);
        topBar.setAlignment(Pos.CENTER);
        topBar.setSpacing(16);
        topBar.setPadding(new Insets(0, 0, 12, 0));
        root.setTop(topBar);
    }

    // ── Grid ──────────────────────────────────────────────────────

    private void buildGrid() {
        grid = new GridPane();
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setAlignment(Pos.CENTER);

        // Let the grid grow to fill available space
        GridPane.setHgrow(grid, Priority.ALWAYS);
        GridPane.setVgrow(grid, Priority.ALWAYS);

        buttons = new Button[R][C];
        board   = new Board(R, C, M);
        flagsPlaced = 0;

        for (int r = 0; r < R; r++) {
            for (int c = 0; c < C; c++) {
                Button btn = new Button();
                // Initial size — rebindCellSizes() will correct this after show()
                btn.setPrefSize(52, 52);
                btn.setMinSize(32, 32);

                int rr = r, cc = c;
                btn.setOnMouseClicked(e -> {
                    if (board.isGameOver() || board.isWon()) return;
                    if (e.getButton() == MouseButton.PRIMARY) {
                        board.reveal(rr, cc);
                    } else if (e.getButton() == MouseButton.SECONDARY) {
                        boolean wasFlagged = board.getCell(rr, cc).isFlagged();
                        board.getCell(rr, cc).toggleFlag();
                        flagsPlaced += wasFlagged ? -1 : 1;
                        mineCountLabel.setText("💣 " + (M - flagsPlaced));
                    }
                    updateUI();
                    rebindCellSizes();
                });

                buttons[r][c] = btn;
                grid.add(btn, c, r);
            }
        }

        // Wrap in a StackPane so it stays centered when window is very large
        StackPane centerPane = new StackPane(grid);
        centerPane.setAlignment(Pos.CENTER);
        VBox.setVgrow(centerPane, Priority.ALWAYS);
        root.setCenter(centerPane);
    }

    // ── Theme ─────────────────────────────────────────────────────

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        themeBtn.setText(isDarkMode ? "☀ Light" : "★ Dark");
        applyTheme();
        updateUI();
        rebindCellSizes();
    }

    private void applyTheme() {
        String bg     = isDarkMode ? D_BG     : L_BG;
        String topbar = isDarkMode ? D_TOPBAR : L_TOPBAR;
        String text   = isDarkMode ? D_TEXT   : L_TEXT;
        String border = isDarkMode ? D_BORDER : L_BORDER;

        root.setStyle("-fx-background-color: " + bg + ";");

        if (root.getTop() instanceof HBox topBar) {
            topBar.setStyle(
                "-fx-background-color: " + topbar + ";" +
                "-fx-background-radius: 8; -fx-padding: 8 12 8 12;"
            );
            topBar.getChildren().forEach(node -> {
                if (node instanceof Label lbl) {
                    lbl.setStyle(lbl.getStyle() + "-fx-text-fill: " + text + ";");
                } else if (node instanceof Button btn) {
                    btn.setStyle(btn.getStyle() +
                        "-fx-background-color: " + (isDarkMode ? "#313244" : "#c8cdd8") + ";" +
                        "-fx-text-fill: " + text + ";" +
                        "-fx-border-color: " + border + ";"
                    );
                }
            });
        }

        if (grid != null) {
            grid.setStyle("-fx-background-color: " + border + "; -fx-padding: 2;");
        }

        if (board != null && board.isWon()) {
            statusLabel.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + (isDarkMode ? "#a6e3a1" : "#2e7d32") + ";"
            );
        } else if (board != null && board.isGameOver()) {
            statusLabel.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + (isDarkMode ? "#f38ba8" : "#c62828") + ";"
            );
        }
    }

    // ── Timer ─────────────────────────────────────────────────────

    private void startTimer() {
        secondsElapsed = 0;
        if (timer != null) timer.stop();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            timerLabel.setText("⏱ " + secondsElapsed + "s");
        }));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
    }

    // ── Reset ─────────────────────────────────────────────────────

    private void resetGame() {
        if (timer != null) timer.stop();
        flagsPlaced = 0;
        mineCountLabel.setText("💣 " + M);
        timerLabel.setText("⏱ 0s");
        resetBtn.setText("😊");
        statusLabel.setText("");
        buildGrid();
        applyTheme();
        startTimer();
        updateUI();
        rebindCellSizes();
    }

    // ── UI update ─────────────────────────────────────────────────

    private void updateUI() {
        if (board == null || buttons == null) return;
        String[] numColors = isDarkMode ? DARK_NUMBER_COLORS : LIGHT_NUMBER_COLORS;

        for (int r = 0; r < R; r++) {
            for (int c = 0; c < C; c++) {
                Cell cell  = board.getCell(r, c);
                Button btn = buttons[r][c];

                if (cell.isRevealed()) {
                    if (cell.isMine()) {
                        btn.setText("X");
                        btn.setStyle(mineStyle());
                    } else {
                        int n = cell.getNeighborMines();
                        btn.setText(n == 0 ? "" : String.valueOf(n));
                        btn.setStyle(revealedStyle(n, numColors));
                    }
                    btn.setDisable(true);

                } else if (cell.isFlagged()) {
                    btn.setText("F");
                    btn.setStyle(flaggedStyle());
                    btn.setDisable(false);

                } else {
                    btn.setText("");
                    btn.setStyle(unrevealedStyle());
                    btn.setDisable(false);
                }
            }
        }

        if (board.isGameOver()) {
            timer.stop();
            resetBtn.setText("😵");
            statusLabel.setText("✖ Game Over!");
            statusLabel.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + (isDarkMode ? "#f38ba8" : "#c62828") + ";"
            );
        } else if (board.isWon()) {
            timer.stop();
            resetBtn.setText("😎");
            statusLabel.setText("★ You Won!");
            statusLabel.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + (isDarkMode ? "#a6e3a1" : "#2e7d32") + ";"
            );
        }
    }

    // ── Cell styles ───────────────────────────────────────────────

    private String unrevealedStyle() {
        String bg     = isDarkMode ? D_UNREVEALED : L_UNREVEALED;
        String border = isDarkMode ? D_BORDER     : L_BORDER;
        return "-fx-background-color: " + bg + ";" +
               "-fx-border-color: " + border + ";" +
               "-fx-border-width: 1; -fx-background-radius: 3;" +
               "-fx-border-radius: 3; -fx-padding: 0; -fx-cursor: hand;";
    }

    private String revealedStyle(int n, String[] numColors) {
        return revealedStyle(n, numColors, 14);
    }

    private String revealedStyle(int n, String[] numColors, double fontSize) {
        String bg     = isDarkMode ? D_REVEALED : L_REVEALED;
        String border = isDarkMode ? D_BORDER   : L_BORDER;
        String color  = (n > 0 && n <= 8)
                        ? numColors[n]
                        : (isDarkMode ? D_TEXT_DIM : L_TEXT_DIM);
        return "-fx-background-color: " + bg + ";" +
               "-fx-border-color: " + border + ";" +
               "-fx-border-width: 1; -fx-background-radius: 3;" +
               "-fx-border-radius: 3; -fx-padding: 0;" +
               "-fx-text-fill: " + color + ";" +
               "-fx-font-weight: bold; -fx-font-size: " + fontSize + "px;";
    }

    private String flaggedStyle() {
        String bg     = isDarkMode ? D_FLAGGED : L_FLAGGED;
        String border = isDarkMode ? D_BORDER  : L_BORDER;
        String text   = isDarkMode ? "#f38ba8" : "#c62828";
        return "-fx-background-color: " + bg + ";" +
               "-fx-border-color: " + border + ";" +
               "-fx-border-width: 1; -fx-background-radius: 3;" +
               "-fx-border-radius: 3; -fx-padding: 0;" +
               "-fx-text-fill: " + text + ";" +
               "-fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;";
    }

    private String mineStyle() {
        String bg   = isDarkMode ? D_MINE    : L_MINE;
        String text = isDarkMode ? "#1e1e2e" : "#ffffff";
        return "-fx-background-color: " + bg + ";" +
               "-fx-border-width: 1; -fx-background-radius: 3;" +
               "-fx-border-radius: 3; -fx-padding: 0;" +
               "-fx-text-fill: " + text + ";" +
               "-fx-font-weight: bold; -fx-font-size: 14px;";
    }

    public static void main(String[] args) {
        launch();
    }
}
