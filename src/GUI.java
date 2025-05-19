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
                animationTimer.stop(); // Stop animasi jika file baru di-load
            }
            loadFile();
        });
        aStarButton.addActionListener(e -> {
            if (gridState == null || kLocation == null) {
                JOptionPane.showMessageDialog(this, "Please load a valid puzzle file with a 'K' goal first.", "A* Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop(); // Stop animasi sebelumnya kalau A* dijalankan lagi
            }

            System.out.println("A* Solver Started...");
            aStarButton.setEnabled(false);
            loadButton.setEnabled(false);
            greedyBestButton.setEnabled(false);
            ucsButton.setEnabled(false);

            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                List<String> solutionPath = solveAStar();
                long endTime = System.currentTimeMillis();

                SwingUtilities.invokeLater(() -> {
                    System.out.println("A* Solver finished in " + (endTime - startTime) + " ms.");
                    if (solutionPath != null && !solutionPath.isEmpty()) {
                        System.out.println("Solution Path (" + solutionPath.size() + " moves):");
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
        fileChooser.setCurrentDirectory(new File("d:\\Programs\\tucil3\\test"));
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
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() > maxLength) {
                    maxLength = line.length();
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file for max line length: " + e.getMessage());
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
            System.err.println("Error reading file for line count: " + e.getMessage());
            return 0;
        }
        lineCount -= 2;
        return lineCount;
    }

    private void parseAndRenderFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // hitung ukuran grid dari file
            int gridRows = getLineCount(file.getPath());
            int gridColumns = getMaxLength(file.getPath());

            // Baca baris pertama
            String[] dimensions = reader.readLine().trim().split("\\s+");
            int rows = Integer.parseInt(dimensions[0]);
            int columns = Integer.parseInt(dimensions[1]);
            
            // Baca baris kedua untuk jumlah piece selain P
            String piecesLine = reader.readLine().trim();
            int numPieces = Integer.parseInt(piecesLine);

            char[][] grid = new char[gridRows][gridColumns];
            this.kLocation = null;
            this.gridState = grid;
            this.gridRows = gridRows;
            this.gridColumns = gridColumns;

            for (int i = 0; i < gridRows; i++) {
                String line = reader.readLine();
                if (line == null) break;
                for (int j = 0; j < gridColumns; j++) {
                    // fill from file or use '.' if beyond line-length
                    if (j < line.length()) {
                        char c = line.charAt(j);
                        if (c == 'K') {
                            // Cari nilai K
                            if (this.kLocation == null) {
                                this.kLocation = new Point(i, j);
                                grid[i][j] = '.';
                            } else {
                                System.out.println("Ada lebih dari satu 'K'. Pastikan hanya ada satu.");
                                System.exit(0);
                            }
                        } else {
                            grid[i][j] = c;
                        }
                    } else {
                        grid[i][j] = ' ';
                    }
                }
            }
            this.gridState = grid;

            List<BlockGroup> pieces = findBlockGroups(grid,gridRows,gridColumns);
            for(BlockGroup b:pieces){
                String orientation;
                if (b.isHorizontal()) orientation = "horizontal";
                else orientation = "vertical";
                System.out.println(
                    "piece '"+b.id+"' is "+orientation+
                    " at "+b.cells);
            }

            greedyBestButton.setEnabled(true);
            ucsButton.setEnabled(true);
            aStarButton.setEnabled(true);

            renderGrid(gridState, gridRows, gridColumns);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Error reading file: " + e.getMessage(),
                "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void moveUp(BlockGroup g) {
        if (g.isHorizontal()) {
            System.out.println("Tidak bisa ke up: piece horizontal");
            return;
        }
        // cari baris paling atas dari group
        int minR = g.cells.stream().mapToInt(p->p.x).min().getAsInt();
        int col  = g.cells.get(0).y;
        if (minR == 0 || gridState[minR-1][col] != '.') {
            System.out.println("Tidak bisa ke up: tabrakan dengan piece lain atau di sisi atas");
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

    public void moveDown(BlockGroup g) {
        if (g.isHorizontal()) {
            System.out.println("Tidak bisa ke down: piece horizontal");
            return;
        }
        int maxR = g.cells.stream().mapToInt(p->p.x).max().getAsInt();
        int col  = g.cells.get(0).y;
        if (maxR == gridRows-1 || gridState[maxR+1][col] != '.') {
            System.out.println("Tidak bisa ke down: tabrakan dengan piece lain atau di sisi bawah");
            return;
        }
        for (Point p : g.cells) gridState[p.x][p.y] = '.';
        for (Point p : g.cells) {
            p.x++;
            gridState[p.x][p.y] = g.id;
        }
        renderGrid(gridState, gridRows, gridColumns);
    }

    public void moveLeft(BlockGroup g) {
        if (!g.isHorizontal()) {
            System.out.println("Tidak bisa ke left: piece vertikal");
            return;
        }
        int row = g.cells.get(0).x;
        int minC = g.cells.stream().mapToInt(p->p.y).min().getAsInt();
        if (minC == 0 || gridState[row][minC-1] != '.') {
            System.out.println("Tidak bisa ke left: tabrakan dengan piece lain atau di sisi kiri");
            return;
        }
        for (Point p : g.cells) gridState[p.x][p.y] = '.';
        for (Point p : g.cells) {
            p.y--;
            gridState[p.x][p.y] = g.id;
        }
        renderGrid(gridState, gridRows, gridColumns);
    }

    public void moveRight(BlockGroup g) {
        if (!g.isHorizontal()) {
            System.out.println("Tidak bisa ke right: piece vertikal");
            return;
        }
        int row = g.cells.get(0).x;
        int maxC = g.cells.stream().mapToInt(p->p.y).max().getAsInt();
        if (maxC == gridColumns-1 || gridState[row][maxC+1] != '.') {
            System.out.println("Tidak bisa ke right: tabrakan dengan piece lain atau di sisi kanan");
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

    /** one connected piece of identical chars */
    private static class BlockGroup {
        final char id;
        final List<Point> cells = new ArrayList<>();
        BlockGroup(char id) { this.id = id; }

        /** 
         * @return true if this piece occupies a single row and spans at least two columns
         *         false otherwise (including vertical pieces or single‐tile pieces)
         */
        public boolean isHorizontal() {
            if (cells.size() < 2) return false;
            // int row0 = cells.get(0).x;
            // int col0 = cells.get(0).y;

            // check if horizontal
            if ((cells.get(1).x == cells.get(0).x) && (cells.get(1).y != cells.get(0).y)) {
                return true;
            }
            // else, check if vertical
            else if ((cells.get(1).x != cells.get(0).x) && (cells.get(1).y == cells.get(0).y)) {
                return false;
            } 
            // else, if neither
            else {
                System.out.println("An abnormal piece detected");
                System.exit(0);
            }

            // for (Point p : cells) {
            //     if (p.x != row0) return false;
            // }
            return false;
        }
    }

    /** scan the grid and return each connected piece as a BlockGroup */
    private List<BlockGroup> findBlockGroups(char[][] grid, int rows, int cols) {
        boolean[][] used = new boolean[rows][cols];
        List<BlockGroup> groups = new ArrayList<>();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                char c = grid[i][j];
                if (c==' '||c=='.'|| used[i][j]) continue;

                BlockGroup g = new BlockGroup(c);
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

    // Heuristic: Manhattan distance from closest cell of 'P' to K's location
    private int calculateHeuristic(char[][] currentBoard, int r, int c, Point kGoalLocation) {
        if (kGoalLocation == null) return Integer.MAX_VALUE; // Should not happen if K was found

        BlockGroup pBlock = null;
        List<BlockGroup> groups = findBlockGroups(currentBoard, r, c);
        for (BlockGroup group : groups) {
            if (group.id == 'P') {
                pBlock = group;
                break;
            }
        }

        if (pBlock == null || pBlock.cells.isEmpty()) {
            return Integer.MAX_VALUE; // 'P' not found
        }

        int minManhattanDistance = Integer.MAX_VALUE;
        for (Point pCell : pBlock.cells) {
            int dist = Math.abs(pCell.x - kGoalLocation.x) + Math.abs(pCell.y - kGoalLocation.y);
            if (dist < minManhattanDistance) {
                minManhattanDistance = dist;
            }
        }
        return minManhattanDistance;
    }

    private boolean isGoalState(char[][] currentBoard, int r, int c, Point kGoalLocation) {
        if (kGoalLocation == null) return false;

        BlockGroup pBlock = null;
        List<BlockGroup> groups = findBlockGroups(currentBoard, r, c);
        for (BlockGroup group : groups) {
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

        List<BlockGroup> blockGroups = findBlockGroups(currentBoard, r, c);

        for (BlockGroup group : blockGroups) {
            if (group.cells.isEmpty()) continue;

            // Coba ke atas
            if (!group.isHorizontal() || group.cells.size() == 1) { // Vertical or single blocks can try to move vertically
                char[][] nextBoardUp = tryMoveGroup(currentBoard, group, r, c, -1, 0);
                if (nextBoardUp != null) {
                    int h = calculateHeuristic(nextBoardUp, r, c, kGoalLocation);
                    successors.add(new PuzzleState(nextBoardUp, currentState.gCost + 1, h, currentState, group.id + "_UP", r, c));
                }
            }
            // Coba ke bawah
            if (!group.isHorizontal() || group.cells.size() == 1) {
                char[][] nextBoardDown = tryMoveGroup(currentBoard, group, r, c, 1, 0);
                if (nextBoardDown != null) {
                    int h = calculateHeuristic(nextBoardDown, r, c, kGoalLocation);
                    successors.add(new PuzzleState(nextBoardDown, currentState.gCost + 1, h, currentState, group.id + "_DOWN", r, c));
                }
            }
            // Coba ke kiri
            if (group.isHorizontal() || group.cells.size() == 1) { // Horizontal or single blocks can try to move horizontally
                char[][] nextBoardLeft = tryMoveGroup(currentBoard, group, r, c, 0, -1);
                if (nextBoardLeft != null) {
                    int h = calculateHeuristic(nextBoardLeft, r, c, kGoalLocation);
                    successors.add(new PuzzleState(nextBoardLeft, currentState.gCost + 1, h, currentState, group.id + "_LEFT", r, c));
                }
            }
            // Coba ke kanan
            if (group.isHorizontal() || group.cells.size() == 1) {
                char[][] nextBoardRight = tryMoveGroup(currentBoard, group, r, c, 0, 1);
                if (nextBoardRight != null) {
                    int h = calculateHeuristic(nextBoardRight, r, c, kGoalLocation);
                    successors.add(new PuzzleState(nextBoardRight, currentState.gCost + 1, h, currentState, group.id + "_RIGHT", r, c));
                }
            }
        }
        return successors;
    }

    // Helper untuk mencoba pergerakan dan return keadaan board baru, atau null kalau tdk valid
    private char[][] tryMoveGroup(char[][] board, BlockGroup group, int rows, int cols, int dRow, int dCol) {
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
        animationTimer.start();
    }

    private void performNextAnimationStep() {
        if (animationMoveIndex < currentSolutionPathForAnimation.size()) {
            String move = currentSolutionPathForAnimation.get(animationMoveIndex);
            char pieceId = move.charAt(0);
            String direction = move.substring(move.indexOf('_') + 1);

            // We need to find the BlockGroup in the current gridState
            List<BlockGroup> currentGroups = findBlockGroups(this.gridState, this.gridRows, this.gridColumns);
            BlockGroup targetGroup = null;
            for (BlockGroup bg : currentGroups) {
                if (bg.id == pieceId) {
                    targetGroup = bg;
                    break;
                }
            }

            if (targetGroup != null) {
                System.out.println("Animasi pergerakan: " + move);
                if ("UP".equals(direction)) { moveUp(targetGroup); }
                else if ("DOWN".equals(direction)) { moveDown(targetGroup); }
                else if ("LEFT".equals(direction)) { moveLeft(targetGroup); }
                else if ("RIGHT".equals(direction)) { moveRight(targetGroup); }
            } else {
                System.err.println("Error pada animasi: Tidak ada piece '" + pieceId + "' untuk pergerakan " + move);
                // Stop animasi/skip?
            }
            animationMoveIndex++;
        } else {
            animationTimer.stop();
            System.out.println("Animation finished.");
            JOptionPane.showMessageDialog(this,
                    "Solusi ditemukan!\n" + currentSolutionPathForAnimation.size() + " pergerakan.",
                    "Solusi A* sudah dianimasikan", JOptionPane.INFORMATION_MESSAGE);
            aStarButton.setEnabled(true);
            loadButton.setEnabled(true);
            greedyBestButton.setEnabled(gridState != null);
            ucsButton.setEnabled(gridState != null);
        }
    }

    public List<String> solveAStar() {
        if (this.gridState == null) {
            System.out.println("Tidak ada board yang di-load.");
            return null;
        }
        if (this.kLocation == null) {
            System.out.println("Tidak ada 'K'. Tidak bisa dicari solusinya");
            JOptionPane.showMessageDialog(this, "Tidak ada 'K' (tujuan). Tidak bisa dicari solusinya.", "Error (A*)", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        PriorityQueue<PuzzleState> openSet = new PriorityQueue<>(Comparator.comparingInt(PuzzleState::getFCost));
        Set<String> closedSet = new HashSet<>();

        char[][] initialBoard = deepCopyBoard(this.gridState, this.gridRows, this.gridColumns);
        int initialHCost = calculateHeuristic(initialBoard, this.gridRows, this.gridColumns, this.kLocation);
        PuzzleState initialState = new PuzzleState(initialBoard, 0, initialHCost, null, "START", this.gridRows, this.gridColumns);

        openSet.add(initialState);

        int iterations = 0;
        final int MAX_ITERATIONS = 500000; // Jika terlalu lama

        while (!openSet.isEmpty()) {
            iterations++;
            if (iterations > MAX_ITERATIONS) {
                System.out.println("Batas iterasi A* (" + MAX_ITERATIONS + ") tercapai.");
                JOptionPane.showMessageDialog(this, "Batas iterasi A* (" + MAX_ITERATIONS + ") tercapai.", "Perhatian (A*)", JOptionPane.WARNING_MESSAGE);
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
        System.out.println("Tidak ada solusi A* yang ketemu setelah " + iterations + " iterasi.");
        JOptionPane.showMessageDialog(this, "Tidak ada solusi A* yang ketemu setelah " + iterations + " iterasi.", "Hasil (A*)", JOptionPane.INFORMATION_MESSAGE);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GUI app = new GUI();
            app.setVisible(true);
        });
    }
}