package Caballos;


import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CaballoSwing extends JFrame {

    // ── Constantes del algoritmo ──────────────────────────────────────────────
    static final int N = 8;
    static final int[] dx = {2, 1, -1, -2, -2, -1, 1, 2};
    static final int[] dy = {1, 2, 2, 1, -1, -2, -2, -1};

    // ── Colores del tablero ───────────────────────────────────────────────────
    static final Color COLOR_CASILLA_CLARA   = new Color(240, 217, 181);
    static final Color COLOR_CASILLA_OSCURA  = new Color(181, 136, 99);
    static final Color COLOR_VISITADA_CLARA  = new Color(186, 220, 158);
    static final Color COLOR_VISITADA_OSCURA = new Color(130, 175, 100);
    static final Color COLOR_ACTUAL          = new Color(255, 236, 80);
    static final Color COLOR_INICIO          = new Color(231, 76, 60);
    static final Color COLOR_FONDO           = new Color(40, 40, 48);
    static final Color COLOR_PANEL           = new Color(52, 52, 62);
    static final Color COLOR_ACENTO          = new Color(91, 168, 90);
    static final Color COLOR_TEXTO           = new Color(230, 230, 230);
    static final Color COLOR_LINEA           = new Color(91, 168, 90, 160);

    // ── Estado de la simulación ───────────────────────────────────────────────
    int[][] tablero      = new int[N][N];
    int[]   secuenciaX   = new int[N * N];
    int[]   secuenciaY   = new int[N * N];
    int     pasoActual   = 0;
    int     totalPasos   = 0;
    boolean resuelto     = false;
    int     inicioX      = 0;
    int     inicioY      = 0;

    // ── Componentes UI ────────────────────────────────────────────────────────
    TableroPanel tableroPanel;
    JLabel       lblEstado;
    JLabel       lblPaso;
    JButton      btnResolver;
    JButton      btnAnterior;
    JButton      btnSiguiente;
    JButton      btnPlay;
    JButton      btnReset;
    JSpinner     spnFila;
    JSpinner     spnColumna;
    JCheckBox    chkRandom;
    JSlider      sldVelocidad;
    Timer        timerAnimacion;
    boolean      reproduciendo = false;

    // ── Selección de inicio en tablero ────────────────────────────────────────
    boolean esperandoClick = false;

    public CaballoSwing() {
        super("♞ Recorrido del Caballo — Knight's Tour");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(COLOR_FONDO);
        construirUI();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Construcción de la interfaz
    // ─────────────────────────────────────────────────────────────────────────
    private void construirUI() {
        setLayout(new BorderLayout(16, 16));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(16, 16, 16, 16));

        // ── Título ────────────────────────────────────────────────────────────
        JLabel titulo = new JLabel("♞  Recorrido del Caballo", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titulo.setForeground(COLOR_TEXTO);
        titulo.setBorder(new EmptyBorder(0, 0, 8, 0));
        add(titulo, BorderLayout.NORTH);

        // ── Centro: tablero ───────────────────────────────────────────────────
        tableroPanel = new TableroPanel();
        tableroPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tableroPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!esperandoClick) return;
                int cellSize = tableroPanel.getCellSize();
                int col = e.getX() / cellSize;
                int row = e.getY() / cellSize;
                if (row >= 0 && row < N && col >= 0 && col < N) {
                    spnFila.setValue(row + 1);
                    spnColumna.setValue(col + 1);
                    esperandoClick = false;
                    tableroPanel.setCursor(Cursor.getDefaultCursor());
                    lblEstado.setText("Posición seleccionada: fila " + (row + 1) + ", col " + (col + 1) + ". Presiona Resolver.");
                    tableroPanel.highlightCell(row, col);
                }
            }
        });
        add(tableroPanel, BorderLayout.CENTER);

        // ── Panel derecho: controles ──────────────────────────────────────────
        JPanel panelControl = new JPanel();
        panelControl.setLayout(new BoxLayout(panelControl, BoxLayout.Y_AXIS));
        panelControl.setBackground(COLOR_PANEL);
        panelControl.setBorder(new CompoundBorder(
                new LineBorder(new Color(80, 80, 95), 1, true),
                new EmptyBorder(16, 14, 16, 14)));
        panelControl.setPreferredSize(new Dimension(220, 480));

        // -- Sección: posición de inicio
        panelControl.add(seccionLabel("POSICIÓN DE INICIO"));
        panelControl.add(Box.createVerticalStrut(8));

        chkRandom = new JCheckBox("Posición aleatoria");
        estilizarCheck(chkRandom);
        chkRandom.addActionListener(e -> actualizarModoInicio());
        panelControl.add(chkRandom);
        panelControl.add(Box.createVerticalStrut(8));

        JPanel panelCoords = new JPanel(new GridLayout(2, 2, 6, 6));
        panelCoords.setBackground(COLOR_PANEL);

        JLabel lblFila = new JLabel("Fila (1-8):");
        lblFila.setForeground(COLOR_TEXTO);
        lblFila.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel lblCol = new JLabel("Columna (1-8):");
        lblCol.setForeground(COLOR_TEXTO);
        lblCol.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        spnFila     = crearSpinner(1, 8, 1);
        spnColumna  = crearSpinner(1, 8, 1);

        panelCoords.add(lblFila);
        panelCoords.add(spnFila);
        panelCoords.add(lblCol);
        panelCoords.add(spnColumna);
        panelControl.add(panelCoords);

        panelControl.add(Box.createVerticalStrut(6));
        JButton btnClickTablero = crearBoton("🖱  Seleccionar en tablero", new Color(70, 90, 130));
        btnClickTablero.addActionListener(e -> {
            esperandoClick = true;
            tableroPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            lblEstado.setText("Haz clic en el tablero para elegir la posición inicial.");
        });
        panelControl.add(btnClickTablero);

        panelControl.add(Box.createVerticalStrut(16));
        panelControl.add(separador());

        // -- Sección: acción resolver
        panelControl.add(Box.createVerticalStrut(12));
        panelControl.add(seccionLabel("ALGORITMO"));
        panelControl.add(Box.createVerticalStrut(8));

        btnResolver = crearBoton("⚙  Resolver", new Color(60, 110, 80));
        btnResolver.addActionListener(e -> resolver());
        panelControl.add(btnResolver);

        btnReset = crearBoton("↺  Reiniciar", new Color(90, 60, 60));
        btnReset.addActionListener(e -> reiniciar());
        panelControl.add(Box.createVerticalStrut(6));
        panelControl.add(btnReset);

        panelControl.add(Box.createVerticalStrut(16));
        panelControl.add(separador());

        // -- Sección: navegación paso a paso
        panelControl.add(Box.createVerticalStrut(12));
        panelControl.add(seccionLabel("NAVEGACIÓN"));
        panelControl.add(Box.createVerticalStrut(8));

        JPanel panelNav = new JPanel(new GridLayout(1, 2, 6, 0));
        panelNav.setBackground(COLOR_PANEL);
        btnAnterior = crearBoton("◀", new Color(70, 70, 90));
        btnSiguiente = crearBoton("▶", new Color(70, 70, 90));
        btnAnterior.addActionListener(e -> retrocederPaso());
        btnSiguiente.addActionListener(e -> avanzarPaso());
        panelNav.add(btnAnterior);
        panelNav.add(btnSiguiente);
        panelControl.add(panelNav);

        panelControl.add(Box.createVerticalStrut(6));
        btnPlay = crearBoton("▶▶  Reproducir", new Color(60, 100, 130));
        btnPlay.addActionListener(e -> togglePlay());
        panelControl.add(btnPlay);

        panelControl.add(Box.createVerticalStrut(10));
        JLabel lblVel = new JLabel("Velocidad:");
        lblVel.setForeground(new Color(170, 170, 170));
        lblVel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblVel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelControl.add(lblVel);

        sldVelocidad = new JSlider(50, 800, 300);
        sldVelocidad.setBackground(COLOR_PANEL);
        sldVelocidad.setForeground(COLOR_TEXTO);
        sldVelocidad.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        sldVelocidad.setInverted(true); // izquierda = rápido
        panelControl.add(sldVelocidad);

        panelControl.add(Box.createVerticalStrut(16));
        panelControl.add(separador());

        // -- Info paso
        panelControl.add(Box.createVerticalStrut(10));
        lblPaso = new JLabel("Paso: — / —");
        lblPaso.setForeground(COLOR_ACENTO);
        lblPaso.setFont(new Font("Segoe UI Mono", Font.BOLD, 14));
        lblPaso.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelControl.add(lblPaso);

        panelControl.add(Box.createVerticalGlue());
        add(panelControl, BorderLayout.EAST);

        // ── Barra de estado inferior ──────────────────────────────────────────
        lblEstado = new JLabel("Elige una posición de inicio y presiona Resolver.");
        lblEstado.setForeground(new Color(180, 180, 180));
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEstado.setBorder(new EmptyBorder(8, 0, 0, 0));
        add(lblEstado, BorderLayout.SOUTH);

        // ── Timer animación ───────────────────────────────────────────────────
        timerAnimacion = new Timer(300, e -> {
            if (pasoActual < totalPasos) {
                avanzarPaso();
            } else {
                detenerPlay();
            }
        });

        actualizarBotones();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Lógica del algoritmo (backtracking)
    // ─────────────────────────────────────────────────────────────────────────
    private void resolver() {
        detenerPlay();
        int fila = chkRandom.isSelected()
                ? new Random().nextInt(N)
                : (int) spnFila.getValue() - 1;
        int col  = chkRandom.isSelected()
                ? new Random().nextInt(N)
                : (int) spnColumna.getValue() - 1;

        inicioX = fila;
        inicioY = col;

        // Reiniciar tablero
        for (int[] r : tablero) java.util.Arrays.fill(r, -1);
        tablero[fila][col] = 0;

        lblEstado.setText("Calculando solución desde (" + (fila+1) + ", " + (col+1) + ")…");
        btnResolver.setEnabled(false);
        repaint();

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                Estado est = new Estado(tablero, 0);
                return resolverDesde(fila, col, est);
            }

            @Override
            protected void done() {
                try {
                    resuelto = get();
                } catch (Exception ex) { resuelto = false; }

                if (resuelto) {
                    // Construir secuencia de movimientos
                    int[][] posiciones = new int[N * N][2];
                    for (int i = 0; i < N; i++)
                        for (int j = 0; j < N; j++)
                            posiciones[tablero[i][j]] = new int[]{i, j};
                    for (int k = 0; k < N * N; k++) {
                        secuenciaX[k] = posiciones[k][0];
                        secuenciaY[k] = posiciones[k][1];
                    }
                    totalPasos = N * N - 1;
                    pasoActual = 0;
                    lblEstado.setText("¡Solución encontrada! Navega paso a paso o reproduce.");
                    tableroPanel.resetVisualizacion();
                    tableroPanel.mostrarPaso(0);
                } else {
                    lblEstado.setText("No se encontró solución desde esa posición.");
                }
                btnResolver.setEnabled(true);
                actualizarBotones();
                tableroPanel.repaint();
            }
        };
        worker.execute();
    }

    static class Estado {
        int[][] tablero;
        int paso;
        Estado(int[][] tablero, int paso) {
            this.tablero = tablero;
            this.paso = paso;
        }
    }

    static boolean resolverDesde(int x, int y, Estado estado) {
        if (estado.paso == N * N - 1) return true;
        for (int i = 0; i < 8; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];
            if (esValido(nx, ny, estado.tablero)) {
                estado.tablero[nx][ny] = estado.paso + 1;
                estado.paso++;
                if (resolverDesde(nx, ny, estado)) return true;
                estado.tablero[nx][ny] = -1;
                estado.paso--;
            }
        }
        return false;
    }

    static boolean esValido(int x, int y, int[][] tablero) {
        return x >= 0 && y >= 0 && x < N && y < N && tablero[x][y] == -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Navegación
    // ─────────────────────────────────────────────────────────────────────────
    private void avanzarPaso() {
        if (!resuelto || pasoActual >= totalPasos) return;
        pasoActual++;
        tableroPanel.mostrarPaso(pasoActual);
        actualizarBotones();
    }

    private void retrocederPaso() {
        if (!resuelto || pasoActual <= 0) return;
        pasoActual--;
        tableroPanel.mostrarPaso(pasoActual);
        actualizarBotones();
    }

    private void togglePlay() {
        if (reproduciendo) {
            detenerPlay();
        } else {
            if (!resuelto) return;
            if (pasoActual >= totalPasos) {
                pasoActual = 0;
                tableroPanel.resetVisualizacion();
                tableroPanel.mostrarPaso(0);
            }
            reproduciendo = true;
            timerAnimacion.setDelay(sldVelocidad.getValue());
            timerAnimacion.start();
            btnPlay.setText("⏹  Detener");
        }
    }

    private void detenerPlay() {
        reproduciendo = false;
        timerAnimacion.stop();
        btnPlay.setText("▶▶  Reproducir");
    }

    private void reiniciar() {
        detenerPlay();
        resuelto   = false;
        pasoActual = 0;
        totalPasos = 0;
        for (int[] r : tablero) java.util.Arrays.fill(r, -1);
        tableroPanel.resetVisualizacion();
        tableroPanel.repaint();
        lblEstado.setText("Elige una posición de inicio y presiona Resolver.");
        lblPaso.setText("Paso: — / —");
        actualizarBotones();
    }

    private void actualizarBotones() {
        boolean ok = resuelto;
        btnAnterior.setEnabled(ok && pasoActual > 0);
        btnSiguiente.setEnabled(ok && pasoActual < totalPasos);
        btnPlay.setEnabled(ok);
        lblPaso.setText(ok ? "Paso: " + pasoActual + " / " + totalPasos : "Paso: — / —");
        timerAnimacion.setDelay(sldVelocidad.getValue());
    }

    private void actualizarModoInicio() {
        boolean random = chkRandom.isSelected();
        spnFila.setEnabled(!random);
        spnColumna.setEnabled(!random);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers UI
    // ─────────────────────────────────────────────────────────────────────────
    private JLabel seccionLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(new Color(130, 130, 150));
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JSeparator separador() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(75, 75, 90));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private JButton crearBoton(String texto, Color bg) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addMouseListener(new MouseAdapter() {
            Color orig = bg;
            public void mouseEntered(MouseEvent e) { btn.setBackground(orig.brighter()); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(orig); }
        });
        return btn;
    }

    private JSpinner crearSpinner(int min, int max, int val) {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(val, min, max, 1));
        sp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sp.setMaximumSize(new Dimension(80, 28));
        return sp;
    }

    private void estilizarCheck(JCheckBox chk) {
        chk.setBackground(COLOR_PANEL);
        chk.setForeground(COLOR_TEXTO);
        chk.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chk.setAlignmentX(Component.LEFT_ALIGNMENT);
        chk.setFocusPainted(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Panel del tablero con dibujo personalizado
    // ─────────────────────────────────────────────────────────────────────────
    class TableroPanel extends JPanel {
        private static final int CELL = 60;
        private static final int OFFSET = 24; // espacio para coordenadas

        // Qué pasos mostrar actualmente
        int pasosMostrados = -1;
        int cellHighlightR = -1, cellHighlightC = -1;

        TableroPanel() {
            int size = N * CELL + OFFSET;
            setPreferredSize(new Dimension(size, size));
            setBackground(COLOR_FONDO);
        }

        int getCellSize() { return CELL; }

        void mostrarPaso(int paso) {
            pasosMostrados = paso;
            actualizarBotones();
            repaint();
        }

        void resetVisualizacion() {
            pasosMostrados = -1;
            cellHighlightR = -1;
            cellHighlightC = -1;
        }

        void highlightCell(int r, int c) {
            cellHighlightR = r;
            cellHighlightC = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // ── Coordenadas del borde ─────────────────────────────────────────
            g2.setFont(new Font("Segoe UI Mono", Font.BOLD, 11));
            g2.setColor(new Color(150, 150, 170));
            for (int i = 0; i < N; i++) {
                // letras (columnas): A-H
                String letra = String.valueOf((char)('A' + i));
                g2.drawString(letra, OFFSET + i * CELL + CELL / 2 - 4, 14);
                // números (filas): 1-8
                g2.drawString(String.valueOf(i + 1), 6, OFFSET + i * CELL + CELL / 2 + 5);
            }

            // ── Celdas ───────────────────────────────────────────────────────
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    int x = OFFSET + c * CELL;
                    int y = OFFSET + r * CELL;
                    boolean claras = (r + c) % 2 == 0;

                    // Determinar si esta celda fue visitada hasta el paso actual
                    boolean visitada = false;
                    int numPaso = -1;
                    if (resuelto && pasosMostrados >= 0) {
                        numPaso = tablero[r][c];
                        visitada = numPaso >= 0 && numPaso <= pasosMostrados;
                    }

                    // Color base
                    Color base;
                    if (visitada) {
                        base = claras ? COLOR_VISITADA_CLARA : COLOR_VISITADA_OSCURA;
                    } else {
                        base = claras ? COLOR_CASILLA_CLARA : COLOR_CASILLA_OSCURA;
                    }
                    g2.setColor(base);
                    g2.fillRect(x, y, CELL, CELL);

                    // Resaltado posición de inicio
                    if (resuelto && r == inicioX && c == inicioY) {
                        g2.setColor(new Color(231, 76, 60, 80));
                        g2.fillRect(x, y, CELL, CELL);
                        g2.setColor(new Color(231, 76, 60));
                        g2.setStroke(new BasicStroke(2.5f));
                        g2.drawRect(x + 1, y + 1, CELL - 2, CELL - 2);
                        g2.setStroke(new BasicStroke(1f));
                    }

                    // Resaltado celda actual (posición del caballo)
                    boolean esCeldasActual = resuelto && pasosMostrados >= 0
                            && secuenciaX[pasosMostrados] == r
                            && secuenciaY[pasosMostrados] == c;
                    if (esCeldasActual) {
                        g2.setColor(new Color(255, 236, 80, 200));
                        g2.fillRect(x, y, CELL, CELL);
                    }

                    // Celda de highlight (selección manual)
                    if (!resuelto && cellHighlightR == r && cellHighlightC == c) {
                        g2.setColor(new Color(91, 168, 90, 120));
                        g2.fillRect(x, y, CELL, CELL);
                        g2.setColor(COLOR_ACENTO);
                        g2.setStroke(new BasicStroke(2f));
                        g2.drawRect(x + 1, y + 1, CELL - 2, CELL - 2);
                        g2.setStroke(new BasicStroke(1f));
                    }

                    // Número de paso dentro de la celda
                    if (visitada && numPaso >= 0) {
                        boolean esActual = esCeldasActual;
                        g2.setColor(esActual ? new Color(40, 40, 40) : new Color(30, 60, 30));
                        g2.setFont(new Font("Segoe UI Mono", Font.BOLD, numPaso < 10 ? 15 : 13));
                        String num = String.valueOf(numPaso);
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(num,
                                x + (CELL - fm.stringWidth(num)) / 2,
                                y + (CELL + fm.getAscent()) / 2 - 2);
                    }
                }
            }

            // ── Líneas del recorrido ──────────────────────────────────────────
            if (resuelto && pasosMostrados >= 1) {
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int p = 1; p <= pasosMostrados; p++) {
                    int x1 = OFFSET + secuenciaY[p - 1] * CELL + CELL / 2;
                    int y1 = OFFSET + secuenciaX[p - 1] * CELL + CELL / 2;
                    int x2 = OFFSET + secuenciaY[p]     * CELL + CELL / 2;
                    int y2 = OFFSET + secuenciaX[p]     * CELL + CELL / 2;

                    // Gradiente de color según avance
                    float t = (float) p / totalPasos;
                    Color lineColor = interpolarColor(
                            new Color(91, 168, 90, 200),
                            new Color(52, 120, 200, 200), t);
                    g2.setColor(lineColor);
                    g2.drawLine(x1, y1, x2, y2);
                }
                g2.setStroke(new BasicStroke(1f));
            }

            // ── Caballo (símbolo) en posición actual ──────────────────────────
            if (resuelto && pasosMostrados >= 0) {
                int cr = secuenciaX[pasosMostrados];
                int cc = secuenciaY[pasosMostrados];
                int kx = OFFSET + cc * CELL;
                int ky = OFFSET + cr * CELL;
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 30));
                g2.setColor(new Color(30, 30, 30));
                g2.drawString("♞", kx + 8, ky + CELL - 10);
            }

            // ── Borde externo del tablero ─────────────────────────────────────
            g2.setColor(new Color(100, 80, 60));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(OFFSET, OFFSET, N * CELL, N * CELL);
            g2.setStroke(new BasicStroke(1f));
        }

        private Color interpolarColor(Color a, Color b, float t) {
            int r = (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t);
            int g = (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bv = (int)(a.getBlue() + (b.getBlue()  - a.getBlue())  * t);
            int al = (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
            return new Color(r, g, bv, al);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Main
    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(CaballoSwing::new);
    }
}