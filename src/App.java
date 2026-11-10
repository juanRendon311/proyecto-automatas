import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}

class MainWindow extends JFrame {
    static final String[] TITLES = {
        "Nivel 0: Máquina de Turing",
        "Nivel 1: Cinta acotada",
        "Nivel 2: Autómata con pila",
        "Nivel 3: Autómata finito"
    };

    static final String[] DESCRIPTIONS = {
        "Reconoce 0^n1^n. Marca cada 0 y busca su 1 correspondiente.",
        "Reconoce a^nb^nc^n. Marca una a, una b y una c en la cinta.",
        "Reconoce a^nb^n. Apila una A por cada a y desapila por cada b.",
        "Reconoce cadenas que terminan en un sufijo configurable."
    };

    static final String[][] EXAMPLES = {
        {"0011", "000111", "001", "0101"},
        {"aabbcc", "abc", "aabcc", "abcc"},
        {"aaabbb", "aabb", "aabbb", "abab"},
        {"aab", "abba", "abab", "baa"}
    };

    static final Color[] COLORS = {
        new Color(124, 92, 230),
        new Color(20, 156, 130),
        new Color(238, 151, 53),
        new Color(48, 119, 220)
    };

    MainWindow() {
        setTitle("Jerarquía de Chomsky - Simulador");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 720);
        setMinimumSize(new Dimension(760, 580));
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        for (int level = 0; level < TITLES.length; level++) {
            tabs.addTab(TITLES[level], new ModulePanel(level));
            tabs.setForegroundAt(level, COLORS[level]);
        }
        add(tabs);
    }
}

class ModulePanel extends JPanel {
    private final int level;
    private final Color accent;
    private final JTextField input = new JTextField();
    private final JTextField suffix = new JTextField("ab", 5);
    private final JComboBox<String> examples;
    private final JComboBox<String> speed = new JComboBox<>(new String[] {"Lento", "Normal", "Rápido"});
    private final JTextArea log = new JTextArea();
    private final JLabel resultLabel = new JLabel("Carga un ejemplo o escribe una cadena para empezar.");
    private final JProgressBar progress = new JProgressBar();
    private final TapeView visualization;
    private final JButton previousButton = new JButton("← Anterior");
    private final JButton nextButton = new JButton("Siguiente →");
    private final JButton playButton = new JButton("▶ Reproducir");
    private final JButton resetButton = new JButton("Reiniciar");

    private Simulator.Result result;
    private int currentFrame;
    private int lastLoggedFrame = -1;
    private Timer timer;

    ModulePanel(int level) {
        this.level = level;
        accent = MainWindow.COLORS[level];
        examples = new JComboBox<>(MainWindow.EXAMPLES[level]);
        visualization = new TapeView(level, accent);
        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(245, 247, 251));
        setBorder(BorderFactory.createEmptyBorder(18, 22, 16, 22));

        add(createHeading(), BorderLayout.NORTH);
        add(createWorkspace(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);

        previousButton.setEnabled(false);
        nextButton.setEnabled(false);
        playButton.setEnabled(false);
        resetButton.setEnabled(false);
    }

    private JPanel createHeading() {
        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setBackground(Color.WHITE);
        heading.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, accent),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        JLabel title = new JLabel("<html><b>" + MainWindow.TITLES[level]
                + "</b><br><span style='font-size:11pt;color:#667085'>"
                + MainWindow.DESCRIPTIONS[level] + "</span></html>");
        title.setFont(new Font("SansSerif", Font.PLAIN, 16));
        heading.add(title, BorderLayout.CENTER);
        JLabel badge = new JLabel("MODELO " + (3 - level));
        badge.setForeground(Color.WHITE);
        badge.setBackground(accent);
        badge.setOpaque(true);
        badge.setFont(new Font("SansSerif", Font.BOLD, 11));
        badge.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        heading.add(badge, BorderLayout.EAST);
        return heading;
    }

    private JPanel createWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout(10, 10));
        workspace.setOpaque(false);
        workspace.add(createControls(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setBackground(Color.WHITE);
        center.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 227, 236)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel visualTitle = new JLabel(level == 3 ? "ESTADOS DEL AUTÓMATA Y CADENA"
                : level == 2 ? "CINTA DE ENTRADA Y PILA" : "CINTA DE SIMULACIÓN");
        visualTitle.setForeground(accent);
        visualTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        center.add(visualTitle, BorderLayout.NORTH);
        center.add(visualization, BorderLayout.CENTER);
        workspace.add(center, BorderLayout.CENTER);

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        log.setForeground(new Color(55, 65, 81));
        log.setBackground(new Color(250, 251, 253));
        log.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setPreferredSize(new Dimension(100, 100));
        logScroll.setBorder(BorderFactory.createTitledBorder("Historial de transiciones"));
        workspace.add(logScroll, BorderLayout.SOUTH);
        return workspace;
    }

    private JPanel createControls() {
        JPanel controls = new JPanel(new BorderLayout(6, 8));
        controls.setBackground(Color.WHITE);
        controls.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 227, 236)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JPanel inputs = new JPanel();
        inputs.setOpaque(false);
        inputs.add(new JLabel("Cadena:"));
        input.setPreferredSize(new Dimension(150, 30));
        input.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        styleField(input);
        inputs.add(input);
        if (level == 3) {
            inputs.add(new JLabel("Sufijo:"));
            suffix.setPreferredSize(new Dimension(58, 30));
            suffix.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            styleField(suffix);
            inputs.add(suffix);
        }
        inputs.add(examples);
        JButton loadExample = makeButton("Usar ejemplo", new Color(232, 238, 248), new Color(50, 67, 94));
        loadExample.addActionListener(event -> input.setText((String) examples.getSelectedItem()));
        inputs.add(loadExample);
        controls.add(inputs, BorderLayout.NORTH);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton simulate = makeButton("▶ Simular", accent, Color.WHITE);
        simulate.addActionListener(this::simulate);
        actions.add(simulate);
        previousButton.addActionListener(event -> showPreviousFrame());
        styleButton(previousButton, new Color(232, 238, 248), new Color(50, 67, 94));
        actions.add(previousButton);
        nextButton.addActionListener(event -> showNextFrame());
        styleButton(nextButton, new Color(232, 238, 248), new Color(50, 67, 94));
        actions.add(nextButton);
        playButton.addActionListener(event -> play());
        styleButton(playButton, accent, Color.WHITE);
        actions.add(playButton);
        resetButton.addActionListener(event -> reset());
        styleButton(resetButton, new Color(255, 237, 237), new Color(173, 57, 57));
        actions.add(resetButton);
        actions.add(new JLabel("Velocidad:"));
        speed.setSelectedIndex(1);
        speed.addActionListener(event -> updateSpeed());
        actions.add(speed);
        controls.add(actions, BorderLayout.SOUTH);
        return controls;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(10, 5));
        footer.setOpaque(false);
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        footer.add(resultLabel, BorderLayout.NORTH);
        progress.setStringPainted(true);
        progress.setString("Listo");
        progress.setForeground(accent);
        progress.setBackground(Color.WHITE);
        footer.add(progress, BorderLayout.SOUTH);
        return footer;
    }

    private void simulate(ActionEvent event) {
        stopTimer();
        playButton.setText("▶ Reproducir");
        String text = input.getText().trim();

        // Cada pestaña llama al simulador correspondiente a su nivel.
        switch (level) {
            case 0:
                result = Simulator.turing(text);
                break;
            case 1:
                result = Simulator.boundedTape(text);
                break;
            case 2:
                result = Simulator.pushdown(text);
                break;
            default:
                result = Simulator.regular(text, suffix.getText().trim());
                break;
        }

        currentFrame = 0;
        lastLoggedFrame = -1;
        log.setText("");
        boolean hasFrames = !result.frames.isEmpty();
        previousButton.setEnabled(false);
        nextButton.setEnabled(hasFrames);
        playButton.setEnabled(hasFrames);
        resetButton.setEnabled(hasFrames);

        if (hasFrames) {
            showFrame();
        } else {
            visualization.clear();
            log.setText(result.message);
            resultLabel.setText(result.message);
            resultLabel.setForeground(new Color(185, 55, 55));
            progress.setValue(0);
            progress.setString("Entrada inválida");
        }
    }

    private void showPreviousFrame() {
        if (result != null && currentFrame > 0) {
            currentFrame--;
            showFrame();
        }
    }

    private void showNextFrame() {
        if (result != null && currentFrame < result.frames.size() - 1) {
            currentFrame++;
            showFrame();
        }
    }

    private void showFrame() {
        Simulator.Frame frame = result.frames.get(currentFrame);
        visualization.showFrame(frame, suffix.getText().trim().length());
        if (currentFrame <= lastLoggedFrame) {
            log.setText("");
            for (int i = 0; i <= currentFrame; i++) {
                appendFrame(i);
            }
        } else {
            appendFrame(currentFrame);
        }
        lastLoggedFrame = currentFrame;
        log.setCaretPosition(log.getDocument().getLength());

        int total = Math.max(1, result.frames.size() - 1);
        progress.setMaximum(total);
        progress.setValue(currentFrame);
        progress.setString("Paso " + currentFrame + " de " + (result.frames.size() - 1));

        boolean finished = currentFrame == result.frames.size() - 1;
        previousButton.setEnabled(currentFrame > 0);
        nextButton.setEnabled(!finished);
        playButton.setEnabled(!finished);
        if (finished) {
            resultLabel.setText(result.message);
            resultLabel.setForeground(result.accepted ? new Color(28, 135, 85) : new Color(185, 55, 55));
            progress.setForeground(result.accepted ? new Color(28, 160, 100) : new Color(200, 70, 70));
        } else {
            resultLabel.setText("Estado " + frame.state + "  ·  " + frame.note);
            resultLabel.setForeground(new Color(55, 65, 81));
        }
    }

    private void play() {
        if (timer != null && timer.isRunning()) {
            stopTimer();
            playButton.setText("▶ Reproducir");
            return;
        }

        playButton.setText("Ⅱ Pausar");
        timer = new Timer(delayForSpeed(), event -> {
            if (currentFrame < result.frames.size() - 1) {
                currentFrame++;
                showFrame();
            } else {
                stopTimer();
                playButton.setText("▶ Reproducir");
            }
        });
        timer.start();
    }

    private void reset() {
        stopTimer();
        playButton.setText("▶ Reproducir");
        currentFrame = 0;
        lastLoggedFrame = -1;
        log.setText("");
        showFrame();
    }

    private void appendFrame(int index) {
        Simulator.Frame frame = result.frames.get(index);
        log.append(String.format("%02d  %-12s %s%n", index, frame.state, frame.note));
    }

    private void updateSpeed() {
        if (timer != null && timer.isRunning()) {
            timer.setDelay(delayForSpeed());
        }
    }

    private int delayForSpeed() {
        switch (speed.getSelectedIndex()) {
            case 0: return 1000;
            case 2: return 180;
            default: return 550;
        }
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    private void styleField(JTextField field) {
        field.setBackground(new Color(249, 250, 252));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 213, 225)),
                BorderFactory.createEmptyBorder(4, 7, 4, 7)));
    }

    private JButton makeButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        styleButton(button, background, foreground);
        return button;
    }

    private void styleButton(JButton button, Color background, Color foreground) {
        button.setBackground(background);
        button.setForeground(foreground);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.darker()),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
    }
}

class TapeView extends JPanel {
    private final int level;
    private final Color accent;
    private Simulator.Frame frame;
    private int stateCount = 2;

    TapeView(int level, Color accent) {
        this.level = level;
        this.accent = accent;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(600, 230));
    }

    void clear() {
        frame = null;
        repaint();
    }

    void showFrame(Simulator.Frame frame, int suffixLength) {
        this.frame = frame;
        stateCount = Math.max(2, suffixLength + 1);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (frame == null) {
            g.setColor(new Color(125, 137, 155));
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            centered(g, "Carga una cadena para ver la simulación", getWidth() / 2, getHeight() / 2);
        } else if (level == 3) {
            drawStates(g);
        } else {
            drawTape(g);
            if (level == 2) {
                drawStack(g);
            }
        }
        g.dispose();
    }

    private void drawStates(Graphics2D g) {
        int radius = 23;
        int gap = Math.min(90, (getWidth() - 70) / Math.max(1, stateCount - 1));
        int start = (getWidth() - gap * (stateCount - 1)) / 2;
        int y = Math.max(48, getHeight() / 3);
        int active = stateNumber(frame.state);

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        for (int i = 0; i < stateCount; i++) {
            int x = start + i * gap;
            if (i < stateCount - 1) {
                g.setColor(new Color(175, 185, 200));
                g.setStroke(new BasicStroke(2));
                g.drawLine(x + radius, y, x + gap - radius, y);
                g.fillPolygon(new int[] {x + gap - radius, x + gap - radius - 7, x + gap - radius - 7},
                        new int[] {y, y - 5, y + 5}, 3);
            }
            g.setColor(i == active ? accent : new Color(239, 243, 249));
            g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            g.setColor(i == active ? accent : new Color(190, 199, 212));
            g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
            if (i == stateCount - 1) {
                g.drawOval(x - radius + 4, y - radius + 4, radius * 2 - 8, radius * 2 - 8);
            }
            g.setColor(i == active ? Color.WHITE : new Color(45, 55, 72));
            centered(g, "q" + i, x, y + 5);
        }
        g.setColor(new Color(75, 85, 100));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        centered(g, "Estado activo: " + frame.state, getWidth() / 2, y + 54);
        drawCells(g, frame.tape, frame.head, y + 83, 34, true);
    }

    private void drawTape(Graphics2D g) {
        String label = level == 2 ? "ENTRADA" : level == 1
                ? "CINTA ACOTADA · ⊢ inicio / ⊣ fin" : "CINTA EXTENSIBLE · □ blanco";
        g.setColor(new Color(75, 85, 100));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        centered(g, label + "     Estado: " + frame.state, getWidth() / 2, 35);
        drawCells(g, frame.tape, frame.head, 65, 42, false);
        g.setColor(accent);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        centered(g, frame.note, getWidth() / 2, 126);
    }

    private void drawCells(Graphics2D g, String tape, int head, int y, int cellWidth, boolean cursorAbove) {
        if (tape.isEmpty()) {
            g.setColor(new Color(115, 125, 140));
            centered(g, "(cadena vacía)", getWidth() / 2, y + 20);
            return;
        }
        int available = Math.max(80, getWidth() - 30);
        int visible = Math.max(1, Math.min(tape.length(), available / cellWidth));
        int start = Math.max(0, Math.min(head - visible / 2, tape.length() - visible));
        int x = (getWidth() - visible * cellWidth) / 2;

        for (int i = start; i < start + visible; i++) {
            char symbol = tape.charAt(i);
            Color fill = colorForSymbol(symbol);
            boolean selected = i == head;
            g.setColor(selected ? mix(fill, accent) : fill);
            g.fillRoundRect(x + 2, y, cellWidth - 4, 34, 8, 8);
            g.setColor(selected ? accent : new Color(210, 218, 230));
            g.drawRoundRect(x + 2, y, cellWidth - 4, 34, 8, 8);
            g.setColor(new Color(42, 52, 68));
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            centered(g, String.valueOf(symbol), x + cellWidth / 2, y + 22);
            if (selected) {
                g.setColor(accent);
                g.setFont(new Font("SansSerif", Font.BOLD, 12));
                centered(g, cursorAbove ? "▼" : "▲", x + cellWidth / 2, cursorAbove ? y + 51 : y - 6);
            }
            x += cellWidth;
        }
        if (start > 0 || start + visible < tape.length()) {
            g.setColor(new Color(115, 125, 140));
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            centered(g, "…", getWidth() / 2, y + 54);
        }
    }

    private void drawStack(Graphics2D g) {
        String stack = frame.extra;
        int width = Math.min(38, (getWidth() - 40) / Math.max(1, stack.length()));
        int start = (getWidth() - stack.length() * width) / 2;
        int y = Math.max(160, getHeight() - 55);
        g.setColor(accent);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        centered(g, "PILA · TOPE →", getWidth() / 2, y - 10);
        for (int i = 0; i < stack.length(); i++) {
            int x = start + i * width;
            g.setColor(i == stack.length() - 1 ? mix(Color.WHITE, accent) : new Color(246, 248, 251));
            g.fillRoundRect(x + 2, y, width - 4, 29, 7, 7);
            g.setColor(accent);
            g.drawRoundRect(x + 2, y, width - 4, 29, 7, 7);
            g.setColor(new Color(42, 52, 68));
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
            centered(g, String.valueOf(stack.charAt(i)), x + width / 2, y + 19);
        }
    }

    private Color colorForSymbol(char symbol) {
        switch (symbol) {
            case 'X': return new Color(255, 236, 190);
            case 'Y': return new Color(216, 238, 255);
            case 'Z': return new Color(220, 244, 224);
            case '□': return new Color(238, 240, 244);
            case '⊢':
            case '⊣': return new Color(229, 232, 239);
            default: return new Color(242, 246, 252);
        }
    }

    private int stateNumber(String state) {
        if (state.startsWith("q")) {
            try {
                return Integer.parseInt(state.substring(1));
            } catch (NumberFormatException exception) {
                return -1;
            }
        }
        return -1;
    }

    private Color mix(Color first, Color second) {
        return new Color((first.getRed() + second.getRed()) / 2,
                (first.getGreen() + second.getGreen()) / 2,
                (first.getBlue() + second.getBlue()) / 2);
    }

    private void centered(Graphics2D g, String text, int x, int y) {
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y);
    }
}
