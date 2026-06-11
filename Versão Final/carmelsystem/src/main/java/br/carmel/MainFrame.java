package br.carmel;

import br.carmel.model.Usuario;
import br.carmel.service.ServiceLocator;
import br.carmel.ui.LicencaDialog;
import br.carmel.ui.LoginScreen;
import br.carmel.ui.SideMenu;
import br.carmel.ui.panels.*;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class MainFrame extends JFrame {

    private static final String PU_NAME = "dadoscarmelPU";

    private final EntityManagerFactory emf;
    private final ServiceLocator services;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel pnlCards = new JPanel(cardLayout);

    private PedidoPanel  pedidoPanel;
    private ClientePanel clientePanel;
    private ProdutoPanel produtoPanel;
    private CaixaPanel   caixaPanel;
    private RelatorioEstoquePanel  relatorioEstoquePanel;
    private NotaTransferenciaPanel notaTransferenciaPanel;
    private EmitirNotaPanel        emitirNotaPanel;
    private ConsultasPanel         consultasPanel;

    private Usuario usuarioAtual;
    private JLabel  statusBar;

    private MainFrame(EntityManagerFactory emf, ServiceLocator services, Usuario usuario) {
        super("Carmel Sistema de Gestão");
        this.emf          = emf;
        this.services     = services;
        this.usuarioAtual = usuario;

        try {
            java.net.URL iconUrl = getClass().getResource("/icone.png");
            if (iconUrl != null) setIconImage(new ImageIcon(iconUrl).getImage());
        } catch (Exception ignored) {}

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 760);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildMenu(),      BorderLayout.NORTH);
        add(buildCards(),     BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        cardLayout.show(pnlCards, "home");
        setVisible(true);
        toFront(); requestFocus(); setState(JFrame.NORMAL);
    }

    private JLabel buildStatusBar() {
        statusBar = new JLabel("  Usuário: " + usuarioAtual.getNomeCompleto()
                + " (" + usuarioAtual.getLogin() + ")  |  Carmel Sistema v1.0");
        statusBar.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        statusBar.setForeground(new Color(100, 80, 45));
        statusBar.setBackground(new Color(238, 228, 200));
        statusBar.setOpaque(true);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 180, 130)),
                new EmptyBorder(3, 8, 3, 8)));
        return statusBar;
    }

    private static void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            
            Color bg      = new Color(250, 246, 235);
            Color surface = new Color(255, 252, 244);
            Color text    = new Color(55, 40, 20);
            Color textSec = new Color(130, 110, 80);
            Color border  = new Color(210, 195, 165);

            UIManager.put("Panel.background",          bg);
            UIManager.put("OptionPane.background",     bg);
            UIManager.put("Button.background",         new Color(242, 234, 210));
            UIManager.put("Button.foreground",         text);
            UIManager.put("TextField.background",      surface);
            UIManager.put("TextField.foreground",      text);
            UIManager.put("TextArea.background",       surface);
            UIManager.put("TextArea.foreground",       text);
            UIManager.put("ComboBox.background",       surface);
            UIManager.put("ComboBox.foreground",       text);
            UIManager.put("Table.background",          surface);
            UIManager.put("Table.foreground",          text);
            UIManager.put("Table.gridColor",           border);
            UIManager.put("ScrollPane.background",     bg);
            UIManager.put("SplitPane.background",      bg);
            UIManager.put("Viewport.background",       bg);
            UIManager.put("Label.foreground",          text);
            UIManager.put("OptionPane.messageForeground", text);
            UIManager.put("OptionPane.foreground",     text);
            UIManager.put("TitledBorder.titleColor",   new Color(130, 90, 20));
            UIManager.put("ScrollBar.thumb",           new Color(200, 175, 120));
            UIManager.put("ScrollBar.track",           new Color(240, 230, 205));
        } catch (Exception e) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
        }
    }

    private SideMenu buildMenu() {
        SideMenu menu = new SideMenu(new SideMenu.NavActions() {
            public void goHome()       { showCard("home"); }
            public void goClientes()   { clientePanel.reloadTable(); showCard("cliente"); }
            public void goProdutos()   { produtoPanel.reloadTable(); showCard("produto"); }
            public void goPedidos()    { pedidoPanel.reloadCombos(); pedidoPanel.reloadPedidos(); showCard("pedido"); }
            public void goCaixa()      { caixaPanel.reloadCaixa(); showCard("caixa"); }
            public void goConsultas()  { consultasPanel.reloadConsultas(); showCard("consultas"); }
            public void goEstoque()    { relatorioEstoquePanel.carregarDados(false); showCard("estoque"); }
            public void goNotas()      { notaTransferenciaPanel.carregarFornecedores(); showCard("notas"); }
            public void goEmitirNota() { emitirNotaPanel.reloadPendentes(); showCard("emitirNota"); }
            public void logout()       { fazerLogout(); }
        });
        menu.setUsuario(usuarioAtual.getNomeCompleto());
        return menu;
    }

    private void fazerLogout() {
        int conf = JOptionPane.showConfirmDialog(this,
                "Deseja sair do sistema?", "Logout", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;
        dispose();
        new LoginScreen(emf, services, usuario -> new MainFrame(emf, services, usuario));
    }

    private JPanel buildCards() {
        pnlCards.setBackground(new Color(250, 246, 235));

        HomePanel homePanel               = new HomePanel(services);
        clientePanel                      = new ClientePanel(services);
        produtoPanel                      = new ProdutoPanel(services);
        pedidoPanel                       = new PedidoPanel(services);
        caixaPanel                        = new CaixaPanel(services);
        relatorioEstoquePanel             = new RelatorioEstoquePanel(services);
        notaTransferenciaPanel            = new NotaTransferenciaPanel(services);
        emitirNotaPanel                   = new EmitirNotaPanel(services);
        consultasPanel                    = new ConsultasPanel(services);

        pedidoPanel.setCaixaPanel(caixaPanel);
        emitirNotaPanel.setCaixaPanel(caixaPanel);

        pnlCards.add(homePanel,              "home");
        pnlCards.add(clientePanel,           "cliente");
        pnlCards.add(produtoPanel,           "produto");
        pnlCards.add(pedidoPanel,            "pedido");
        pnlCards.add(caixaPanel,             "caixa");
        pnlCards.add(consultasPanel,         "consultas");
        pnlCards.add(relatorioEstoquePanel,  "estoque");
        pnlCards.add(notaTransferenciaPanel, "notas");
        pnlCards.add(emitirNotaPanel,        "emitirNota");

        return pnlCards;
    }

    private void showCard(String name) { cardLayout.show(pnlCards, name); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            applyLookAndFeel();

            boolean licencaOk = LicencaDialog.verificarEExibir(null);
            if (!licencaOk) { System.exit(0); return; }

            EntityManagerFactory emf;
            try {
                emf = Persistence.createEntityManagerFactory(PU_NAME);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Erro ao inicializar banco de dados:\n" + e.getMessage(),
                        "Erro Crítico", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                System.exit(1);
                return;
            }

            ServiceLocator services = new ServiceLocator(emf);
            services.usuarios().criarUsuarioPadrao();
            new LoginScreen(emf, services, usuario -> new MainFrame(emf, services, usuario));
        });
    }
}