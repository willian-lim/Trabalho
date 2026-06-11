package br.carmel.ui;

import br.carmel.model.Usuario;
import br.carmel.util.LicencaManager;
import br.carmel.util.LicencaManager.ResultadoLicenca;
import br.carmel.util.UIFactory;
import br.carmel.util.Validator;

import jakarta.persistence.EntityManagerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;

public class LoginScreen extends JFrame {

    private static final DateTimeFormatter FMT_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EntityManagerFactory emf;
    private final br.carmel.service.ServiceLocator services;

    public interface OnLoginSuccess {
        void onSuccess(Usuario usuario);
    }

    private final OnLoginSuccess onSuccess;

    public LoginScreen(EntityManagerFactory emf,
                       br.carmel.service.ServiceLocator services,
                       OnLoginSuccess onSuccess) {
        super("Carmel Sistema - Login");
        this.emf      = emf;
        this.services = services;
        this.onSuccess = onSuccess;

        try {
            java.net.URL iconUrl = getClass().getResource("/icone.png");
            if (iconUrl != null)
                setIconImage(new javax.swing.ImageIcon(iconUrl).getImage());
        } catch (Exception ignored) {}

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(420, 500);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        build();
        setVisible(true);
    }

    private void build() {
        JPanel fundo = new JPanel(new BorderLayout()) {
            private java.awt.image.BufferedImage bgImage;
            {
                try {
                    java.net.URL url = getClass().getResource("/fundo_login.jpg");
                    if (url != null) bgImage = javax.imageio.ImageIO.read(url);
                } catch (Exception ignored) {}
            }
            @Override protected void paintComponent(Graphics g) {
                if (bgImage != null) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(new Color(14, 14, 20));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, UIFactory.W11_ACCENT_DARK,
                        getWidth(), 0, new Color(14, 14, 20)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setPreferredSize(new Dimension(0, 70));
        JLabel logo = new JLabel("  CARMEL SISTEMA");
        logo.setFont(new Font("Tahoma", Font.BOLD, 22));
        logo.setForeground(Color.WHITE);
        JLabel sub = new JLabel("  Acesso ao Sistema");
        sub.setFont(new Font("Tahoma", Font.PLAIN, 11));
        sub.setForeground(new Color(140, 175, 220));
        JPanel headerText = new JPanel(new GridLayout(2, 1));
        headerText.setOpaque(false);
        headerText.add(logo); headerText.add(sub);
        header.add(headerText, BorderLayout.CENTER);
        fundo.add(header, BorderLayout.NORTH);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(new Color(28, 28, 38, 245));
        card.setOpaque(true);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIFactory.W11_ACCENT, 2),
                new EmptyBorder(20, 24, 20, 24)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; c.gridwidth = 1; c.weightx = 0;
        card.add(UIFactory.labelLight("Login:"), c);
        JTextField tfLogin = UIFactory.styledField("");
        tfLogin.setPreferredSize(new Dimension(200, 24));
        c.gridx = 1; c.weightx = 1.0; card.add(tfLogin, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        card.add(UIFactory.labelLight("Senha:"), c);
        JPasswordField tfSenha = UIFactory.styledPasswordField("");
        tfSenha.setPreferredSize(new Dimension(200, 24));
        c.gridx = 1; c.weightx = 1.0; card.add(tfSenha, c);

        JButton btnEntrar = UIFactory.bigActionButton("Entrar", e ->
                realizarLogin(tfLogin.getText().trim(), new String(tfSenha.getPassword())));
        btnEntrar.setBackground(new Color(30, 150, 60));
        btnEntrar.setForeground(Color.WHITE);
        btnEntrar.setPreferredSize(new Dimension(200, 36));
        btnEntrar.setFont(new Font("Tahoma", Font.BOLD, 13));
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.insets = new Insets(14, 4, 4, 4);
        card.add(btnEntrar, c);

        c.gridy = 3; c.insets = new Insets(10, 0, 10, 0);
        card.add(new JSeparator(), c);

        JPanel gestao = new JPanel(new GridLayout(1, 3, 6, 0));
        gestao.setOpaque(false);
        JButton btnCadastrar    = UIFactory.bigActionButton("Novo Usuário",    ev -> dlgCadastrarUsuario());
        JButton btnAlterarSenha = UIFactory.bigActionButton("Alterar Senha",   ev -> dlgAlterarSenha());
        JButton btnExcluir      = UIFactory.bigActionButton("Excluir Usuário", ev -> dlgExcluirUsuario());
        gestao.add(btnCadastrar); gestao.add(btnAlterarSenha); gestao.add(btnExcluir);
        c.gridy = 4; c.insets = new Insets(0, 4, 4, 4);
        card.add(gestao, c);

        c.gridy = 5; c.insets = new Insets(10, 0, 6, 0);
        card.add(new JSeparator(), c);

        JButton btnLicenca = UIFactory.bigActionButton("🔑  Licença do Sistema", ev -> dlgLicenca());
        btnLicenca.setBackground(new Color(50, 50, 80));
        btnLicenca.setForeground(new Color(160, 200, 255));
        btnLicenca.setFont(new Font("Tahoma", Font.PLAIN, 11));
        c.gridy = 6; c.insets = new Insets(0, 4, 4, 4);
        card.add(btnLicenca, c);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(card, new GridBagConstraints());
        fundo.add(wrapper, BorderLayout.CENTER);

        JLabel rodape = new JLabel("v1.0 • Carmel Sistema de Gestão", SwingConstants.CENTER);
        rodape.setFont(new Font("Tahoma", Font.PLAIN, 9));
        rodape.setForeground(Color.WHITE);
        rodape.setOpaque(false);
        rodape.setBorder(new EmptyBorder(4, 0, 4, 0));
        fundo.add(rodape, BorderLayout.SOUTH);

        add(fundo);

        KeyAdapter enterLogin = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    realizarLogin(tfLogin.getText().trim(), new String(tfSenha.getPassword()));
            }
        };
        tfLogin.addKeyListener(enterLogin);
        tfSenha.addKeyListener(enterLogin);
    }

    private void realizarLogin(String login, String senha) {
        if (Validator.isBlank(login)) { showAviso("Informe o login."); return; }
        if (Validator.isBlank(senha)) { showAviso("Informe a senha."); return; }
        try {
            Usuario u = services.usuarios().autenticar(login, senha);
            dispose();
            onSuccess.onSuccess(u);
        } catch (br.carmel.service.RegraNegocioException e) {
            showErro(e.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            showErro("Erro ao autenticar: " + ex.getMessage());
        }
    }

    

    private void dlgLicenca() {
        ResultadoLicenca r = LicencaManager.verificar();

        JDialog dlg = new JDialog(this, "Licença do Sistema", true);
        dlg.setSize(430, 320);
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        
        Color corHeader = switch (r.status()) {
            case VALIDA                                    -> new Color(20, 100, 40);
            case SEM_LICENCA, EXPIRADA, CHAVE_JA_UTILIZADA -> new Color(160, 30, 30);
            case CORROMPIDA                                -> new Color(120, 60, 0);
        };
        String iconeStatus = switch (r.status()) {
            case VALIDA                                    -> "✔";
            case SEM_LICENCA, EXPIRADA, CHAVE_JA_UTILIZADA -> "✘";
            case CORROMPIDA                                -> "⚠";
        };
        String tituloStatus = switch (r.status()) {
            case VALIDA              -> "Licença Válida";
            case SEM_LICENCA        -> "Não Ativado";
            case EXPIRADA           -> "Licença Expirada";
            case CORROMPIDA         -> "Licença Inválida";
            case CHAVE_JA_UTILIZADA -> "Chave Já Utilizada";
        };

        JPanel headerDlg = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(corHeader);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        JLabel lblTitulo = new JLabel(iconeStatus + "  " + tituloStatus);
        lblTitulo.setFont(new Font("Tahoma", Font.BOLD, 16));
        lblTitulo.setForeground(Color.WHITE);
        headerDlg.add(lblTitulo);
        dlg.add(headerDlg, BorderLayout.NORTH);

        
        JPanel corpo = new JPanel(new GridBagLayout());
        corpo.setBackground(new Color(28, 28, 38));
        corpo.setBorder(new EmptyBorder(16, 20, 8, 20));
        GridBagConstraints c = gbc();

        
        String dataInst = r.dataInicio() != null ? r.dataInicio().format(FMT_BR) : "-";
        String dataExp  = r.dataInicio() != null
                ? r.dataInicio().plusDays(LicencaManager.DIAS_VALIDADE).format(FMT_BR) : "-";

        infoRow(corpo, c, 0, "Status:",        tituloStatus);
        infoRow(corpo, c, 1, "Instalação:",    dataInst);
        infoRow(corpo, c, 2, "Expira em:",     dataExp);
        infoRow(corpo, c, 3, "Dias restantes:",
                (r.status() == LicencaManager.Status.EXPIRADA
                        || r.status() == LicencaManager.Status.SEM_LICENCA
                        || r.status() == LicencaManager.Status.CHAVE_JA_UTILIZADA)
                        ? "—" : r.diasRestantes() + " dia(s)");

        c.gridx = 0; c.gridy = 4; c.gridwidth = 2; c.insets = new Insets(12, 0, 6, 0);
        corpo.add(new JSeparator(), c);

        JLabel lblChave = new JLabel("Chave de renovação:");
        lblChave.setForeground(new Color(160, 200, 255));
        lblChave.setFont(new Font("Tahoma", Font.BOLD, 11));
        c.gridy = 5; c.insets = new Insets(0, 0, 4, 0);
        corpo.add(lblChave, c);

        JTextArea tfChave = new JTextArea(2, 30);
        tfChave.setFont(new Font("Monospaced", Font.PLAIN, 11));
        tfChave.setBackground(new Color(20, 20, 30));
        tfChave.setForeground(new Color(100, 220, 100));
        tfChave.setCaretColor(Color.WHITE);
        tfChave.setBorder(BorderFactory.createLineBorder(new Color(60, 120, 200), 1));
        tfChave.setLineWrap(true);
        tfChave.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(tfChave);
        scroll.setPreferredSize(new Dimension(370, 46));
        c.gridy = 6; c.insets = new Insets(0, 0, 8, 0);
        corpo.add(scroll, c);

        dlg.add(corpo, BorderLayout.CENTER);

        
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setBackground(new Color(22, 22, 30));

        JButton btnRenovar = new JButton("Ativar Chave");
        btnRenovar.setBackground(new Color(30, 130, 60));
        btnRenovar.setForeground(Color.WHITE);
        btnRenovar.setFocusPainted(false);
        btnRenovar.setFont(new Font("Tahoma", Font.BOLD, 11));
        btnRenovar.addActionListener(e -> {
            String chave = tfChave.getText().trim();
            if (chave.isEmpty()) {
                showAviso("Cole a chave de renovação no campo acima.");
                return;
            }
            try {
                if (LicencaManager.renovar(chave)) {
                    
                    String novaExp = LicencaManager.verificar().dataInicio()
                            .plusDays(LicencaManager.DIAS_VALIDADE).format(FMT_BR);
                    JOptionPane.showMessageDialog(dlg,
                            "Licença renovada com sucesso!\nNova validade: " + novaExp,
                            "Ativado", JOptionPane.INFORMATION_MESSAGE);
                    dlg.dispose();
                } else {
                    JOptionPane.showMessageDialog(dlg,
                            "Chave inválida. Verifique e tente novamente.",
                            "Erro", JOptionPane.ERROR_MESSAGE);
                }
            } catch (LicencaManager.ChaveJaUtilizadaException ex) {
                JOptionPane.showMessageDialog(dlg,
                        "⚠  Esta chave já foi utilizada anteriormente.\n\n" +
                        "Cada chave só pode ser ativada uma única vez.\n" +
                        "Solicite uma nova chave ao suporte.",
                        "Chave Já Utilizada", JOptionPane.WARNING_MESSAGE);
            }
        });

        JButton btnFechar = new JButton("Fechar");
        btnFechar.setFocusPainted(false);
        btnFechar.addActionListener(e -> dlg.dispose());

        btns.add(btnRenovar);
        btns.add(btnFechar);
        dlg.add(btns, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void infoRow(JPanel p, GridBagConstraints c, int row, String label, String valor) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        c.insets = new Insets(3, 0, 3, 12);
        JLabel lbl = new JLabel(label);
        lbl.setForeground(new Color(160, 160, 180));
        lbl.setFont(new Font("Tahoma", Font.BOLD, 11));
        p.add(lbl, c);

        c.gridx = 1; c.weightx = 1.0; c.insets = new Insets(3, 0, 3, 0);
        JLabel val = new JLabel(valor);
        val.setForeground(new Color(220, 220, 230));
        val.setFont(new Font("Tahoma", Font.PLAIN, 11));
        p.add(val, c);
    }

    

    private void dlgCadastrarUsuario() {
        JDialog dlg = new JDialog(this, "Novo Usuário", true);
        dlg.setSize(360, 290);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(new EmptyBorder(14, 16, 8, 16));
        GridBagConstraints c = gbc();

        JPasswordField tfAdmin  = UIFactory.styledPasswordField("");
        JTextField tfNome       = UIFactory.styledField("");
        JTextField tfLoginField = UIFactory.styledField("");
        JPasswordField tfSenha  = UIFactory.styledPasswordField("");
        JPasswordField tfConf   = UIFactory.styledPasswordField("");

        row(form, c, 0, "Senha do administrador:", tfAdmin);
        row(form, c, 1, "Nome completo:",           tfNome);
        row(form, c, 2, "Login:",                   tfLoginField);
        row(form, c, 3, "Senha:",                   tfSenha);
        row(form, c, 4, "Confirmar senha:",          tfConf);
        dlg.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setBackground(UIFactory.XP_BG);
        boolean[] ok = {false};
        JButton btnOk = UIFactory.bigActionButton("Cadastrar", e -> { ok[0] = true; dlg.dispose(); });
        btns.add(btnOk); btns.add(UIFactory.bigActionButton("Cancelar", e -> dlg.dispose()));
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.getRootPane().setDefaultButton(btnOk);
        SwingUtilities.invokeLater(tfAdmin::requestFocusInWindow);
        dlg.setVisible(true);
        if (!ok[0]) return;

        try {
            services.usuarios().cadastrar(
                    new String(tfAdmin.getPassword()),
                    tfNome.getText().trim(),
                    tfLoginField.getText().trim().toLowerCase(),
                    new String(tfSenha.getPassword()),
                    new String(tfConf.getPassword()));
            JOptionPane.showMessageDialog(this, "Usuário cadastrado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (br.carmel.service.RegraNegocioException e) {
            showErro(e.getMessage());
        } catch (Exception ex) {
            showErro("Erro ao cadastrar: " + ex.getMessage());
        }
    }

    private void dlgAlterarSenha() {
        JDialog dlg = new JDialog(this, "Alterar Senha", true);
        dlg.setSize(340, 230);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(new EmptyBorder(14, 16, 8, 16));
        GridBagConstraints c = gbc();

        JTextField     tfLoginField = UIFactory.styledField("");
        JPasswordField tfAtual      = UIFactory.styledPasswordField("");
        JPasswordField tfNova       = UIFactory.styledPasswordField("");
        JPasswordField tfConf       = UIFactory.styledPasswordField("");

        row(form, c, 0, "Login:",          tfLoginField);
        row(form, c, 1, "Senha atual:",    tfAtual);
        row(form, c, 2, "Nova senha:",     tfNova);
        row(form, c, 3, "Confirmar nova:", tfConf);
        dlg.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setBackground(UIFactory.XP_BG);
        boolean[] ok = {false};
        JButton btnOk = UIFactory.bigActionButton("Alterar", e -> { ok[0] = true; dlg.dispose(); });
        btns.add(btnOk); btns.add(UIFactory.bigActionButton("Cancelar", e -> dlg.dispose()));
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.getRootPane().setDefaultButton(btnOk);
        SwingUtilities.invokeLater(tfLoginField::requestFocusInWindow);
        dlg.setVisible(true);
        if (!ok[0]) return;

        try {
            services.usuarios().alterarSenha(
                    tfLoginField.getText().trim().toLowerCase(),
                    new String(tfAtual.getPassword()),
                    new String(tfNova.getPassword()),
                    new String(tfConf.getPassword()));
            JOptionPane.showMessageDialog(this, "Senha alterada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (br.carmel.service.RegraNegocioException e) {
            showErro(e.getMessage());
        } catch (Exception ex) {
            showErro("Erro ao alterar senha: " + ex.getMessage());
        }
    }

    private void dlgExcluirUsuario() {
        java.util.List<Usuario> usuarios = services.usuarios().listarTodos();
        if (usuarios.isEmpty()) { showAviso("Nenhum usuário cadastrado."); return; }

        JDialog dlg = new JDialog(this, "Excluir Usuário", true);
        dlg.setSize(400, 310);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(new EmptyBorder(12, 14, 8, 14));
        GridBagConstraints c = gbc();

        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Login", "Nome"}, 0) {
            public boolean isCellEditable(int r, int col) { return false; }
        };
        for (Usuario u : usuarios) model.addRow(new Object[]{ u.getId(), u.getLogin(), u.getNomeCompleto() });
        JTable table = UIFactory.styledTable();
        table.setModel(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(350, 110));

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.insets = new Insets(0, 0, 4, 0);
        form.add(UIFactory.labelLight("Selecione o usuário a excluir:"), c);
        c.gridy = 1; c.insets = new Insets(0, 0, 10, 0);
        form.add(scroll, c);

        JPasswordField tfAdmin = UIFactory.styledPasswordField("");
        c.insets = new Insets(4, 4, 4, 4);
        row(form, c, 2, "Senha do administrador:", tfAdmin);
        dlg.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setBackground(UIFactory.XP_BG);
        boolean[] ok = {false};
        JButton btnOk = UIFactory.bigActionButton("Excluir", e -> { ok[0] = true; dlg.dispose(); });
        btnOk.setBackground(new Color(180, 30, 30)); btnOk.setForeground(Color.WHITE);
        btns.add(btnOk); btns.add(UIFactory.bigActionButton("Cancelar", e -> dlg.dispose()));
        dlg.add(btns, BorderLayout.SOUTH);
        SwingUtilities.invokeLater(tfAdmin::requestFocusInWindow);
        dlg.setVisible(true);
        if (!ok[0]) return;

        int row = table.getSelectedRow();
        if (row < 0) { showAviso("Selecione um usuário na tabela."); return; }

        Long idExcluir      = (Long)   model.getValueAt(row, 0);
        String loginExcluir = (String) model.getValueAt(row, 1);

        int conf = JOptionPane.showConfirmDialog(this,
                "Confirma a exclusão do usuário \"" + loginExcluir + "\"?",
                "Confirmar Exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION) return;

        try {
            services.usuarios().excluir(idExcluir, new String(tfAdmin.getPassword()));
            JOptionPane.showMessageDialog(this, "Usuário \"" + loginExcluir + "\" excluído.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (br.carmel.service.RegraNegocioException e) {
            showErro(e.getMessage());
        } catch (Exception ex) {
            showErro("Erro ao excluir: " + ex.getMessage());
        }
    }

    

    private GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    private void row(JPanel p, GridBagConstraints c, int rowIdx, String label, JComponent field) {
        c.gridx = 0; c.gridy = rowIdx; c.gridwidth = 1; c.weightx = 0;
        p.add(UIFactory.labelLight(label), c);
        c.gridx = 1; c.weightx = 1.0;
        field.setPreferredSize(new Dimension(180, 24));
        p.add(field, c);
    }

    private void showErro(String msg)  { JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE); }
    private void showAviso(String msg) { JOptionPane.showMessageDialog(this, msg, "Atenção", JOptionPane.WARNING_MESSAGE); }
}