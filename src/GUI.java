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

public class GUI extends JFrame {
    private JPanel gridPanel;
    private JButton loadButton;
    private final Map<Character, Color> colorMap = new HashMap<>();
    private final Random random = new Random();
    private JButton greedyBestButton, ucsButton, aStarButton;

    public GUI() {
        setTitle("Block Grid Renderer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        gridPanel = new JPanel();

        // Create components
        loadButton = new JButton("Load File");

        greedyBestButton = new JButton("Greedy Best First Search");
        greedyBestButton.setEnabled(false);
        ucsButton = new JButton("Uniform Cost Search");
        ucsButton.setEnabled(false);
        aStarButton = new JButton("A*");
        aStarButton.setEnabled(false);

        
        // Add action listener to the button
        loadButton.addActionListener(e -> loadFile());
        
        JPanel methodRow = new JPanel(new GridLayout(1,3,5,0));
        methodRow.add(greedyBestButton);
        methodRow.add(ucsButton);
        methodRow.add(aStarButton);
        
        JPanel topPanel = new JPanel(new BorderLayout(0,5));
        // topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(loadButton, BorderLayout.NORTH);
        topPanel.add(methodRow, BorderLayout.SOUTH);
        
        // Add components to the frame
        add(topPanel, BorderLayout.NORTH);
        add(gridPanel, BorderLayout.CENTER);

        // Set initial size
        setSize(600, 600);
        setLocationRelativeTo(null); // Center on screen
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
            // Optionally, rethrow or handle more gracefully
            return 0; // Or throw an exception
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
            // Optionally, rethrow or handle more gracefully
            return 0; // Or throw an exception
        }
        lineCount -= 2;
        return lineCount;
    }

    private void parseAndRenderFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // determine grid size from file
            int gridRows = getLineCount(file.getPath());
            int gridColumns = getMaxLength(file.getPath());

            // Read first line for dimensions
            String[] dimensions = reader.readLine().trim().split("\\s+");
            int rows = Integer.parseInt(dimensions[0]);
            int columns = Integer.parseInt(dimensions[1]);
            
            // Read second line for number of pieces (not used yet but stored)
            String piecesLine = reader.readLine().trim();
            int numPieces = Integer.parseInt(piecesLine);

            char[][] grid = new char[gridRows][gridColumns];
            for (int i = 0; i < gridRows; i++) {
                String line = reader.readLine();
                if (line == null) break;
                for (int j = 0; j < gridColumns; j++) {
                    // fill from file or use '.' if beyond line-length
                    grid[i][j] = (j < line.length() ? line.charAt(j) : ' ');
                }
            }

            List<BlockGroup> pieces = findBlockGroups(grid,gridRows,gridColumns);
            for(BlockGroup b:pieces){
            System.out.println(
                "piece '"+b.id+"' is "+
                (b.isHorizontal()?"horizontal":"vertikal")+
                " at "+b.cells);
            }

            greedyBestButton.setEnabled(true);
            ucsButton.setEnabled(true);
            aStarButton.setEnabled(true);

            renderGrid(grid, gridRows, gridColumns);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Error reading file: " + e.getMessage(),
                "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renderGrid(char[][] grid, int rows, int columns) {
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GUI app = new GUI();
            app.setVisible(true);
        });
    }
}