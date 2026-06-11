package br.carmel.ui;

import br.carmel.util.UIFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class SideMenu extends JPanel {

    public interface NavActions {
        void goHome();
        void goClientes();
        void goProdutos();
        void goPedidos();
        void goCaixa();
        void goConsultas();
        void goEstoque();
        void goNotas();
        void goEmitirNota();
        void logout();
    }

    private JLabel userLabel;

    public SideMenu(NavActions nav) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 96));
        setOpaque(false);
        build(nav);
    }

    public void setUsuario(String nome) {
        if (userLabel != null) userLabel.setText(nome);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2.setPaint(new GradientPaint(0, 0, new Color(52, 38, 18),
                getWidth(), 0, new Color(68, 50, 24)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        g2.setColor(new Color(160, 110, 30, 180));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
        g2.dispose();
    }

    private void build(NavActions nav) {
        
        JPanel topRow = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(44, 32, 14),
                        getWidth(), 0, new Color(60, 44, 20)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(120, 85, 30, 120));
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        topRow.setOpaque(false);
        topRow.setPreferredSize(new Dimension(0, 48));
        topRow.setBorder(new EmptyBorder(0, 16, 0, 16));

        
        JLabel logo = new JLabel("⬡  CARMEL") {
            float pulse = 0; boolean up = true;
            {
                Timer t = new Timer(40, e -> {
                    pulse += up ? 0.03f : -0.03f;
                    if (pulse >= 1) up = false;
                    else if (pulse <= 0) up = true;
                    repaint();
                });
                t.start();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                Color glow = new Color(200, 140, 40, (int)(30 + pulse * 50));
                g2.setColor(glow);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = getInsets().left;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                for (int dx = -2; dx <= 2; dx++)
                    for (int dy = -2; dy <= 2; dy++)
                        if (dx != 0 || dy != 0)
                            g2.drawString(getText(), tx+dx, ty+dy);
                
                g2.setColor(new Color(
                        (int)(210 + pulse * 30),
                        (int)(160 + pulse * 30),
                        (int)(60  + pulse * 20)));
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logo.setForeground(UIFactory.W11_ACCENT);
        logo.setBorder(new EmptyBorder(0, 0, 0, 24));

        JLabel sub = new JLabel("Sistema de Gestão");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        sub.setForeground(new Color(160, 130, 80));

        JPanel logoBox = new JPanel(new BorderLayout(0, 2));
        logoBox.setOpaque(false);
        logoBox.add(logo, BorderLayout.NORTH);
        logoBox.add(sub, BorderLayout.SOUTH);

        
        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBox.setOpaque(false);

        JLabel userIcon = new JLabel("👤");
        userIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));

        userLabel = new JLabel("");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        userLabel.setForeground(new Color(210, 185, 140));

        JButton btnSair = UIFactory.topNavButton("⏻  Sair", e -> nav.logout());
        btnSair.setForeground(new Color(220, 100, 80));
        btnSair.setFont(new Font("Segoe UI", Font.BOLD, 11));

        rightBox.add(userIcon);
        rightBox.add(userLabel);
        rightBox.add(Box.createHorizontalStrut(8));
        rightBox.add(btnSair);

        topRow.add(logoBox,  BorderLayout.WEST);
        topRow.add(rightBox, BorderLayout.EAST);

        
        JPanel navRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        navRow.setOpaque(false);
        navRow.setPreferredSize(new Dimension(0, 48));
        navRow.setBorder(new EmptyBorder(4, 12, 4, 12));

        navRow.add(navBtn("🏠  Painel",         e -> nav.goHome()));
        navRow.add(navBtn("👥  Clientes",        e -> nav.goClientes()));
        navRow.add(navBtn("📦  Produtos",        e -> nav.goProdutos()));
        navRow.add(navBtn("🛒  Pedidos",         e -> nav.goPedidos()));
        navRow.add(navBtn("💰  Caixa",           e -> nav.goCaixa()));
        navRow.add(navBtn("🔍  Consultas",       e -> nav.goConsultas()));
        navRow.add(navBtn("📊  Est. Estoque",    e -> nav.goEstoque()));
        navRow.add(navBtn("🔄  Transferências",  e -> nav.goNotas()));
        navRow.add(navBtn("🧾  Emitir Nota",     e -> nav.goEmitirNota()));

        add(topRow,  BorderLayout.NORTH);
        add(navRow,  BorderLayout.CENTER);
    }

    private JButton navBtn(String text, ActionListener a) {
        return UIFactory.topNavButton(text, a);
    }
}