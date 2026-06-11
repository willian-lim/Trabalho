package br.carmel.ui.panels;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

public class BackgroundPanel extends JPanel {

    private static BufferedImage bgImage;

    static {
        try {
            URL url = BackgroundPanel.class.getResource("/fundo.jpg");
            if (url != null) bgImage = ImageIO.read(url);
        } catch (Exception ignored) {}
    }

    public BackgroundPanel(LayoutManager layout) {
        super(layout);
        setOpaque(true);
    }

    public BackgroundPanel() {
        this(new BorderLayout());
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (bgImage != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(new Color(236, 233, 216));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    
    protected void aplicarFundoTransparente() {
        tornarTransparente(this);
        
        SwingUtilities.invokeLater(() -> tornarTransparente(this));
    }

    
    public static <T extends JPanel> T transparente(T panel) {
        tornarTransparenteEstatico(panel);
        return panel;
    }

    private void tornarTransparente(Container container) {
        tornarTransparenteEstatico(container);
    }

    private static void tornarTransparenteEstatico(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JTable || comp instanceof JTextField
                    || comp instanceof JTextArea || comp instanceof JPasswordField
                    || comp instanceof JComboBox) {
                
            } else if (comp instanceof JScrollPane sp) {
                sp.setOpaque(false);
                sp.getViewport().setOpaque(false);
                tornarTransparenteEstatico(sp.getViewport());
            } else if (comp instanceof JSplitPane sp) {
                sp.setOpaque(false);
                tornarTransparenteEstatico(sp);
            } else if (comp instanceof JTabbedPane tp) {
                tp.setOpaque(false);
                
                for (int i = 0; i < tp.getTabCount(); i++) {
                    Component tabComp = tp.getComponentAt(i);
                    if (tabComp instanceof Container c) tornarTransparenteEstatico(c);
                }
                tornarTransparenteEstatico(tp);
            } else if (comp instanceof JPanel p) {
                p.setOpaque(false);
                tornarTransparenteEstatico(p);
            } else if (comp instanceof Container c) {
                tornarTransparenteEstatico(c);
            }
        }
    }
}