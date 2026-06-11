package br.carmel.util;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * UIFactory — tema claro amarelado, elegante e de baixo cansaço visual.
 * Paleta: creme/âmbar como fundo, marrom escuro como texto, dourado como accent.
 */
public final class UIFactory {

    // ── Paleta principal (claro / amarelado / creme) ─────────────────────────
    public static final Color W11_BG           = new Color(252, 248, 238); // creme quente
    public static final Color W11_SURFACE      = new Color(255, 252, 244); // branco creme
    public static final Color W11_SURFACE2     = new Color(247, 241, 226); // creme médio
    public static final Color W11_PANEL_BG     = new Color(250, 246, 235); // painel
    public static final Color W11_ACCENT       = new Color(160, 110,  30); // âmbar/dourado
    public static final Color W11_ACCENT_DARK  = new Color(120,  80,  15); // dourado escuro
    public static final Color W11_ACCENT_LIGHT = new Color(240, 220, 170); // dourado pálido
    public static final Color W11_BORDER       = new Color(210, 195, 165); // borda suave
    public static final Color W11_BORDER_FOCUS = new Color(160, 110,  30); // foco dourado
    public static final Color W11_TEXT         = new Color( 55,  40,  20); // marrom escuro
    public static final Color W11_TEXT_SEC     = new Color(130, 110,  80); // marrom médio
    public static final Color W11_TABLE_SEL    = new Color(235, 210, 150); // seleção dourada
    public static final Color W11_TABLE_SEL_FG = new Color( 55,  40,  20);
    public static final Color W11_BTN_BG       = new Color(242, 234, 210); // botão creme
    public static final Color W11_BTN_HOVER    = new Color(230, 215, 175); // hover dourado
    public static final Color W11_BTN_PRESS    = new Color(215, 195, 145); // press
    public static final Color W11_MENU_BG      = new Color( 48,  36,  18); // menu escuro quente
    public static final Color W11_MENU_BTN     = new Color( 60,  46,  24);
    public static final Color W11_MENU_HOVER   = new Color(160, 110,  30);
    public static final Color W11_SUCCESS      = new Color( 60, 140,  60);
    public static final Color W11_DANGER       = new Color(185,  50,  50);
    public static final Color W11_WARNING      = new Color(190, 130,  20);

    // Aliases retrocompatíveis
    public static final Color XP_BG         = W11_BG;
    public static final Color XP_PANEL_BG   = W11_PANEL_BG;
    public static final Color XP_BTN_BG     = W11_BTN_BG;
    public static final Color XP_BTN_HOVER  = W11_BTN_HOVER;
    public static final Color XP_BTN_BORDER = W11_BORDER;
    public static final Color XP_FIELD_BG   = W11_SURFACE;
    public static final Color XP_FIELD_FG   = W11_TEXT;
    public static final Color XP_TABLE_SEL  = W11_TABLE_SEL;
    public static final Color XP_MENU_BG    = W11_MENU_BG;
    public static final Color XP_MENU_BTN   = W11_MENU_BTN;
    public static final Color XP_MENU_HOVER = W11_MENU_HOVER;

    // ── Tipografia ────────────────────────────────────────────────────────────
    public static final Font FONT_NORMAL = new Font("Segoe UI", Font.PLAIN,  12);
    public static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD,   12);
    public static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,   14);
    public static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN,  11);
    public static final Font FONT_LARGE  = new Font("Segoe UI", Font.BOLD,   17);

    private UIFactory() {}

    // ── Campos ────────────────────────────────────────────────────────────────

    public static JTextField styledField(String text) {
        JTextField f = new JTextField(text);
        applyFieldStyle(f);
        return f;
    }

    public static void styleSmallField(JTextField f) { applyFieldStyle(f); }

    public static JPasswordField styledPasswordField(String text) {
        JPasswordField pf = new JPasswordField(text);
        applyFieldStyle(pf);
        return pf;
    }

    public static JTextArea styledTextArea(int rows, int cols) {
        JTextArea ta = new JTextArea(rows, cols);
        ta.setBackground(W11_SURFACE);
        ta.setForeground(W11_TEXT);
        ta.setFont(FONT_NORMAL);
        ta.setCaretColor(W11_ACCENT);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        return ta;
    }

    private static void applyFieldStyle(javax.swing.text.JTextComponent f) {
        f.setBackground(W11_SURFACE);
        f.setForeground(W11_TEXT);
        f.setFont(FONT_NORMAL);
        f.setCaretColor(W11_ACCENT);
        f.setSelectionColor(W11_ACCENT_LIGHT);
        f.setSelectedTextColor(W11_TEXT);
        setBorderNormal(f);
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                setBorderFocused(f);
                SwingUtilities.invokeLater(f::selectAll);
            }
            @Override public void focusLost(FocusEvent e) { setBorderNormal(f); }
        });
    }

    private static void setBorderNormal(javax.swing.text.JTextComponent f) {
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(W11_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }
    private static void setBorderFocused(javax.swing.text.JTextComponent f) {
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(W11_BORDER_FOCUS, 2),
                BorderFactory.createEmptyBorder(3, 7, 3, 7)));
    }

    // ── Labels ────────────────────────────────────────────────────────────────

    public static JLabel labelLight(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(W11_TEXT);
        l.setFont(FONT_NORMAL);
        return l;
    }

    // ── Botão com RIPPLE ──────────────────────────────────────────────────────

    private static JButton rippleButton(String text,
                                        Color bgNormal, Color bgHover, Color bgPress, Color fg) {
        return new JButton(text) {
            float ripR = 0, ripA = 0;
            int   rx, ry;
            float glowA = 0;
            boolean hov = false;
            {
                setOpaque(false); setContentAreaFilled(false); setFocusPainted(false);
                setForeground(fg); setBackground(bgNormal); setFont(FONT_NORMAL);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hov = true;  glow(true);  setBackground(bgHover); }
                    @Override public void mouseExited(MouseEvent e)  { hov = false; glow(false); setBackground(bgNormal); }
                    @Override public void mousePressed(MouseEvent e) {
                        setBackground(bgPress);
                        rx = e.getX(); ry = e.getY(); ripR = 0; ripA = 0.3f;
                        Timer t = new Timer(14, ev -> {
                            ripR += getWidth() * 0.08f; ripA -= 0.022f;
                            if (ripA <= 0) { ripA = 0; ((Timer)ev.getSource()).stop(); }
                            repaint();
                        }); t.start();
                    }
                    @Override public void mouseReleased(MouseEvent e) { setBackground(hov ? bgHover : bgNormal); }
                });
            }
            void glow(boolean in) {
                Timer t = new Timer(16, null);
                t.addActionListener(e -> {
                    glowA += in ? 0.08f : -0.08f;
                    glowA  = Math.max(0, Math.min(1, glowA));
                    repaint();
                    if ((in && glowA >= 1) || (!in && glowA <= 0)) ((Timer)e.getSource()).stop();
                }); t.start();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                if (ripA > 0) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ripA));
                    g2.setColor(new Color(255, 220, 100));
                    float d = ripR * 2;
                    g2.fill(new Ellipse2D.Float(rx - ripR, ry - ripR, d, d));
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
                g2.dispose();
                super.paintComponent(g);
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (glowA > 0) {
                    g2.setColor(new Color(160, 110, 30, (int)(glowA * 80)));
                    g2.setStroke(new BasicStroke(3f));
                    g2.draw(new RoundRectangle2D.Float(1, 1, getWidth()-2, getHeight()-2, 8, 8));
                }
                g2.setColor(W11_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 8, 8));
                g2.dispose();
            }
        };
    }

    // ── Botões públicos ───────────────────────────────────────────────────────

    public static JButton primaryButton(String text, ActionListener action) {
        JButton b = rippleButton(text, W11_ACCENT, W11_ACCENT_DARK,
                new Color(100, 70, 10), Color.WHITE);
        b.setFont(FONT_BOLD);
        b.setPreferredSize(new Dimension(130, 32));
        b.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));
        if (action != null) b.addActionListener(action);
        return b;
    }

    public static JButton bigActionButton(String text, ActionListener a) {
        JButton b = rippleButton(text, W11_BTN_BG, W11_BTN_HOVER, W11_BTN_PRESS, W11_TEXT);
        b.setFont(FONT_NORMAL);
        b.setPreferredSize(new Dimension(120, 30));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(W11_BORDER, 1),
                BorderFactory.createEmptyBorder(3, 12, 3, 12)));
        if (a != null) b.addActionListener(a);
        return b;
    }

    public static JButton bigSmallButton(String text) { return bigActionButton(text, null); }

    public static void styleActionButton(JButton b) {
        b.setBackground(W11_BTN_BG); b.setForeground(W11_TEXT); b.setFont(FONT_NORMAL);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(W11_BORDER, 1),
                BorderFactory.createEmptyBorder(3, 12, 3, 12)));
    }

    // ── Botão de menu lateral ─────────────────────────────────────────────────

    public static JButton bigMenuButton(String text, ActionListener action) {
        JButton b = new JButton(text) {
            float anim = 0; boolean hov = false;
            {
                setOpaque(false); setContentAreaFilled(false); setFocusPainted(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hov=true;  startA(); }
                    @Override public void mouseExited(MouseEvent e)  { hov=false; startA(); }
                });
            }
            void startA() {
                Timer t = new Timer(16, null);
                t.addActionListener(e -> {
                    anim += hov ? 0.12f : -0.12f; anim = Math.max(0, Math.min(1, anim)); repaint();
                    if ((hov && anim >= 1) || (!hov && anim <= 0)) ((Timer)e.getSource()).stop();
                }); t.start();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(interpolate(W11_MENU_BTN, W11_MENU_HOVER, anim));
                g2.fill(new RoundRectangle2D.Float(4, 2, getWidth()-8, getHeight()-4, 8, 8));
                if (anim > 0) {
                    g2.setColor(new Color(210, 160, 60, (int)(anim * 255)));
                    g2.fillRect(4, 2, 3, getHeight()-4);
                }
                g2.dispose();
                setForeground(anim > 0.5f ? new Color(255, 230, 160) : new Color(210, 190, 150));
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setPreferredSize(new Dimension(188, 44));
        b.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 10));
        if (action != null) b.addActionListener(action);
        return b;
    }

    // ── Botão de TopBar ───────────────────────────────────────────────────────

    public static JButton topNavButton(String text, ActionListener action) {
        JButton b = new JButton(text) {
            float anim = 0; boolean hov = false;
            float ripR = 0, ripA = 0; int rx, ry;
            {
                setOpaque(false); setContentAreaFilled(false); setFocusPainted(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hov=true;  startA(); }
                    @Override public void mouseExited(MouseEvent e)  { hov=false; startA(); }
                    @Override public void mousePressed(MouseEvent e) {
                        rx=e.getX(); ry=e.getY(); ripR=0; ripA=0.25f;
                        Timer t = new Timer(14, ev -> {
                            ripR += getWidth()*0.1f; ripA -= 0.025f;
                            if (ripA <= 0) { ripA=0; ((Timer)ev.getSource()).stop(); }
                            repaint();
                        }); t.start();
                    }
                });
            }
            void startA() {
                Timer t = new Timer(14, null);
                t.addActionListener(e -> {
                    anim += hov ? 0.15f : -0.15f; anim = Math.max(0, Math.min(1, anim)); repaint();
                    if ((hov && anim >= 1) || (!hov && anim <= 0)) ((Timer)e.getSource()).stop();
                }); t.start();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (anim > 0) {
                    g2.setColor(new Color(210, 160, 50, (int)(anim * 50)));
                    g2.fill(new RoundRectangle2D.Float(2, 2, getWidth()-4, getHeight()-4, 8, 8));
                }
                if (anim > 0) {
                    int bw = (int)(anim * (getWidth() - 12));
                    g2.setColor(new Color(210, 160, 50, (int)(anim * 220)));
                    g2.fillRoundRect((getWidth()-bw)/2, getHeight()-3, bw, 3, 3, 3);
                }
                if (ripA > 0) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ripA));
                    g2.setColor(new Color(255, 220, 100));
                    float d = ripR * 2;
                    g2.fill(new Ellipse2D.Float(rx-ripR, ry-ripR, d, d));
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setForeground(new Color(230, 210, 170));
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        if (action != null) b.addActionListener(action);
        return b;
    }

    // ── Tabela ────────────────────────────────────────────────────────────────

    public static JTable styledTable() {
        JTable t = new JTable();
        t.setFont(FONT_NORMAL);
        t.setBackground(W11_SURFACE);
        t.setForeground(W11_TEXT);
        t.setGridColor(new Color(220, 205, 175));
        t.setSelectionBackground(W11_TABLE_SEL);
        t.setSelectionForeground(W11_TABLE_SEL_FG);
        t.setRowHeight(28);
        t.setShowGrid(true);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setFocusable(true);
        DefaultTableCellRenderer hdr = new DefaultTableCellRenderer();
        hdr.setBackground(new Color(240, 228, 195));
        hdr.setForeground(W11_ACCENT_DARK);
        hdr.setFont(FONT_BOLD);
        hdr.setHorizontalAlignment(SwingConstants.LEFT);
        hdr.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, W11_ACCENT),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        t.getTableHeader().setDefaultRenderer(hdr);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setBackground(new Color(240, 228, 195));
        t.getTableHeader().setForeground(W11_ACCENT_DARK);
        t.getTableHeader().setFont(FONT_BOLD);
        t.getTableHeader().setPreferredSize(new Dimension(0, 34));
        return t;
    }

    // ── Bordas / painéis ──────────────────────────────────────────────────────

    public static Border groupBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(W11_BORDER, 1),
                title, TitledBorder.LEFT, TitledBorder.TOP, FONT_BOLD, W11_ACCENT);
    }

    public static JPanel xpTitleBar(String text) {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(245, 235, 205),
                        getWidth(), 0, new Color(235, 220, 180)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, W11_BORDER));
        bar.setPreferredSize(new Dimension(0, 42));
        JLabel lbl = new JLabel("  " + text);
        lbl.setForeground(W11_ACCENT_DARK);
        lbl.setFont(FONT_LARGE);
        bar.add(lbl, BorderLayout.WEST);
        return bar;
    }

    public static JPanel summaryCard(String title, String icon, ActionListener a) {
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            float hov = 0;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { animate(true); }
                    @Override public void mouseExited(MouseEvent e)  { animate(false); }
                });
            }
            void animate(boolean in) {
                Timer t = new Timer(16, null);
                t.addActionListener(e -> {
                    hov += in ? 0.1f : -0.1f; hov = Math.max(0, Math.min(1, hov)); repaint();
                    if ((in && hov >= 1) || (!in && hov <= 0)) ((Timer)e.getSource()).stop();
                }); t.start();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Sombra suave
                g2.setColor(new Color(180, 150, 80, 40));
                g2.fill(new RoundRectangle2D.Float(3, 5, getWidth()-4, getHeight()-4, 14, 14));
                // Fundo do card
                Color c1 = interpolate(W11_SURFACE, new Color(255, 245, 215), hov);
                Color c2 = interpolate(W11_SURFACE2, new Color(245, 230, 185), hov);
                g2.setPaint(new GradientPaint(0, 0, c1, 0, getHeight(), c2));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-2, getHeight()-2, 14, 14));
                // Borda
                Color border = interpolate(W11_BORDER, W11_ACCENT, hov * 0.7f);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth()-2, getHeight()-2, 14, 14));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JLabel lbl = new JLabel(icon + "  " + title, SwingConstants.CENTER);
        lbl.setForeground(W11_ACCENT); lbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        card.add(lbl, BorderLayout.CENTER);
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bp.setOpaque(false); bp.add(primaryButton("Abrir", a));
        card.add(bp, BorderLayout.SOUTH);
        return card;
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    public static void enterActsAsTab(JComponent c) {
        c.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) { e.consume(); c.transferFocus(); }
            }
        });
    }

    public static Color interpolate(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                (int)(a.getRed()   + t * (b.getRed()   - a.getRed())),
                (int)(a.getGreen() + t * (b.getGreen() - a.getGreen())),
                (int)(a.getBlue()  + t * (b.getBlue()  - a.getBlue())));
    }
}