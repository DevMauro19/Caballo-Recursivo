package Caballos;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class KnightTourVisualizer extends JFrame {

    // ── Constantes del algoritmo ──────────────────────────────────────────────
    static final int N = 8;
    static final int[] desplazamientoX = {2, 1, -1, -2, -2, -1, 1, 2};
    static final int[] desplazamientoY = {1, 2, 2, 1, -1, -2, -2, -1};
    static volatile boolean exito = false;

    // ── Colores ───────────────────────────────────────────────────────────────
    static final Color BG_DARK         = new Color(22, 23, 30);
    static final Color BG_PANEL        = new Color(30, 31, 40);
    static final Color CELL_LIGHT      = new Color(240, 217, 181);
    static final Color CELL_DARK       = new Color(181, 136, 99);
    static final Color CELL_VISITED_L  = new Color(155, 210, 130);
    static final Color CELL_VISITED_D  = new Color(100, 165, 80);
    static final Color CELL_CURRENT    = new Color(255, 220, 50);
    static final Color CELL_START      = new Color(235, 85, 60);
    static final Color CELL_SELECTED   = new Color(80, 160, 240);
    static final Color TEXT_MAIN       = new Color(235, 235, 240);
    static final Color TEXT_DIM        = new Color(150, 150, 165);
    static final Color TEXT_ACCENT     = new Color(100, 200, 120);
    static final Color PATH_START      = new Color(100, 200, 80, 200);
    static final Color PATH_END        = new Color(70, 130, 220, 200);
    static final Color BORDER_COLOR    = new Color(65, 60, 50);
    static final Color BTN_RESOLVE     = new Color(55, 120, 70);
    static final Color BTN_STOP        = new Color(180, 55, 50);
    static final Color BTN_RESET       = new Color(95, 65, 55);
    static final Color BTN_NAV         = new Color(60, 65, 85);
    static final Color BTN_PLAY        = new Color(50, 90, 140);
    static final Color BTN_BOARD       = new Color(65, 75, 110);

    // ── Estado ────────────────────────────────────────────────────────────────
    int[][] tablero    = new int[N][N];
    int[]   secuenciaX = new int[N * N];
    int[]   secuenciaY = new int[N * N];
    int     pasoActual = 0;
    int     totalPasos = 0;
    boolean resuelto   = false;
    int     inicioX    = 0;
    int     inicioY    = 0;

    // ── Control de cancelación ────────────────────────────────────────────────
    volatile boolean cancelado = false;
    SwingWorker<Void, Void> workerActual;

    // ── Componentes UI ────────────────────────────────────────────────────────
    BoardPanel  boardPanel;
    JLabel      lblStatus;
    JLabel      lblStep;
    JLabel      lblTime;
    JButton     btnResolver;
    JButton     btnDetener;
    JButton     btnPrev;
    JButton     btnNext;
    JButton     btnPlay;
    JButton     btnReset;
    JButton     btnBoardPick;
    JSpinner    spnRow;
    JSpinner    spnCol;
    JCheckBox   chkRandom;
    JSlider     sldSpeed;
    Timer       animTimer;
    boolean     playing = false;
    boolean     esperandoClickTablero = false;
    long        tiempoInicio;

    // ── Constructor ───────────────────────────────────────────────────────────
    public KnightTourVisualizer() {
        super("♞  Knight's Tour — Visualizador del Recorrido del Caballo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(BG_DARK);
        construirUI();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN DE LA INTERFAZ
    // ═══════════════════════════════════════════════════════════════════════════
    private void construirUI() {
        setLayout(new BorderLayout(14, 14));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

        // ── Cabecera ──────────────────────────────────────────────────────────
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_DARK);
        JLabel title = new JLabel("♞  Knight's Tour — Recorrido del Caballo", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(255, 210, 90));
        headerPanel.add(title, BorderLayout.CENTER);

        JLabel subtitle = new JLabel("Algoritmo de Backtracking — 8×8", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_DIM);
        headerPanel.add(subtitle, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // ── Tablero central ───────────────────────────────────────────────────
        boardPanel = new BoardPanel();
        boardPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!esperandoClickTablero) return;
                int cs = BoardPanel.CELL;
                int col = (e.getX() - BoardPanel.MARGIN) / cs;
                int row = (e.getY() - BoardPanel.MARGIN) / cs;
                if (row >= 0 && row < N && col >= 0 && col < N) {
                    spnRow.setValue(row);
                    spnCol.setValue(col);
                    esperandoClickTablero = false;
                    boardPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    lblStatus.setText("Posicion seleccionada: (" + row + ", " + col + "). Presiona Resolver.");
                    boardPanel.highlightCell = new Point(col, row);
                    boardPanel.repaint();
                }
            }
        });
        add(boardPanel, BorderLayout.CENTER);

        // ── Panel de control (derecha) ────────────────────────────────────────
        JPanel ctrl = new JPanel();
        ctrl.setLayout(new BoxLayout(ctrl, BoxLayout.Y_AXIS));
        ctrl.setBackground(BG_PANEL);
        ctrl.setBorder(new CompoundBorder(
                new LineBorder(new Color(60, 60, 75), 1, true),
                new EmptyBorder(16, 14, 16, 14)));
        ctrl.setPreferredSize(new Dimension(230, 540));

        // Sección: Posición de inicio
        ctrl.add(sectionLabel("INICIO"));
        ctrl.add(Box.createVerticalStrut(8));

        chkRandom = new JCheckBox("Aleatorio");
        styleCheck(chkRandom);
        chkRandom.addActionListener(e -> {
            spnRow.setEnabled(!chkRandom.isSelected());
            spnCol.setEnabled(!chkRandom.isSelected());
            btnBoardPick.setEnabled(!chkRandom.isSelected());
        });
        ctrl.add(chkRandom);
        ctrl.add(Box.createVerticalStrut(8));

        JPanel coordPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        coordPanel.setBackground(BG_PANEL);

        JLabel lr = new JLabel("Fila (0-7):");
        lr.setForeground(TEXT_MAIN); lr.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JLabel lc = new JLabel("Columna (0-7):");
        lc.setForeground(TEXT_MAIN); lc.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        spnRow = makeSpinner(0, 7, 0);
        spnCol = makeSpinner(0, 7, 0);

        coordPanel.add(lr); coordPanel.add(spnRow);
        coordPanel.add(lc); coordPanel.add(spnCol);
        ctrl.add(coordPanel);

        ctrl.add(Box.createVerticalStrut(6));
        btnBoardPick = makeButton("Seleccionar en tablero", BTN_BOARD);
        btnBoardPick.addActionListener(e -> {
            esperandoClickTablero = true;
            boardPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            lblStatus.setText("Haz clic en una casilla del tablero...");
        });
        ctrl.add(btnBoardPick);

        ctrl.add(Box.createVerticalStrut(14));
        ctrl.add(separator());
        ctrl.add(Box.createVerticalStrut(12));

        // Sección: Algoritmo
        ctrl.add(sectionLabel("ALGORITMO"));
        ctrl.add(Box.createVerticalStrut(8));

        JPanel rowBtns = new JPanel(new GridLayout(1, 2, 6, 0));
        rowBtns.setBackground(BG_PANEL);
        btnResolver = makeButton("Resolver", BTN_RESOLVE);
        btnDetener  = makeButton("Detener", BTN_STOP);
        btnDetener.setEnabled(false);
        btnResolver.addActionListener(e -> resolver());
        btnDetener.addActionListener(e -> detenerCalculo());
        rowBtns.add(btnResolver);
        rowBtns.add(btnDetener);
        ctrl.add(rowBtns);

        ctrl.add(Box.createVerticalStrut(6));
        btnReset = makeButton("Reiniciar", BTN_RESET);
        btnReset.addActionListener(e -> reiniciar());
        ctrl.add(btnReset);

        ctrl.add(Box.createVerticalStrut(14));
        ctrl.add(separator());
        ctrl.add(Box.createVerticalStrut(12));

        // Sección: Navegación
        ctrl.add(sectionLabel("NAVEGACION"));
        ctrl.add(Box.createVerticalStrut(8));

        JPanel navRow = new JPanel(new GridLayout(1, 3, 6, 0));
        navRow.setBackground(BG_PANEL);
        btnPrev = makeButton("<< Anterior", BTN_NAV);
        btnNext = makeButton("Siguiente >>", BTN_NAV);
        btnPlay = makeButton("Reproducir", BTN_PLAY);
        btnPrev.addActionListener(e -> retroceder());
        btnNext.addActionListener(e -> avanzar());
        btnPlay.addActionListener(e -> togglePlay());
        navRow.add(btnPrev); navRow.add(btnPlay); navRow.add(btnNext);
        ctrl.add(navRow);

        ctrl.add(Box.createVerticalStrut(10));
        JLabel lblSpeed = new JLabel("Velocidad:");
        lblSpeed.setForeground(TEXT_DIM);
        lblSpeed.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSpeed.setAlignmentX(Component.LEFT_ALIGNMENT);
        ctrl.add(lblSpeed);

        sldSpeed = new JSlider(50, 800, 300);
        sldSpeed.setBackground(BG_PANEL);
        sldSpeed.setForeground(TEXT_MAIN);
        sldSpeed.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        sldSpeed.setInverted(true);
        ctrl.add(sldSpeed);

        ctrl.add(Box.createVerticalStrut(14));
        ctrl.add(separator());
        ctrl.add(Box.createVerticalStrut(10));

        lblStep = new JLabel("Paso: — / —");
        lblStep.setForeground(TEXT_ACCENT);
        lblStep.setFont(new Font("Segoe UI Mono", Font.BOLD, 14));
        lblStep.setAlignmentX(Component.LEFT_ALIGNMENT);
        ctrl.add(lblStep);

        ctrl.add(Box.createVerticalStrut(4));
        lblTime = new JLabel("");
        lblTime.setForeground(TEXT_DIM);
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTime.setAlignmentX(Component.LEFT_ALIGNMENT);
        ctrl.add(lblTime);

        ctrl.add(Box.createVerticalGlue());
        add(ctrl, BorderLayout.EAST);

        // ── Barra de estado ───────────────────────────────────────────────────
        lblStatus = new JLabel("Selecciona una posicion de inicio y presiona Resolver.");
        lblStatus.setForeground(TEXT_DIM);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setBorder(new EmptyBorder(8, 0, 0, 0));
        add(lblStatus, BorderLayout.SOUTH);

        // ── Timer de animación ────────────────────────────────────────────────
        animTimer = new Timer(300, e -> {
            if (pasoActual < totalPasos) {
                avanzar();
            } else {
                detenerPlay();
            }
        });

        actualizarBotones();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CLASE ESTADO
    // ═══════════════════════════════════════════════════════════════════════════
    static class Estado {
        int[][] tablero;
        int paso;
        Estado(int[][] tablero, int paso) {
            this.tablero = tablero;
            this.paso    = paso;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Metodo resolverDesde
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Método con el que se busca encontrar el camino que lleva a la solución.
     */
    static void resolverDesde(int x, int y, Estado estado) {

        // Caso base: el caballo ya recorrió todas las casillas
        if (estado.paso == N * N - 1) {
            exito = true;
        }

        int i = 0;

        while (i < 8 && !exito) {
            int nx = x + desplazamientoX[i]; // hacia donde nos movemos en x
            int ny = y + desplazamientoY[i]; // hacia donde nos movemos en y

            if (esValido(nx, ny, estado.tablero)) {
                estado.tablero[nx][ny] = estado.paso + 1; // si es válido nos movemos y sumamos 1 al paso
                estado.paso++;

                resolverDesde(nx, ny, estado);

                if (!exito) {
                    // backtrack: el movimiento no llevó a solución
                    estado.tablero[nx][ny] = -1;
                    estado.paso--;
                } else {
                    exito = true; // solución encontrada, no retroceder
                }
            }

            i++;
        }
    }

    static boolean esValido(int x, int y, int[][] tablero) {
        return x >= 0 && y >= 0 && x < N && y < N && tablero[x][y] == -1;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  RESOLVER (llamado desde el botón)
    // ═══════════════════════════════════════════════════════════════════════════
    private void resolver() {
        detenerPlay();
        cancelado = false;
        resuelto  = false;
        exito     = false; // ← reiniciar bandera global antes de cada búsqueda

        int fila = chkRandom.isSelected() ? new Random().nextInt(N) : (int) spnRow.getValue();
        int col  = chkRandom.isSelected() ? new Random().nextInt(N) : (int) spnCol.getValue();

        inicioX = fila;
        inicioY = col;
        spnRow.setValue(fila);
        spnCol.setValue(col);

        for (int[] r : tablero) java.util.Arrays.fill(r, -1);
        tablero[fila][col] = 0;

        lblStatus.setText("Calculando desde (" + fila + ", " + col + ")...");
        lblTime.setText("");
        tiempoInicio = System.currentTimeMillis();
        btnResolver.setEnabled(false);
        btnDetener.setEnabled(true);
        boardPanel.highlightCell = null;
        boardPanel.repaint();

        // Capturamos la referencia local para pasarla al worker (evita capturar 'this' en lambda de forma ambigua)
        int[][] tableroLocal = tablero;

        workerActual = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                Estado ei = new Estado(tableroLocal, 0);
                resolverDesde(fila, col, ei);
                return null;
            }

            @Override
            protected void done() {
                long elapsed = System.currentTimeMillis() - tiempoInicio;

                if (cancelado) {
                    lblStatus.setText("Calculo cancelado por el usuario.");
                    lblTime.setText("");
                } else if (exito) {
                    // Reconstruir la secuencia de posiciones a partir del tablero resuelto
                    int[][] pos = new int[N * N][2];
                    for (int r = 0; r < N; r++)
                        for (int c = 0; c < N; c++)
                            if (tableroLocal[r][c] >= 0)
                                pos[tableroLocal[r][c]] = new int[]{r, c};

                    for (int k = 0; k < N * N; k++) {
                        secuenciaX[k] = pos[k][0];
                        secuenciaY[k] = pos[k][1];
                    }
                    totalPasos = N * N - 1;
                    pasoActual = 0;
                    resuelto   = true;
                    lblStatus.setText("Solucion encontrada en "
                            + String.format("%.2f", elapsed / 1000.0)
                            + " s. Usa los controles para navegar.");
                    lblTime.setText("Tiempo: " + String.format("%.2f", elapsed / 1000.0) + " s");
                    boardPanel.resetView();
                    boardPanel.showStep(0);
                } else {
                    lblStatus.setText("No se encontro solucion desde (" + fila + ", " + col + ").");
                    lblTime.setText("Tiempo: " + String.format("%.2f", elapsed / 1000.0) + " s");
                }

                btnResolver.setEnabled(true);
                btnDetener.setEnabled(false);
                actualizarBotones();
                boardPanel.repaint();
            }
        };
        workerActual.execute();
    }

    private void detenerCalculo() {
        cancelado = true;
        exito     = true; // forzar que el while termine inmediatamente
        if (workerActual != null && !workerActual.isDone()) {
            workerActual.cancel(true);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  NAVEGACIÓN
    // ═══════════════════════════════════════════════════════════════════════════
    private void avanzar() {
        if (!resuelto || pasoActual >= totalPasos) return;
        pasoActual++;
        boardPanel.showStep(pasoActual);
        actualizarBotones();
    }

    private void retroceder() {
        if (!resuelto || pasoActual <= 0) return;
        pasoActual--;
        boardPanel.showStep(pasoActual);
        actualizarBotones();
    }

    private void togglePlay() {
        if (playing) {
            detenerPlay();
        } else {
            if (!resuelto) return;
            if (pasoActual >= totalPasos) {
                pasoActual = 0;
                boardPanel.resetView();
                boardPanel.showStep(0);
            }
            playing = true;
            animTimer.setDelay(sldSpeed.getValue());
            animTimer.start();
            btnPlay.setText("Pausa");
        }
    }

    private void detenerPlay() {
        playing = false;
        animTimer.stop();
        btnPlay.setText("Reproducir");
    }

    private void reiniciar() {
        detenerPlay();
        detenerCalculo();
        resuelto   = false;
        pasoActual = 0;
        totalPasos = 0;
        cancelado  = false;
        exito      = false;
        for (int[] r : tablero) java.util.Arrays.fill(r, -1);
        boardPanel.resetView();
        boardPanel.highlightCell = null;
        boardPanel.repaint();
        lblStatus.setText("Selecciona una posicion de inicio y presiona Resolver.");
        lblStep.setText("Paso: — / —");
        lblTime.setText("");
        btnResolver.setEnabled(true);
        btnDetener.setEnabled(false);
        actualizarBotones();
    }

    private void actualizarBotones() {
        boolean ok = resuelto;
        btnPrev.setEnabled(ok && pasoActual > 0);
        btnNext.setEnabled(ok && pasoActual < totalPasos);
        btnPlay.setEnabled(ok);
        lblStep.setText(ok ? "Paso: " + pasoActual + " / " + totalPasos : "Paso: — / —");
        animTimer.setDelay(sldSpeed.getValue());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  HELPERS UI
    // ═══════════════════════════════════════════════════════════════════════════
    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(130, 130, 150));
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JSeparator separator() {
        JSeparator s = new JSeparator();
        s.setForeground(new Color(65, 65, 80));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    private JButton makeButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addMouseListener(new MouseAdapter() {
            Color orig = bg;
            public void mouseEntered(MouseEvent e) { b.setBackground(orig.brighter()); }
            public void mouseExited(MouseEvent e)  { b.setBackground(orig); }
        });
        return b;
    }

    private JSpinner makeSpinner(int min, int max, int val) {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(val, min, max, 1));
        sp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sp.setMaximumSize(new Dimension(75, 28));
        return sp;
    }

    private void styleCheck(JCheckBox cb) {
        cb.setBackground(BG_PANEL);
        cb.setForeground(TEXT_MAIN);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        cb.setFocusPainted(false);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PANEL DEL TABLERO
    // ═══════════════════════════════════════════════════════════════════════════
    class BoardPanel extends JPanel {
        static final int CELL   = 64;
        static final int MARGIN = 32;

        int   shownStep    = -1;
        Point highlightCell = null;

        BoardPanel() {
            int size = N * CELL + MARGIN + 4;
            setPreferredSize(new Dimension(size, size));
            setBackground(BG_DARK);
        }

        void showStep(int step) {
            shownStep = step;
            actualizarBotones();
            repaint();
        }

        void resetView() {
            shownStep = -1;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Etiquetas de filas y columnas
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.setColor(TEXT_DIM);
            for (int i = 0; i < N; i++) {
                String s = String.valueOf(i);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(s, MARGIN + i * CELL + CELL / 2 - fm.stringWidth(s) / 2, MARGIN - 8);
                g2.drawString(s, 8, MARGIN + i * CELL + CELL / 2 + fm.getAscent() / 3);
            }

            // Celdas
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    int x = MARGIN + c * CELL;
                    int y = MARGIN + r * CELL;
                    boolean light = (r + c) % 2 == 0;

                    boolean visited = false;
                    int stepNum = -1;
                    if (resuelto && shownStep >= 0) {
                        stepNum = tablero[r][c];
                        visited = stepNum >= 0 && stepNum <= shownStep;
                    }

                    Color bg;
                    if (visited) {
                        bg = light ? CELL_VISITED_L : CELL_VISITED_D;
                    } else {
                        bg = light ? CELL_LIGHT : CELL_DARK;
                    }
                    g2.setColor(bg);
                    g2.fillRoundRect(x + 1, y + 1, CELL - 2, CELL - 2, 6, 6);

                    // Marca de inicio
                    if (resuelto && r == inicioX && c == inicioY) {
                        g2.setColor(CELL_START);
                        g2.fillOval(x + CELL - 22, y + 4, 18, 18);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                        g2.drawString("I", x + CELL - 17, y + 17);
                    }

                    // Celda actual (caballo)
                    boolean isCurrent = resuelto && shownStep >= 0
                            && secuenciaX[shownStep] == r
                            && secuenciaY[shownStep] == c;
                    if (isCurrent) {
                        g2.setColor(new Color(255, 220, 50, 180));
                        g2.fillRoundRect(x + 1, y + 1, CELL - 2, CELL - 2, 6, 6);
                    }

                    // Highlight selección manual
                    if (!resuelto && highlightCell != null && highlightCell.y == r && highlightCell.x == c) {
                        g2.setColor(new Color(80, 160, 240, 100));
                        g2.fillRoundRect(x + 1, y + 1, CELL - 2, CELL - 2, 6, 6);
                        g2.setColor(CELL_SELECTED);
                        g2.setStroke(new BasicStroke(2.5f));
                        g2.drawRoundRect(x + 2, y + 2, CELL - 5, CELL - 5, 5, 5);
                        g2.setStroke(new BasicStroke(1f));
                    }

                    // Número de paso
                    if (visited && stepNum >= 0) {
                        g2.setColor(isCurrent ? new Color(50, 50, 40) : new Color(25, 50, 25));
                        g2.setFont(new Font("Segoe UI Mono", Font.BOLD, stepNum < 10 ? 16 : 13));
                        String ns = String.valueOf(stepNum);
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(ns,
                                x + (CELL - fm.stringWidth(ns)) / 2,
                                y + (CELL + fm.getAscent()) / 2 - 2);
                    }
                }
            }

            // Líneas del recorrido
            if (resuelto && shownStep >= 1) {
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int p = 1; p <= shownStep; p++) {
                    int x1 = MARGIN + secuenciaY[p - 1] * CELL + CELL / 2;
                    int y1 = MARGIN + secuenciaX[p - 1] * CELL + CELL / 2;
                    int x2 = MARGIN + secuenciaY[p]     * CELL + CELL / 2;
                    int y2 = MARGIN + secuenciaX[p]     * CELL + CELL / 2;

                    float t = (float) p / totalPasos;
                    g2.setColor(interpColor(PATH_START, PATH_END, t));
                    g2.drawLine(x1, y1, x2, y2);
                }
                g2.setStroke(new BasicStroke(1f));
            }

            // Caballo en posición actual
            if (resuelto && shownStep >= 0) {
                int cr = secuenciaX[shownStep];
                int cc = secuenciaY[shownStep];
                int kx = MARGIN + cc * CELL;
                int ky = MARGIN + cr * CELL;
                int cx = kx + CELL / 2;
                int cy = ky + CELL / 2;

                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillOval(cx - 15, cy - 15, 30, 30);

                g2.setColor(new Color(60, 55, 45));
                g2.fillOval(cx - 13, cy - 13, 26, 26);
                g2.setColor(new Color(220, 200, 140));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(cx - 13, cy - 13, 26, 26);
                g2.setStroke(new BasicStroke(1f));

                g2.setColor(new Color(220, 200, 140));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm2 = g2.getFontMetrics();
                g2.drawString("C", cx - fm2.stringWidth("C") / 2, cy + fm2.getAscent() / 3);
            }

            // Borde del tablero
            g2.setColor(BORDER_COLOR);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(MARGIN, MARGIN, N * CELL, N * CELL, 8, 8);
            g2.setStroke(new BasicStroke(1f));

            // Leyenda
            int lx = MARGIN;
            int ly = MARGIN + N * CELL + 18;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));

            g2.setColor(TEXT_DIM);
            g2.drawString("Leyenda:", lx, ly);

            g2.setColor(CELL_START);
            g2.fillRoundRect(lx + 60, ly - 10, 12, 12, 3, 3);
            g2.setColor(TEXT_DIM);
            g2.drawString("Inicio", lx + 76, ly);

            g2.setColor(CELL_CURRENT);
            g2.fillRoundRect(lx + 120, ly - 10, 12, 12, 3, 3);
            g2.setColor(TEXT_DIM);
            g2.drawString("Actual", lx + 136, ly);

            g2.setColor(CELL_VISITED_L);
            g2.fillRoundRect(lx + 190, ly - 10, 12, 12, 3, 3);
            g2.setColor(TEXT_DIM);
            g2.drawString("Visitada", lx + 206, ly);

            g2.setColor(PATH_START);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(lx + 270, ly - 4, lx + 290, ly - 4);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(TEXT_DIM);
            g2.drawString("Recorrido", lx + 296, ly);
        }

        private Color interpColor(Color a, Color b, float t) {
            int r  = (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t);
            int gr = (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bl = (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t);
            int al = (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
            return new Color(r, gr, bl, al);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  MAIN
    // ═══════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(KnightTourVisualizer::new);
    }
}
