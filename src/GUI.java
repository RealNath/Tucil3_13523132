import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Arrays;

public class GUI extends JFrame {
    private JPanel gridPanel;
    private JButton loadButton;
    private final Map<Character, Color> colorMap = new HashMap<>();
    private final Random random = new Random();
    private JButton greedyBestButton, ucsButton, aStarButton;
    private char[][] gridState;
    private int gridRows, gridColumns;
    private Point kLocation = null;
    
    // Animasi
    private Timer animationTimer;
    private List<String> currentSolutionPathForAnimation;
    private int animationMoveIndex;
    private long searchExecutionTime;
    private long animationStartTime;

    public GUI() {
        setTitle("Block Grid Renderer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        gridPanel = new JPanel();

        loadButton = new JButton("Load File");
        greedyBestButton = new JButton("Greedy Best First Search");
        greedyBestButton.setEnabled(false);
        ucsButton = new JButton("Uniform Cost Search");
        ucsButton.setEnabled(false);
        aStarButton = new JButton("A*");
        aStarButton.setEnabled(false);

        loadButton.addActionListener(e -> {
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop(); // Stop animasi kalau file baru di-load
            }
            loadFile();
        });
        greedyBestButton.addActionListener(e -> {
            if (gridState == null || kLocation == null) {
                JOptionPane.showMessageDialog(this, "Tolong load file puzzle valid dengan 'K'", "Error (Greedy Best First Search)", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop(); // Stop animasi sebelumnya kalau A* dijalankan lagi
            }

            System.out.println("\nGreedy Best First Search Solver Dimulai");
            greedyBestButton.setEnabled(false);
            loadButton.setEnabled(false);
            aStarButton.setEnabled(false);
            ucsButton.setEnabled(false);

            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                List<String> solutionPath = solveGreedyBestFirstSearch(startTime);
                long endTime = System.currentTimeMillis();
                this.searchExecutionTime = endTime - startTime;
                System.out.println("Greedy Best First Search Solver berakhir dalam " + this.searchExecutionTime + " ms.");

                SwingUtilities.invokeLater(() -> {
                    if (solutionPath != null && !solutionPath.isEmpty()) {
                        System.out.println("\nPencarian dengan Greedy Best First Search (" + solutionPath.size() + " pergerakan):");
                        for (String move : solutionPath) {
                            System.out.println(move);
                        }
                        animateSolution(solutionPath);
                    } else {
                        // Kalau tidak ada solusi atau error saat solveGreedyBestFirstSearch
                        greedyBestButton.setEnabled(true);
                        loadButton.setEnabled(true);
                        aStarButton.setEnabled(gridState != null);
                        ucsButton.setEnabled(gridState != null);
                    }
                });
            }).start();
        });
        ucsButton.addActionListener(e -> {
            if (gridState == null || kLocation == null) {
                JOptionPane.showMessageDialog(this, "Tolong load file puzzle valid dengan 'K'", "Error (Uniform Cost Search)", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop(); // Stop previous animation if UCS is run again
            }

            System.out.println();
            System.out.println("\nUniform Cost Search Solver Dimulai");
            ucsButton.setEnabled(false);
            loadButton.setEnabled(false);
            greedyBestButton.setEnabled(false);
            aStarButton.setEnabled(false);

            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                List<String> solutionPath = solveUniformCostSearch(startTime);
                long endTime = System.currentTimeMillis();
                this.searchExecutionTime = endTime - startTime;
                System.out.println("Uniform Cost Search Solver berakhir dalam " + this.searchExecutionTime + " ms.");

                SwingUtilities.invokeLater(() -> {
                    if (solutionPath != null && !solutionPath.isEmpty()) {
                        System.out.println("\nPencarian dengan Uniform Cost Search (" + solutionPath.size() + " pergerakan):");
                        for (String move : solutionPath) {
                            System.out.println(move);
                        }
                        animateSolution(solutionPath); // Call animation method
                    } else {
                        ucsButton.setEnabled(true);
                        loadButton.setEnabled(true);
                        greedyBestButton.setEnabled(gridState != null);
                        aStarButton.setEnabled(gridState != null);
                    }
                    // Buttons re-enabled by animateSolution or if no solution
                });
            }).start();
        });
        aStarButton.addActionListener(e -> {
            if (gridState == null || kLocation == null) {
                JOptionPane.showMessageDialog(this, "Tolong load file puzzle valid dengan 'K'", "Error (A*)", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }

            System.out.println("\nA* Solver Dimulai");
            aStarButton.setEnabled(false);
            loadButton.setEnabled(false);
            greedyBestButton.setEnabled(false);
            ucsButton.setEnabled(false);

            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                List<String> solutionPath = solveAStar(startTime);
                long endTime = System.currentTimeMillis();
                this.searchExecutionTime = endTime - startTime;
                System.out.println("A* Solver berakhir dalam " + this.searchExecutionTime + " ms.");
                
                SwingUtilities.invokeLater(() -> {
                    if (solutionPath != null && !solutionPath.isEmpty()) {
                        System.out.println("\nPencarian dengan A* (" + solutionPath.size() + " pergerakan):");
                        for (String move : solutionPath) {
                            System.out.println(move);
                        }
                        animateSolution(solutionPath); // Call animation method
                    } else {
                        // Tidak ada solusi atau error saat solveAStar
                        aStarButton.setEnabled(true);
                        loadButton.setEnabled(true);
                        greedyBestButton.setEnabled(gridState != null);
                        ucsButton.setEnabled(gridState != null);
                    }
                });
            }).start();
        });
        
        JPanel methodRow = new JPanel(new GridLayout(1,3,5,0));
        methodRow.add(greedyBestButton);
        methodRow.add(ucsButton);
        methodRow.add(aStarButton);
        
        JPanel topPanel = new JPanel(new BorderLayout(0,5));
        topPanel.add(loadButton, BorderLayout.NORTH);
        topPanel.add(methodRow, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);
        add(gridPanel, BorderLayout.CENTER);

        // Ukuran awal window
        setSize(600, 600);
        // Tengah layar
        setLocationRelativeTo(null);
    }

    private void loadFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("..\\test"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            parseAndRenderFile(selectedFile);
        }
    }

    public static int getMaxLength(String filePath) {
        int maxLength = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // Skip dua baris pertama
            reader.readLine();
            reader.readLine();
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() > maxLength) {
                    maxLength = line.length();
                }
            }
        } catch (IOException e) {
            System.err.println("Error membaca file teks untuk mencari panjang baris maksimal: " + e.getMessage());
            return 0;
        }
        return maxLength;
    }

    public static int getLineCount(String filePath) {
        int lineCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            while (reader.readLine() != null) {
                lineCount++;
            }
        } catch (IOException e) {
            System.err.println("Error membaca file teks: " + e.getMessage());
            return 0;
        }
        lineCount -= 2; // dua baris pertama
        return lineCount;
    }

    private void parseAndRenderFile(File file) {
        char[][] newGridData = null;
        Point newKLocationFromFile = null;
        int gridRow = 0;
        int gridCol = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // Baris pertama
            String firstLine = reader.readLine();
            if (firstLine == null) {
                JOptionPane.showMessageDialog(this, "File kosong atau format header salah.", "Error (File Teks)", JOptionPane.ERROR_MESSAGE);
                resetPuzzleStateAndButtons();
                return;
            }
            String[] dimensionsStr = firstLine.trim().split("\\s+");
            if (dimensionsStr.length < 2) {
                JOptionPane.showMessageDialog(this, "Format baris pertama salah (dimensi). Harus ada dua angka.", "Error (File Teks)", JOptionPane.ERROR_MESSAGE);
                resetPuzzleStateAndButtons();
                return;
            }
            int row = Integer.parseInt(dimensionsStr[0]);
            int col = Integer.parseInt(dimensionsStr[1]);

            // Baris kedua
            if (reader.readLine() == null) { // piecesLine
                JOptionPane.showMessageDialog(this, "File tidak lengkap, baris kedua (jumlah piece) hilang.", "Error (File Teks)", JOptionPane.ERROR_MESSAGE);
                resetPuzzleStateAndButtons();
                return;
            }

            // Ukuran grid sebenarnya
            gridRow = getLineCount(file.getPath());
            gridCol = getMaxLength(file.getPath());

            if (gridRow <= 0) {
                 JOptionPane.showMessageDialog(this, "Tidak ada data.", "Error (File Teks)", JOptionPane.ERROR_MESSAGE);
                resetPuzzleStateAndButtons();
                return;
            }

            newGridData = new char[gridRow][gridCol];

            // 3. Read grid data and Validate 'K' Position
            for (int i = 0; i < gridRow; i++) {
                String line = reader.readLine();
                if (line == null) {
                    JOptionPane.showMessageDialog(this, "Struktur file tidak konsisten: jumlah baris data aktual lebih sedikit dari yang dihitung.", "Error (File Teks)", JOptionPane.ERROR_MESSAGE);
                    resetPuzzleStateAndButtons();
                    return;
                }
                for (int j = 0; j < gridCol; j++) {
                    if (j < line.length()) {
                        char c = line.charAt(j);
                        if (c == 'K') {
                            // Kalau ada lebih dari satu 'K'
                            if (newKLocationFromFile != null) {
                                JOptionPane.showMessageDialog(this, "Error: Ada lebih dari satu 'K' dalam file.", "Error (File Teks)", JOptionPane.ERROR_MESSAGE);
                                resetPuzzleStateAndButtons();
                                return;
                            }
                            newKLocationFromFile = new Point(i, j);
                            newGridData[i][j] = '.'; // Replace 'K' with '.' for solver logic
                        } else {
                            newGridData[i][j] = c;
                        }
                    } else {
                        newGridData[i][j] = ' '; // Fill short lines with spaces (or '.')
                    }
                }
            }

            // Kalau tidak ada 'K'
            if (newKLocationFromFile == null) {
                JOptionPane.showMessageDialog(this, "Error: Tidak ada 'K' (tujuan) yang ditemukan dalam file.", "Error (File Teks)", JOptionPane.ERROR_MESSAGE);
                resetPuzzleStateAndButtons();
                return;
            }

            // If all validations pass, update the GUI state
            this.gridState = newGridData;
            this.kLocation = newKLocationFromFile;
            this.gridRows = gridRow;
            this.gridColumns = gridCol;

            // For debugging piece identification:
            List<Piece> pieces = findPieces(this.gridState, this.gridRows, this.gridColumns);
            System.out.println("\nPiece ditemukan di " + file.getName() + ":");
            for (Piece b : pieces) {
                String orientation = "single";
                try {
                    if (b.cells.size() >= 2) {
                        if (b.isHorizontal()) { orientation = "horizontal"; }
                        else { orientation = "vertical"; }
                    }
                    else {
                        throw new AbnormalPieceException("Piece " + b.id + " abnormal");
                    }
                    // else orientation = "abnormal";
                } catch (AbnormalPieceException e) {
                    JOptionPane.showMessageDialog(this, e.getMessage() + "\n" + file.getName(), "Error (File Teks)", JOptionPane.ERROR_MESSAGE);
                    resetPuzzleStateAndButtons();
                }
                System.out.println("piece '"+b.id+"' is "+orientation+" at "+b.cells);
            }

            greedyBestButton.setEnabled(true);
            ucsButton.setEnabled(true);
            aStarButton.setEnabled(true); // kLocation is confirmed non-null here

            renderGrid(this.gridState, this.gridRows, this.gridColumns);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saat membaca file: " + e.getMessage(), "Error (File Teks)", JOptionPane.ERROR_MESSAGE);
            resetPuzzleStateAndButtons();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Format angka salah di header file: " + e.getMessage(), "Error (File Teks)", JOptionPane.ERROR_MESSAGE);
            resetPuzzleStateAndButtons();
        } catch (ArrayIndexOutOfBoundsException e) {
            JOptionPane.showMessageDialog(this, "Format header file tidak benar: " + e.getMessage(), "Error (File Teks)", JOptionPane.ERROR_MESSAGE);
            resetPuzzleStateAndButtons();
        }
    }

    // Helper untuk reset keadaan puzzle dan buat tombol solver grayed out
    private void resetPuzzleStateAndButtons() {
        this.gridState = null;
        this.kLocation = null;
        this.gridRows = 0;
        this.gridColumns = 0;
        greedyBestButton.setEnabled(false);
        ucsButton.setEnabled(false);
        aStarButton.setEnabled(false);
        gridPanel.removeAll();
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    public void moveUp(Piece g) {
        if (g.isHorizontal()) {
            System.out.println("Tidak bisa ke atas: piece horizontal");
            return;
        }
        // cari baris paling atas dari group
        int minR = g.cells.stream().mapToInt(p->p.x).min().getAsInt();
        int col  = g.cells.get(0).y;
        if (minR == 0 || gridState[minR-1][col] != '.') {
            System.out.println("Tidak bisa ke atas: tabrakan dengan piece lain atau di sisi atas");
            return;
        }
        // bersihkan yang sebelumnya
        for (Point p : g.cells) gridState[p.x][p.y] = '.';
        // geser posisi
        for (Point p : g.cells) {
            p.x--;
            gridState[p.x][p.y] = g.id;
        }
        renderGrid(gridState, gridRows, gridColumns);
    }

    public void moveDown(Piece g) {
        if (g.isHorizontal()) {
            System.out.println("Tidak bisa ke bawah: piece horizontal");
            return;
        }
        int maxR = g.cells.stream().mapToInt(p->p.x).max().getAsInt();
        int col  = g.cells.get(0).y;
        if (maxR == gridRows-1 || gridState[maxR+1][col] != '.') {
            System.out.println("Tidak bisa ke bawah: tabrakan dengan piece lain atau di sisi bawah");
            return;
        }
        for (Point p : g.cells) gridState[p.x][p.y] = '.';
        for (Point p : g.cells) {
            p.x++;
            gridState[p.x][p.y] = g.id;
        }
        renderGrid(gridState, gridRows, gridColumns);
    }

    public void moveLeft(Piece g) {
        if (!g.isHorizontal()) {
            System.out.println("Tidak bisa ke kiri: piece vertikal");
            return;
        }
        int row = g.cells.get(0).x;
        int minC = g.cells.stream().mapToInt(p->p.y).min().getAsInt();
        if (minC == 0 || gridState[row][minC-1] != '.') {
            System.out.println("Tidak bisa ke kiri: tabrakan dengan piece lain atau di sisi kiri");
            return;
        }
        for (Point p : g.cells) gridState[p.x][p.y] = '.';
        for (Point p : g.cells) {
            p.y--;
            gridState[p.x][p.y] = g.id;
        }
        renderGrid(gridState, gridRows, gridColumns);
    }

    public void moveRight(Piece g) {
        if (!g.isHorizontal()) {
            System.out.println("Tidak bisa ke kanan: piece vertikal");
            return;
        }
        int row = g.cells.get(0).x;
        int maxC = g.cells.stream().mapToInt(p->p.y).max().getAsInt();
        if (maxC == gridColumns-1 || gridState[row][maxC+1] != '.') {
            System.out.println("Tidak bisa ke kanan: tabrakan dengan piece lain atau di sisi kanan");
            return;
        }
        for (Point p : g.cells) gridState[p.x][p.y] = '.';
        for (Point p : g.cells) {
            p.y++;
            gridState[p.x][p.y] = g.id;
        }
        renderGrid(gridState, gridRows, gridColumns);
    }

    private void renderGrid(
        char[][] grid, int rows, int columns) {
        // Clear the grid panel and reset layout
        gridPanel.removeAll();
        gridPanel.setLayout(new GridLayout(rows, columns));
        
        // Create a 2D array to hold all cell panels
        JPanel[][] cells = new JPanel[rows][columns];
        
        // First create all cells
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                cells[i][j] = new JPanel();
                cells[i][j].setLayout(new BorderLayout());
                // cells[i][j].setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            }
        }
        
        // Now set colors and content for each cell
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                char c = grid[i][j];
                
                if (c == '.') {
                    // Empty cell
                    cells[i][j].setBackground(Color.WHITE);
                } else if (c == ' ') {
                    cells[i][j].setBackground(Color.BLACK);
                } else {
                    // Get or create color for this character
                    if (!colorMap.containsKey(c)) {
                        if (c == 'P') {
                            colorMap.put(c, Color.RED);
                        } else {
                            // Generate a random color that's not too light
                            Color randomColor = new Color(
                                    random.nextInt(200),
                                    random.nextInt(200),
                                    random.nextInt(200)
                            );
                            colorMap.put(c, randomColor);
                        }
                    }
                    cells[i][j].setBackground(colorMap.get(c));
                    
                    // Add character label
                    JLabel label = new JLabel(String.valueOf(c), JLabel.CENTER);
                    label.setForeground(Color.WHITE);
                    cells[i][j].add(label, BorderLayout.CENTER);
                }
            }
        }
        
        // Example: Set specific colors manually
        // cells[2][3].setBackground(Color.RED);  // Set cell at row 2, column 3 to red
        // cells[4][1].setBackground(Color.BLUE); // Set cell at row 4, column 1 to blue
        
        // Example: Copy color from one cell to another
        // cells[5][2].setBackground(cells[1][1].getBackground());
        
        // Add cells to grid panel in order
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                gridPanel.add(cells[i][j]);
            }
        }
        
        // Refresh the UI
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    public static class AbnormalPieceException extends RuntimeException {
        public AbnormalPieceException(String message) {
            super(message);
        }
    }

    // kumpulan cell yang saling terhubung
    private static class Piece {
        final char id;
        final List<Point> cells = new ArrayList<>();
        Piece(char id) { this.id = id; }

        // return true kalau horizontal, false kalau vertikal, pop up window kalau dua-duanya baris dan kolom >= 2 (hanya boleh salah satu)
        public boolean isHorizontal() {
            if (cells.size() < 2) {
                throw new AbnormalPieceException("Piece " + id + " abnormal");
                // System.err.println("Piece " + id + " berukuran 1x1 atau lebih kecil");
                // System.exit(0);
            }
            // int row0 = cells.get(0).x;
            // int col0 = cells.get(0).y;

            // cek kalau horizontal
            if ((cells.get(1).x == cells.get(0).x) && (cells.get(1).y != cells.get(0).y)) {
                return true;
            }
            // kalau bukan, cek apakah vertikal
            else if ((cells.get(1).x != cells.get(0).x) && (cells.get(1).y == cells.get(0).y)) {
                return false;
            } 
            // kalau bukan keduanya, ada yang salah
            else {
                throw new AbnormalPieceException("Piece " + id + " abnormal");
            }
            // return false;
        }
    }

    /** iterasi grid, return each connected piece as a BlockGroup */
    private List<Piece> findPieces(char[][] grid, int rows, int cols) {
        boolean[][] used = new boolean[rows][cols];
        List<Piece> groups = new ArrayList<>();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                char c = grid[i][j];
                if (c==' '||c=='.'|| used[i][j]) continue;

                Piece g = new Piece(c);
                used[i][j] = true;
                g.cells.add(new Point(i,j));

                // check horizontal
                if (j+1<cols && grid[i][j+1]==c) {
                    for (int x=j+1; x<cols && grid[i][x]==c; x++) {
                        used[i][x]=true;
                        g.cells.add(new Point(i,x));
                    }
                }
                // else check vertical
                else if (i+1<rows && grid[i+1][j]==c) {
                    for (int y=i+1; y<rows && grid[y][j]==c; y++) {
                        used[y][j]=true;
                        g.cells.add(new Point(y,j));
                    }
                }
                // else it remains a single‐tile group

                groups.add(g);
            }
        }
        return groups;
    }

    // Helper class for A* states
    class PuzzleState {
        char[][] board;
        int gCost; // Nilai/harga dari start ke state ini
        int hCost; // Nilai/harga heuristik ke tujuan
        PuzzleState parent;
        String moveToReachThisState; // misal "A_UP", "P_RIGHT"
        int boardRows;
        int boardCols;

        public PuzzleState(char[][] board, int gCost, int hCost, PuzzleState parent, String move, int r, int c) {
            this.board = board;
            this.gCost = gCost;
            this.hCost = hCost;
            this.parent = parent;
            this.moveToReachThisState = move;
            this.boardRows = r;
            this.boardCols = c;
        }

        public int getFCost() {
            return gCost + hCost;
        }

        public String getBoardString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < boardRows; i++) {
                for (int j = 0; j < boardCols; j++) {
                    sb.append(board[i][j]);
                }
            }
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PuzzleState that = (PuzzleState) o;
            return Arrays.deepEquals(board, that.board);
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(board);
        }
    }

    // Heuristic: Manhattan distance dari cell terdekat 'P' ke K
    private int calculateHCost(char[][] currentBoard, int r, int c, Point exitLocation) {
        if (exitLocation == null) return Integer.MAX_VALUE;

        Piece primaryPiece = null;
        List<Piece> groups = findPieces(currentBoard, r, c);
        for (Piece group : groups) {
            if (group.id == 'P') {
                primaryPiece = group;
                break;
            }
        }

        if (primaryPiece == null || primaryPiece.cells.isEmpty()) {
            return Integer.MAX_VALUE; // 'P' tidak ketemu
        }

        int minManhattanDistance = Integer.MAX_VALUE;
        for (Point primaryCell : primaryPiece.cells) {
            int dist = Math.abs(primaryCell.x - exitLocation.x) + Math.abs(primaryCell.y - exitLocation.y);
            minManhattanDistance = Math.min(minManhattanDistance, dist);
        }
        return minManhattanDistance;
    }

    private boolean isGoalState(char[][] currentBoard, int r, int c, Point kGoalLocation) {
        if (kGoalLocation == null) return false;

        Piece pBlock = null;
        List<Piece> groups = findPieces(currentBoard, r, c);
        for (Piece group : groups) {
            if (group.id == 'P') {
                pBlock = group;
                break;
            }
        }

        if (pBlock == null) {
            return false; // 'P' not found
        }

        for (Point pCell : pBlock.cells) {
            if (pCell.x == kGoalLocation.x && pCell.y == kGoalLocation.y) {
                return true; // A part of 'P' is at K's original location
            }
        }
        return false;
    }

    private char[][] deepCopyBoard(char[][] original, int r, int c) {
        char[][] copy = new char[r][c];
        for (int i = 0; i < r; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, c);
        }
        return copy;
    }

    private List<PuzzleState> getSuccessors(PuzzleState currentState, Point kGoalLocation) {
        List<PuzzleState> successors = new ArrayList<>();
        char[][] currentBoard = currentState.board;
        int r = currentState.boardRows;
        int c = currentState.boardCols;

        List<Piece> blockGroups = findPieces(currentBoard, r, c);

        for (Piece group : blockGroups) {
            if (group.cells.isEmpty()) continue;

            // Coba ke atas
            if (!group.isHorizontal() || group.cells.size() == 1) { // Vertical or single blocks can try to move vertically
                char[][] nextBoardUp = tryMoveGroup(currentBoard, group, r, c, -1, 0);
                if (nextBoardUp != null) {
                    int h = calculateHCost(nextBoardUp, r, c, kGoalLocation);
                    successors.add(new PuzzleState(nextBoardUp, currentState.gCost + 1, h, currentState, group.id + "_UP", r, c));
                }
            }
            // Coba ke bawah
            if (!group.isHorizontal() || group.cells.size() == 1) {
                char[][] nextBoardDown = tryMoveGroup(currentBoard, group, r, c, 1, 0);
                if (nextBoardDown != null) {
                    int h = calculateHCost(nextBoardDown, r, c, kGoalLocation);
                    successors.add(new PuzzleState(nextBoardDown, currentState.gCost + 1, h, currentState, group.id + "_DOWN", r, c));
                }
            }
            // Coba ke kiri
            if (group.isHorizontal() || group.cells.size() == 1) { // Horizontal or single blocks can try to move horizontally
                char[][] nextBoardLeft = tryMoveGroup(currentBoard, group, r, c, 0, -1);
                if (nextBoardLeft != null) {
                    int h = calculateHCost(nextBoardLeft, r, c, kGoalLocation);
                    successors.add(new PuzzleState(nextBoardLeft, currentState.gCost + 1, h, currentState, group.id + "_LEFT", r, c));
                }
            }
            // Coba ke kanan
            if (group.isHorizontal() || group.cells.size() == 1) {
                char[][] nextBoardRight = tryMoveGroup(currentBoard, group, r, c, 0, 1);
                if (nextBoardRight != null) {
                    int h = calculateHCost(nextBoardRight, r, c, kGoalLocation);
                    successors.add(new PuzzleState(nextBoardRight, currentState.gCost + 1, h, currentState, group.id + "_RIGHT", r, c));
                }
            }
        }
        return successors;
    }

    // Helper untuk mencoba pergerakan dan return keadaan board baru, atau null kalau tdk valid
    private char[][] tryMoveGroup(char[][] board, Piece group, int rows, int cols, int dRow, int dCol) {
        char[][] newBoard = deepCopyBoard(board, rows, cols);

        // Bersihkan cell sekarang sebuah group pada newBoard secara sementara
        // untuk mengecek self-collision dengan benar
        for (Point p : group.cells) {
            newBoard[p.x][p.y] = '.';
        }

        // Cek kalau semua posisi valid dan kosong (pada board yang mana piece-nya barusan dibersihkan)
        for (Point p : group.cells) {
            int nextR = p.x + dRow;
            int nextC = p.y + dCol;
            // Kalau lewat batas
            if (nextR < 0 || nextR >= rows || nextC < 0 || nextC >= cols) return null; // Lewat batas
            // Kalau tumpang tindih dengan piece lain atau kotak hitam
            if (newBoard[nextR][nextC] != '.') return null;
        }

        // Kalau valid, set posisi baru secara permanen pada fresh copy (atau pada newBoard yang baru dimodif)
        // newBoard sudah dibersihkan tadi, tinggal isi aja.
        for (Point p : group.cells) {
            newBoard[p.x + dRow][p.y + dCol] = group.id;
        }
        return newBoard;
    }

    private void animateSolution(List<String> solutionPath) {
        this.currentSolutionPathForAnimation = solutionPath;
        this.animationMoveIndex = 0;

        // Stop timer yang ada
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        // Update board tiap 700 ms
        animationTimer = new Timer(700, e -> performNextAnimationStep());
        // Mulai pergerakan pertama langsung
        animationTimer.setInitialDelay(0);
        this.animationStartTime = System.currentTimeMillis();
        animationTimer.start();
    }

    private void performNextAnimationStep() {
        if (animationMoveIndex < currentSolutionPathForAnimation.size()) {
            String move = currentSolutionPathForAnimation.get(animationMoveIndex);
            char pieceId = move.charAt(0);
            String direction = move.substring(move.indexOf('_') + 1);

            // Cari piece pada gridState sekarang
            List<Piece> currentPieces = findPieces(this.gridState, this.gridRows, this.gridColumns);
            Piece targetPiece = null;
            for (Piece bg : currentPieces) {
                if (bg.id == pieceId) {
                    targetPiece = bg;
                    break;
                }
            }

            if (targetPiece != null) {
                System.out.println("Animasi pergerakan: " + move);
                if ("UP".equals(direction)) { moveUp(targetPiece); }
                else if ("DOWN".equals(direction)) { moveDown(targetPiece); }
                else if ("LEFT".equals(direction)) { moveLeft(targetPiece); }
                else if ("RIGHT".equals(direction)) { moveRight(targetPiece); }
            } else {
                System.err.println("Error pada animasi: Tidak ada piece '" + pieceId + "' untuk pergerakan " + move);
                // Stop animasi/skip?
            }
            animationMoveIndex++;
        } else {
            animationTimer.stop();
            long animationEndTime = System.currentTimeMillis();
            long animationTime = animationEndTime - animationStartTime;
            System.out.println("Animation finished.");
            String output = "Solusi ditemukan dalam " + currentSolutionPathForAnimation.size() + " pergerakan." +
                            "\nWaktu pencarian: " + this.searchExecutionTime + " ms" +
                            "\nDurasi animasi: " + animationTime + " ms";
            JOptionPane.showMessageDialog(this, output, "Solusi", JOptionPane.INFORMATION_MESSAGE);
            System.out.println(output);
            aStarButton.setEnabled(true);
            loadButton.setEnabled(true);
            greedyBestButton.setEnabled(gridState != null);
            ucsButton.setEnabled(gridState != null);
        }
    }

    public List<String> solveAStar(long startTimeMillis) { // Added startTimeMillis
        if (this.gridState == null) {
            System.out.println("Tidak ada board yang di-load.");
            return null;
        }
        if (this.kLocation == null) {
            System.out.println("Tidak ada 'K'. Tidak bisa dicari solusinya");
            // This popup is fine as it's a pre-condition fail, not a search fail.
            JOptionPane.showMessageDialog(this, "Tidak ada 'K' (tujuan). Tidak bisa dicari solusinya.", "Error (A*)", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        PriorityQueue<PuzzleState> openSet = new PriorityQueue<>(Comparator.comparingInt(PuzzleState::getFCost));
        Set<String> closedSet = new HashSet<>();

        char[][] initialBoard = deepCopyBoard(this.gridState, this.gridRows, this.gridColumns);
        int initialHCost = calculateHCost(initialBoard, this.gridRows, this.gridColumns, this.kLocation);
        PuzzleState initialState = new PuzzleState(initialBoard, 0, initialHCost, null, "START", this.gridRows, this.gridColumns);

        openSet.add(initialState);

        int iterations = 0;
        final int MAX_ITERATIONS = 500000; 

        while (!openSet.isEmpty()) {
            iterations++;
            if (iterations > MAX_ITERATIONS) {
                long execTimeSoFar = System.currentTimeMillis() - startTimeMillis;
                System.out.println("Batas iterasi A* (" + MAX_ITERATIONS + ") tercapai.");
                JOptionPane.showMessageDialog(this,
                        "Batas iterasi A* (" + MAX_ITERATIONS + ") tercapai.\nWaktu eksekusi: " + execTimeSoFar + " ms",
                        "Perhatian (A*)", JOptionPane.WARNING_MESSAGE);
                return null;
            }

            PuzzleState currentState = openSet.poll();
            String currentBoardStr = currentState.getBoardString();

            if (closedSet.contains(currentBoardStr)) {
                continue;
            }
            closedSet.add(currentBoardStr);

            if (isGoalState(currentState.board, currentState.boardRows, currentState.boardCols, this.kLocation)) {
                System.out.println("Solusi A* ketemu dalam " + iterations + " iterasi! gCost: " + currentState.gCost);
                return reconstructPath(currentState);
            }

            List<PuzzleState> successors = getSuccessors(currentState, this.kLocation);
            for (PuzzleState successor : successors) {
                if (!closedSet.contains(successor.getBoardString())) {
                    openSet.add(successor);
                }
            }
        }
        long execTimeSoFar = System.currentTimeMillis() - startTimeMillis;
        System.out.println("Tidak ada solusi A* yang ketemu setelah " + iterations + " iterasi.");
        JOptionPane.showMessageDialog(this,
                "Tidak ada solusi A* yang ketemu setelah " + iterations + " iterasi.\nWaktu eksekusi: " + execTimeSoFar + " ms",
                "Hasil (A*)", JOptionPane.INFORMATION_MESSAGE);
        return null;
    }

    private List<String> reconstructPath(PuzzleState goalState) {
        List<String> path = new ArrayList<>();
        PuzzleState current = goalState;
        while (current != null && current.moveToReachThisState != null && !current.moveToReachThisState.equals("START")) {
            path.add(current.moveToReachThisState);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    public List<String> solveGreedyBestFirstSearch(long startTimeMillis) { // Added startTimeMillis
        if (this.gridState == null) {
            System.out.println("Tidak ada board yang di-load (GBFS).");
            return null;
        }
        if (this.kLocation == null) {
            System.out.println("Tidak ada 'K'. Tidak bisa dicari solusinya (GBFS).");
            JOptionPane.showMessageDialog(this, "Tidak ada 'K' (tujuan). Tidak bisa dicari solusinya (GBFS).", "Error (GBFS)", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        PriorityQueue<PuzzleState> openSet = new PriorityQueue<>(Comparator.comparingInt(state -> state.hCost));
        Set<String> closedSet = new HashSet<>();

        char[][] initialBoard = deepCopyBoard(this.gridState, this.gridRows, this.gridColumns);
        int initialHCost = calculateHCost(initialBoard, this.gridRows, this.gridColumns, this.kLocation);
        PuzzleState initialState = new PuzzleState(initialBoard, 0, initialHCost, null, "START", this.gridRows, this.gridColumns);

        openSet.add(initialState);

        int iterations = 0;
        final int MAX_ITERATIONS = 500000; 

        while (!openSet.isEmpty()) {
            iterations++;
            if (iterations > MAX_ITERATIONS) {
                long execTimeSoFar = System.currentTimeMillis() - startTimeMillis;
                System.out.println("Batas iterasi GBFS (" + MAX_ITERATIONS + ") tercapai.");
                JOptionPane.showMessageDialog(this,
                        "Batas iterasi GBFS (" + MAX_ITERATIONS + ") tercapai.\nWaktu eksekusi: " + execTimeSoFar + " ms",
                        "Perhatian (GBFS)", JOptionPane.WARNING_MESSAGE);
                return null;
            }

            PuzzleState currentState = openSet.poll();
            String currentBoardStr = currentState.getBoardString();

            if (closedSet.contains(currentBoardStr)) {
                continue;
            }
            closedSet.add(currentBoardStr);

            if (isGoalState(currentState.board, currentState.boardRows, currentState.boardCols, this.kLocation)) {
                System.out.println("Solusi GBFS ketemu dalam " + iterations + " iterasi! Path length (gCost): " + currentState.gCost);
                return reconstructPath(currentState);
            }

            List<PuzzleState> successors = getSuccessors(currentState, this.kLocation);
            for (PuzzleState successor : successors) {
                if (!closedSet.contains(successor.getBoardString())) {
                    openSet.add(successor);
                }
            }
        }
        long execTimeSoFar = System.currentTimeMillis() - startTimeMillis;
        System.out.println("Tidak ada solusi Greedy Best First Search yang ketemu setelah " + iterations + " iterasi.");
        JOptionPane.showMessageDialog(this,
                "Tidak ada solusi Greedy Best First Search yang ketemu setelah " + iterations + " iterasi.\nWaktu eksekusi: " + execTimeSoFar + " ms",
                "Hasil (Greedy Best First Search)", JOptionPane.INFORMATION_MESSAGE);
        return null;
    }

    public List<String> solveUniformCostSearch(long startTimeMillis) { // Added startTimeMillis
        if (this.gridState == null) {
            System.out.println("Tidak ada board yang di-load (Uniform Cost Search).");
            return null;
        }
        if (this.kLocation == null) {
            System.out.println("Tidak ada 'K'. Tidak bisa dicari solusinya (Uniform Cost Search).");
            JOptionPane.showMessageDialog(this, "Tidak ada 'K' (tujuan). Tidak bisa dicari solusinya (Uniform Cost Search).", "Error (Uniform Cost Search)", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        PriorityQueue<PuzzleState> openSet = new PriorityQueue<>(Comparator.comparingInt(state -> state.gCost));
        Map<String, PuzzleState> openSetMap = new HashMap<>(); 
        Set<String> closedSet = new HashSet<>();

        char[][] initialBoard = deepCopyBoard(this.gridState, this.gridRows, this.gridColumns);
        int initialHCost = 0; 
        PuzzleState initialState = new PuzzleState(initialBoard, 0, initialHCost, null, "START", this.gridRows, this.gridColumns);

        openSet.add(initialState);
        openSetMap.put(initialState.getBoardString(), initialState);

        int iterations = 0;
        final int MAX_ITERATIONS = 1000000; 

        while (!openSet.isEmpty()) {
            iterations++;
            if (iterations > MAX_ITERATIONS) {
                long execTimeSoFar = System.currentTimeMillis() - startTimeMillis;
                System.out.println("Batas iterasi UCS (" + MAX_ITERATIONS + ") tercapai.");
                JOptionPane.showMessageDialog(this,
                        "Batas iterasi UCS (" + MAX_ITERATIONS + ") tercapai.\nWaktu eksekusi: " + execTimeSoFar + " ms",
                        "Perhatian (UCS)", JOptionPane.WARNING_MESSAGE);
                return null;
            }

            PuzzleState currentState = openSet.poll();
            String currentBoardStr = currentState.getBoardString();
            openSetMap.remove(currentBoardStr); 

            if (closedSet.contains(currentBoardStr)) {
                continue;
            }
            closedSet.add(currentBoardStr);

            if (isGoalState(currentState.board, currentState.boardRows, currentState.boardCols, this.kLocation)) {
                System.out.println("Solusi UCS ketemu dalam " + iterations + " iterasi! Path length (gCost): " + currentState.gCost);
                return reconstructPath(currentState);
            }

            List<PuzzleState> successors = getSuccessors(currentState, this.kLocation); 
            for (PuzzleState successor : successors) {
                String successorBoardStr = successor.getBoardString();
                if (closedSet.contains(successorBoardStr)) {
                    continue;
                }

                PuzzleState existingInOpen = openSetMap.get(successorBoardStr);
                if (existingInOpen == null || successor.gCost < existingInOpen.gCost) {
                    if (existingInOpen != null) {
                        openSet.remove(existingInOpen); 
                    }
                    openSet.add(successor);
                    openSetMap.put(successorBoardStr, successor);
                }
            }
        }
        long executionTime = System.currentTimeMillis() - startTimeMillis;
        System.out.println("Tidak ada solusi UCS yang ketemu setelah " + iterations + " iterasi.");
        JOptionPane.showMessageDialog(this,
                "Tidak ada solusi UCS yang ketemu setelah " + iterations + " iterasi.\nWaktu eksekusi: " + executionTime + " ms",
                "Hasil (UCS)", JOptionPane.INFORMATION_MESSAGE);
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GUI app = new GUI();
            app.setVisible(true);
        });
    }
}