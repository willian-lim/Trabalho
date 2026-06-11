package br.carmel.ui.panels;

import br.carmel.model.Cliente;
import br.carmel.model.ClienteJuridico;
import br.carmel.model.Endereco;
import br.carmel.model.Fornecedor;
import br.carmel.service.RegraNegocioException;
import br.carmel.service.ServiceLocator;
import br.carmel.util.CnpjClient;
import br.carmel.util.UIFactory;
import br.carmel.util.Validator;
import br.carmel.model.viaCepClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel de Cadastro de Clientes e Fornecedores.
 * Toda lógica de negócio delegada ao ClienteService.
 */
public class ClientePanel extends JPanel {

    private final ServiceLocator services;

    // ── Campos PF ─────────────────────────────────────────────────────────────
    private final JTextField tfNome   = UIFactory.styledField("");
    private final JTextField tfCpf    = UIFactory.styledField("");
    private final JTextField tfTel    = UIFactory.styledField("");
    private final JTextField tfEmail  = UIFactory.styledField("");
    private final JTextField tfNumero = UIFactory.styledField("");
    private final JTextField tfCep    = UIFactory.styledField("");
    private final JTextField tfLog    = UIFactory.styledField("");
    private final JTextField tfComp   = UIFactory.styledField("");
    private final JTextField tfBairro = UIFactory.styledField("");
    private final JTextField tfCidade = UIFactory.styledField("");
    private final JTextField tfUf     = UIFactory.styledField("");

    // ── Campos PJ ─────────────────────────────────────────────────────────────
    private final JTextField tfPJRazao    = UIFactory.styledField("");
    private final JTextField tfPJFantasia = UIFactory.styledField("");
    private final JTextField tfPJCnpj     = UIFactory.styledField("");
    private final JTextField tfPJIE       = UIFactory.styledField("");
    private final JTextField tfPJTel      = UIFactory.styledField("");
    private final JTextField tfPJEmail    = UIFactory.styledField("");
    private final JTextField tfPJContato  = UIFactory.styledField("");
    private final JTextField tfPJCep      = UIFactory.styledField("");
    private final JTextField tfPJLog      = UIFactory.styledField("");
    private final JTextField tfPJNumero   = UIFactory.styledField("");
    private final JTextField tfPJBairro   = UIFactory.styledField("");
    private final JTextField tfPJCidade   = UIFactory.styledField("");
    private final JTextField tfPJUf       = UIFactory.styledField("");

    // ── Campos Fornecedor ─────────────────────────────────────────────────────
    private final JTextField tfFRazao    = UIFactory.styledField("");
    private final JTextField tfFFantasia = UIFactory.styledField("");
    private final JTextField tfFCnpj     = UIFactory.styledField("");
    private final JTextField tfFIE       = UIFactory.styledField("");
    private final JTextField tfFTel      = UIFactory.styledField("");
    private final JTextField tfFEmail    = UIFactory.styledField("");
    private final JTextField tfFContato  = UIFactory.styledField("");
    private final JTextField tfFCep      = UIFactory.styledField("");
    private final JTextField tfFLog      = UIFactory.styledField("");
    private final JTextField tfFNumero   = UIFactory.styledField("");
    private final JTextField tfFBairro   = UIFactory.styledField("");
    private final JTextField tfFCidade   = UIFactory.styledField("");
    private final JTextField tfFUf       = UIFactory.styledField("");

    // ── Estado ────────────────────────────────────────────────────────────────
    private Long idPFSel = null, idPJSel = null, idFSel = null;

    // ── Tabelas ───────────────────────────────────────────────────────────────
    private DefaultTableModel pfModel, pjModel, fornModel;
    private JTable pfTable, pjTable, fornTable;
    private JTabbedPane formTabs, tableTabs;

    // ── Campos de busca ───────────────────────────────────────────────────────
    private JTextField tfBuscaPF, tfBuscaPJ, tfBuscaForn;

    public ClientePanel(ServiceLocator services) {
        this.services = services;
        setLayout(new BorderLayout());
        setBackground(UIFactory.XP_BG);
        build();
    }

    // ── Montagem ──────────────────────────────────────────────────────────────

    private void build() {
        add(UIFactory.xpTitleBar("Cadastro de Clientes e Fornecedores"), BorderLayout.NORTH);

        formTabs = new JTabbedPane();
        formTabs.setFont(UIFactory.FONT_BOLD);
        formTabs.setBackground(UIFactory.XP_BG);
        formTabs.addTab("👤 Pessoa Física",   buildFormPF());
        formTabs.addTab("🏢 Pessoa Jurídica", buildFormPJ());
        formTabs.addTab("🏭 Fornecedor",      buildFormFornecedor());
        formTabs.addChangeListener(e -> {
            if (tableTabs != null) tableTabs.setSelectedIndex(formTabs.getSelectedIndex());
        });

        tableTabs = new JTabbedPane();
        tableTabs.setFont(UIFactory.FONT_BOLD);
        tableTabs.addTab("👤 Pessoa Física",   buildTabelaPF());
        tableTabs.addTab("🏢 Pessoa Jurídica", buildTabelaPJ());
        tableTabs.addTab("🏭 Fornecedor",      buildTabelaFornecedor());
        tableTabs.addChangeListener(e -> {
            if (formTabs != null) formTabs.setSelectedIndex(tableTabs.getSelectedIndex());
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formTabs, tableTabs);
        split.setDividerLocation(370);
        split.setDividerSize(5);
        split.setBackground(UIFactory.XP_BG);
        add(split, BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);
    }

    // ── Formulários ───────────────────────────────────────────────────────────

    private JPanel buildFormPF() {
        JPanel p = form("Cliente — Pessoa Física");
        GridBagConstraints c = gbc(); int row = 0;
        addRow(p, c, row++, "Nome:",        tfNome);
        addRow(p, c, row++, "CPF:",         tfCpf);
        addRow(p, c, row++, "Telefone:",    tfTel);
        addRow(p, c, row++, "E-mail:",      tfEmail);
        addRow(p, c, row++, "Número:",      tfNumero);
        addCepRow(p, c, row++, tfCep, tfLog, tfBairro, tfCidade, tfUf);
        addRow(p, c, row++, "Logradouro:",  tfLog);
        addRow(p, c, row++, "Complemento:", tfComp);
        addRow(p, c, row++, "Bairro:",      tfBairro);
        addRow(p, c, row++, "Cidade:",      tfCidade);
        addRow(p, c, row,   "UF:",          tfUf);
        return wrap(p);
    }

    private JPanel buildFormPJ() {
        JPanel p = form("Cliente — Pessoa Jurídica");
        GridBagConstraints c = gbc(); int row = 0;
        addRow(p, c, row++, "Razão Social:",       tfPJRazao);
        addRow(p, c, row++, "Nome Fantasia:",      tfPJFantasia);
        addCnpjRow(p, c, row++, tfPJCnpj, tfPJRazao, tfPJFantasia, tfPJTel, tfPJEmail,
                tfPJCep, tfPJLog, tfPJNumero, tfPJBairro, tfPJCidade, tfPJUf);
        addRow(p, c, row++, "Inscrição Estadual:", tfPJIE);
        addRow(p, c, row++, "Telefone:",           tfPJTel);
        addRow(p, c, row++, "E-mail:",             tfPJEmail);
        addRow(p, c, row++, "Responsável:",        tfPJContato);
        addCepRow(p, c, row++, tfPJCep, tfPJLog, tfPJBairro, tfPJCidade, tfPJUf);
        addRow(p, c, row++, "Logradouro:", tfPJLog);
        addRow(p, c, row++, "Número:",     tfPJNumero);
        addRow(p, c, row++, "Bairro:",     tfPJBairro);
        addRow(p, c, row++, "Cidade:",     tfPJCidade);
        addRow(p, c, row,   "UF:",         tfPJUf);
        return wrap(p);
    }

    private JPanel buildFormFornecedor() {
        JPanel p = form("Fornecedor / Empresa Vendedora");
        GridBagConstraints c = gbc(); int row = 0;
        addRow(p, c, row++, "Razão Social:",       tfFRazao);
        addRow(p, c, row++, "Nome Fantasia:",      tfFFantasia);
        addCnpjRow(p, c, row++, tfFCnpj, tfFRazao, tfFFantasia, tfFTel, tfFEmail,
                tfFCep, tfFLog, tfFNumero, tfFBairro, tfFCidade, tfFUf);
        addRow(p, c, row++, "Inscrição Estadual:", tfFIE);
        addRow(p, c, row++, "Telefone:",           tfFTel);
        addRow(p, c, row++, "E-mail:",             tfFEmail);
        addRow(p, c, row++, "Contato:",            tfFContato);
        addCepRow(p, c, row++, tfFCep, tfFLog, tfFBairro, tfFCidade, tfFUf);
        addRow(p, c, row++, "Logradouro:", tfFLog);
        addRow(p, c, row++, "Número:",     tfFNumero);
        addRow(p, c, row++, "Bairro:",     tfFBairro);
        addRow(p, c, row++, "Cidade:",     tfFCidade);
        addRow(p, c, row,   "UF:",         tfFUf);
        return wrap(p);
    }

    // ── Tabelas com barra de busca ────────────────────────────────────────────

    private JPanel buildTabelaPF() {
        pfModel = tableModel("ID","Nome","CPF","Telefone","E-mail");
        pfTable = UIFactory.styledTable();
        pfTable.setModel(pfModel);
        pfTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        pfTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        pfTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        pfTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        pfTable.getColumnModel().getColumn(4).setPreferredWidth(130);
        pfTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && pfTable.getSelectedRow() >= 0)
                preencherPF((Long) pfModel.getValueAt(pfTable.getSelectedRow(), 0));
        });
        tfBuscaPF = buildBuscaField(txt -> filtrarPF(txt));
        return tabelaComBusca(pfTable, tfBuscaPF, "Nome ou CPF...");
    }

    private JPanel buildTabelaPJ() {
        pjModel = tableModel("ID","Razão Social","CNPJ","Telefone","Cidade");
        pjTable = UIFactory.styledTable();
        pjTable.setModel(pjModel);
        pjTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        pjTable.getColumnModel().getColumn(1).setPreferredWidth(170);
        pjTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        pjTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        pjTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        pjTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && pjTable.getSelectedRow() >= 0)
                preencherPJ((Long) pjModel.getValueAt(pjTable.getSelectedRow(), 0));
        });
        tfBuscaPJ = buildBuscaField(txt -> filtrarPJ(txt));
        return tabelaComBusca(pjTable, tfBuscaPJ, "Razão social, fantasia ou CNPJ...");
    }

    private JPanel buildTabelaFornecedor() {
        fornModel = tableModel("ID","Razão Social","CNPJ","Telefone","Cidade");
        fornTable = UIFactory.styledTable();
        fornTable.setModel(fornModel);
        fornTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        fornTable.getColumnModel().getColumn(1).setPreferredWidth(170);
        fornTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        fornTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        fornTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        fornTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && fornTable.getSelectedRow() >= 0)
                preencherFornecedor((Long) fornModel.getValueAt(fornTable.getSelectedRow(), 0));
        });
        tfBuscaForn = buildBuscaField(txt -> filtrarFornecedor(txt));
        return tabelaComBusca(fornTable, tfBuscaForn, "Razão social, fantasia ou CNPJ...");
    }

    /** Cria o painel de tabela com a barra de busca no topo. */
    private JPanel tabelaComBusca(JTable tabela, JTextField tfBusca, String placeholder) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 4, 8, 8));

        // Barra de busca
        JPanel barBusca = new JPanel(new BorderLayout(6, 0));
        barBusca.setBackground(UIFactory.XP_PANEL_BG);
        barBusca.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(172, 168, 153)),
                new EmptyBorder(5, 6, 5, 6)));

        JLabel ico = new JLabel("🔍");
        ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        ico.setBorder(new EmptyBorder(0, 0, 0, 4));

        // Hint text (placeholder)
        tfBusca.putClientProperty("placeholder", placeholder);
        tfBusca.setFont(UIFactory.FONT_NORMAL);
        tfBusca.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(172, 168, 153)),
                new EmptyBorder(2, 6, 2, 6)));

        JButton btnLimpar = UIFactory.bigSmallButton("✕ Limpar");
        btnLimpar.addActionListener(e -> { tfBusca.setText(""); tfBusca.requestFocusInWindow(); });

        barBusca.add(ico,      BorderLayout.WEST);
        barBusca.add(tfBusca,  BorderLayout.CENTER);
        barBusca.add(btnLimpar, BorderLayout.EAST);
        p.add(barBusca, BorderLayout.NORTH);

        // Contador de resultados
        JLabel lblContador = new JLabel(" ");
        lblContador.setFont(new Font("Tahoma", Font.ITALIC, 10));
        lblContador.setForeground(new Color(100, 100, 100));
        lblContador.setBorder(new EmptyBorder(0, 4, 0, 0));

        // Atualiza contador quando model muda
        tabela.getModel().addTableModelListener(e2 -> {
            int n = tabela.getRowCount();
            lblContador.setText(n == 0 ? "Nenhum resultado." : n + " registro(s)");
        });

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));

        JPanel centro = new JPanel(new BorderLayout(0, 2));
        centro.setOpaque(false);
        centro.add(lblContador, BorderLayout.NORTH);
        centro.add(scroll,      BorderLayout.CENTER);
        p.add(centro, BorderLayout.CENTER);
        return p;
    }

    /** Cria um JTextField que dispara a ação a cada tecla digitada. */
    private JTextField buildBuscaField(java.util.function.Consumer<String> onSearch) {
        JTextField tf = UIFactory.styledField("");
        tf.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { SwingUtilities.invokeLater(() -> onSearch.accept(tf.getText())); }
            public void removeUpdate(DocumentEvent e)  { SwingUtilities.invokeLater(() -> onSearch.accept(tf.getText())); }
            public void changedUpdate(DocumentEvent e) { SwingUtilities.invokeLater(() -> onSearch.accept(tf.getText())); }
        });
        // Enter também aciona busca
        tf.addActionListener(e -> onSearch.accept(tf.getText()));
        return tf;
    }

    // ── Filtros (disparam a busca no service) ─────────────────────────────────

    private void filtrarPF(String termo) {
        pfModel.setRowCount(0);
        idPFSel = null;
        services.clientes().buscarPF(termo).forEach(c ->
                pfModel.addRow(new Object[]{ c.getId(), c.getNome(), c.getCpf(), c.getTelefone(), n(c.getEmail()) }));
    }

    private void filtrarPJ(String termo) {
        pjModel.setRowCount(0);
        idPJSel = null;
        services.clientes().buscarPJ(termo).forEach(c ->
                pjModel.addRow(new Object[]{ c.getId(), c.getRazaoSocial(), n(c.getCnpj()), n(c.getTelefone()), n(c.getCidade()) }));
    }

    private void filtrarFornecedor(String termo) {
        fornModel.setRowCount(0);
        idFSel = null;
        services.clientes().buscarFornecedores(termo).forEach(f ->
                fornModel.addRow(new Object[]{ f.getId(), f.getRazaoSocial(), n(f.getCnpj()), n(f.getTelefone()), n(f.getCidade()) }));
    }

    // ── Barra de botões únicos ────────────────────────────────────────────────

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        bar.setBackground(new Color(212, 208, 200));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));

        JLabel lblTipo = new JLabel("Pessoa Física");
        lblTipo.setFont(UIFactory.FONT_BOLD);
        lblTipo.setForeground(new Color(0, 84, 166));
        lblTipo.setBorder(new EmptyBorder(0, 4, 0, 8));
        bar.add(lblTipo);

        bar.add(UIFactory.bigActionButton("Novo",      e -> novo()));
        bar.add(UIFactory.bigActionButton("Salvar",    e -> salvar()));
        bar.add(UIFactory.bigActionButton("Atualizar", e -> atualizar()));
        bar.add(UIFactory.bigActionButton("Excluir",   e -> excluir()));
        bar.add(new JSeparator(JSeparator.VERTICAL));
        bar.add(UIFactory.bigActionButton("↻ Atualizar Lista", e -> reloadTable()));

        String[] nomes = {"Pessoa Física", "Pessoa Jurídica", "Fornecedor"};
        Color[]  cores  = {new Color(0, 84, 166), new Color(0, 120, 60), new Color(140, 60, 0)};
        formTabs.addChangeListener(e -> {
            int i = formTabs.getSelectedIndex();
            if (i >= 0 && i < nomes.length) {
                lblTipo.setText(nomes[i]);
                lblTipo.setForeground(cores[i]);
            }
        });
        return bar;
    }

    // ── Botões únicos → delegam para a aba ativa ──────────────────────────────

    private void novo() {
        switch (formTabs.getSelectedIndex()) {
            case 0 -> limparPF();
            case 1 -> limparPJ();
            case 2 -> limparFornecedor();
        }
    }

    private void salvar() {
        switch (formTabs.getSelectedIndex()) {
            case 0 -> salvarPF();
            case 1 -> salvarPJ();
            case 2 -> salvarFornecedor();
        }
    }

    private void atualizar() {
        switch (formTabs.getSelectedIndex()) {
            case 0 -> atualizarPF();
            case 1 -> atualizarPJ();
            case 2 -> atualizarFornecedor();
        }
    }

    private void excluir() {
        switch (formTabs.getSelectedIndex()) {
            case 0 -> excluirPF();
            case 1 -> excluirPJ();
            case 2 -> excluirFornecedor();
        }
    }

    // ── Ações PF ──────────────────────────────────────────────────────────────

    private void limparPF() {
        idPFSel = null;
        tfNome.setText(""); tfCpf.setText(""); tfTel.setText(""); tfEmail.setText("");
        tfNumero.setText(""); tfCep.setText(""); tfLog.setText(""); tfComp.setText("");
        tfBairro.setText(""); tfCidade.setText(""); tfUf.setText("");
        pfTable.clearSelection();
        formTabs.setSelectedIndex(0);
    }

    private void salvarPF() {
        try {
            Cliente c = new Cliente();
            c.setNome(tfNome.getText().trim()); c.setCpf(tfCpf.getText().trim());
            c.setTelefone(tfTel.getText().trim()); c.setEmail(tfEmail.getText().trim());
            Endereco e = buildEndereco();
            e.setCliente(c);
            c.setEnderecos(new ArrayList<>(List.of(e)));
            services.clientes().salvarPF(c);
            JOptionPane.showMessageDialog(this, "Cliente (PF) salvo!");
            limparPF(); reloadPF();
        } catch (RegraNegocioException e) { showError(e.getMessage()); }
        catch (Exception ex) { showError("Erro: " + ex.getMessage()); }
    }

    private void atualizarPF() {
        if (idPFSel == null) { JOptionPane.showMessageDialog(this, "Selecione um cliente."); return; }
        try {
            Cliente c = services.clientes().buscarPFPorId(idPFSel)
                    .orElseThrow(() -> new RegraNegocioException("Cliente não encontrado."));
            c.setNome(tfNome.getText().trim()); c.setCpf(tfCpf.getText().trim());
            c.setTelefone(tfTel.getText().trim()); c.setEmail(tfEmail.getText().trim());
            if (c.getEnderecos() == null || c.getEnderecos().isEmpty()) {
                Endereco e = buildEndereco(); e.setCliente(c);
                c.setEnderecos(new ArrayList<>(List.of(e)));
            } else { applyEndereco(c.getEnderecos().get(0)); }
            services.clientes().salvarPF(c);
            JOptionPane.showMessageDialog(this, "Cliente (PF) atualizado.");
            reloadPF();
        } catch (RegraNegocioException e) { showError(e.getMessage()); }
        catch (Exception ex) { showError("Erro: " + ex.getMessage()); }
    }

    private void excluirPF() {
        if (idPFSel == null) { JOptionPane.showMessageDialog(this, "Selecione um cliente."); return; }
        if (confirm("Confirma exclusão?")) {
            try { services.clientes().excluirPF(idPFSel); JOptionPane.showMessageDialog(this, "Cliente removido."); limparPF(); reloadPF(); }
            catch (RegraNegocioException e) { showError(e.getMessage()); }
        }
    }

    // ── Ações PJ ──────────────────────────────────────────────────────────────

    private void limparPJ() {
        idPJSel = null;
        tfPJRazao.setText(""); tfPJFantasia.setText(""); tfPJCnpj.setText(""); tfPJIE.setText("");
        tfPJTel.setText(""); tfPJEmail.setText(""); tfPJContato.setText("");
        tfPJCep.setText(""); tfPJLog.setText(""); tfPJNumero.setText("");
        tfPJBairro.setText(""); tfPJCidade.setText(""); tfPJUf.setText("");
        pjTable.clearSelection(); formTabs.setSelectedIndex(1);
    }

    private void salvarPJ() {
        try {
            services.clientes().salvarPJ(buildPJ(new ClienteJuridico()));
            JOptionPane.showMessageDialog(this, "Cliente (PJ) salvo!"); limparPJ(); reloadPJ();
        } catch (RegraNegocioException e) { showError(e.getMessage()); }
        catch (Exception ex) { showError("Erro: " + ex.getMessage()); }
    }

    private void atualizarPJ() {
        if (idPJSel == null) { JOptionPane.showMessageDialog(this, "Selecione um cliente PJ."); return; }
        try {
            ClienteJuridico c = services.clientes().buscarPJPorId(idPJSel)
                    .orElseThrow(() -> new RegraNegocioException("Cliente PJ não encontrado."));
            services.clientes().salvarPJ(buildPJ(c));
            JOptionPane.showMessageDialog(this, "Cliente (PJ) atualizado."); reloadPJ();
        } catch (RegraNegocioException e) { showError(e.getMessage()); }
        catch (Exception ex) { showError("Erro: " + ex.getMessage()); }
    }

    private void excluirPJ() {
        if (idPJSel == null) { JOptionPane.showMessageDialog(this, "Selecione um cliente PJ."); return; }
        if (confirm("Confirma exclusão?")) {
            try { services.clientes().excluirPJ(idPJSel); JOptionPane.showMessageDialog(this, "Cliente PJ removido."); limparPJ(); reloadPJ(); }
            catch (RegraNegocioException e) { showError(e.getMessage()); }
        }
    }

    // ── Ações Fornecedor ──────────────────────────────────────────────────────

    private void limparFornecedor() {
        idFSel = null;
        tfFRazao.setText(""); tfFFantasia.setText(""); tfFCnpj.setText(""); tfFIE.setText("");
        tfFTel.setText(""); tfFEmail.setText(""); tfFContato.setText("");
        tfFCep.setText(""); tfFLog.setText(""); tfFNumero.setText("");
        tfFBairro.setText(""); tfFCidade.setText(""); tfFUf.setText("");
        fornTable.clearSelection(); formTabs.setSelectedIndex(2);
    }

    private void salvarFornecedor() {
        try {
            services.clientes().salvarFornecedor(buildFornecedor(new Fornecedor()));
            JOptionPane.showMessageDialog(this, "Fornecedor salvo!"); limparFornecedor(); reloadFornecedor();
        } catch (RegraNegocioException e) { showError(e.getMessage()); }
        catch (Exception ex) { showError("Erro: " + ex.getMessage()); }
    }

    private void atualizarFornecedor() {
        if (idFSel == null) { JOptionPane.showMessageDialog(this, "Selecione um fornecedor."); return; }
        try {
            Fornecedor f = services.clientes().buscarFornecedorPorId(idFSel)
                    .orElseThrow(() -> new RegraNegocioException("Fornecedor não encontrado."));
            services.clientes().salvarFornecedor(buildFornecedor(f));
            JOptionPane.showMessageDialog(this, "Fornecedor atualizado."); reloadFornecedor();
        } catch (RegraNegocioException e) { showError(e.getMessage()); }
        catch (Exception ex) { showError("Erro: " + ex.getMessage()); }
    }

    private void excluirFornecedor() {
        if (idFSel == null) { JOptionPane.showMessageDialog(this, "Selecione um fornecedor."); return; }
        if (confirm("Confirma exclusão?")) {
            try { services.clientes().excluirFornecedor(idFSel); JOptionPane.showMessageDialog(this, "Fornecedor removido."); limparFornecedor(); reloadFornecedor(); }
            catch (RegraNegocioException e) { showError(e.getMessage()); }
        }
    }

    // ── Reload ────────────────────────────────────────────────────────────────

    public void reloadTable() { reloadPF(); reloadPJ(); reloadFornecedor(); }

    private void reloadPF() {
        // Respeitando o filtro atual se houver um termo digitado
        String termo = tfBuscaPF != null ? tfBuscaPF.getText() : "";
        pfModel.setRowCount(0);
        services.clientes().buscarPF(termo).forEach(c ->
                pfModel.addRow(new Object[]{ c.getId(), c.getNome(), c.getCpf(), c.getTelefone(), n(c.getEmail()) }));
    }

    private void reloadPJ() {
        String termo = tfBuscaPJ != null ? tfBuscaPJ.getText() : "";
        pjModel.setRowCount(0);
        services.clientes().buscarPJ(termo).forEach(c ->
                pjModel.addRow(new Object[]{ c.getId(), c.getRazaoSocial(), n(c.getCnpj()), n(c.getTelefone()), n(c.getCidade()) }));
    }

    private void reloadFornecedor() {
        String termo = tfBuscaForn != null ? tfBuscaForn.getText() : "";
        fornModel.setRowCount(0);
        services.clientes().buscarFornecedores(termo).forEach(f ->
                fornModel.addRow(new Object[]{ f.getId(), f.getRazaoSocial(), n(f.getCnpj()), n(f.getTelefone()), n(f.getCidade()) }));
    }

    // ── Preencher formulários ─────────────────────────────────────────────────

    private void preencherPF(Long id) {
        services.clientes().buscarPFPorId(id).ifPresent(c -> {
            idPFSel = id;
            tfNome.setText(n(c.getNome())); tfCpf.setText(n(c.getCpf()));
            tfTel.setText(n(c.getTelefone())); tfEmail.setText(n(c.getEmail()));
            if (c.getEnderecos() != null && !c.getEnderecos().isEmpty()) {
                Endereco e = c.getEnderecos().get(0);
                tfNumero.setText(String.valueOf(e.getNumero()));
                tfCep.setText(n(e.getCep())); tfLog.setText(n(e.getLogradouro()));
                tfComp.setText(n(e.getComplemento())); tfBairro.setText(n(e.getBairro()));
                tfCidade.setText(n(e.getLocalidade())); tfUf.setText(n(e.getUf()));
            }
        });
    }

    private void preencherPJ(Long id) {
        services.clientes().buscarPJPorId(id).ifPresent(c -> {
            idPJSel = id;
            tfPJRazao.setText(n(c.getRazaoSocial())); tfPJFantasia.setText(n(c.getNomeFantasia()));
            tfPJCnpj.setText(n(c.getCnpj())); tfPJIE.setText(n(c.getInscricaoEstadual()));
            tfPJTel.setText(n(c.getTelefone())); tfPJEmail.setText(n(c.getEmail()));
            tfPJContato.setText(n(c.getContato())); tfPJCep.setText(n(c.getCep()));
            tfPJLog.setText(n(c.getLogradouro())); tfPJNumero.setText(n(c.getNumero()));
            tfPJBairro.setText(n(c.getBairro())); tfPJCidade.setText(n(c.getCidade()));
            tfPJUf.setText(n(c.getUf()));
        });
    }

    private void preencherFornecedor(Long id) {
        services.clientes().buscarFornecedorPorId(id).ifPresent(f -> {
            idFSel = id;
            tfFRazao.setText(n(f.getRazaoSocial())); tfFFantasia.setText(n(f.getNomeFantasia()));
            tfFCnpj.setText(n(f.getCnpj())); tfFIE.setText(n(f.getInscricaoEstadual()));
            tfFTel.setText(n(f.getTelefone())); tfFEmail.setText(n(f.getEmail()));
            tfFContato.setText(n(f.getContato())); tfFCep.setText(n(f.getCep()));
            tfFLog.setText(n(f.getLogradouro())); tfFNumero.setText(n(f.getNumero()));
            tfFBairro.setText(n(f.getBairro())); tfFCidade.setText(n(f.getCidade()));
            tfFUf.setText(n(f.getUf()));
        });
    }

    // ── Builders de model ─────────────────────────────────────────────────────

    private Endereco buildEndereco() {
        Endereco e = new Endereco();
        applyEndereco(e);
        return e;
    }

    private void applyEndereco(Endereco e) {
        try { e.setNumero(Integer.parseInt(tfNumero.getText().trim())); } catch (Exception ex) { e.setNumero(0); }
        e.setCep(tfCep.getText().trim()); e.setLogradouro(tfLog.getText().trim());
        e.setComplemento(tfComp.getText().trim()); e.setBairro(tfBairro.getText().trim());
        e.setLocalidade(tfCidade.getText().trim()); e.setUf(tfUf.getText().trim().toUpperCase());
    }

    private ClienteJuridico buildPJ(ClienteJuridico c) {
        c.setRazaoSocial(tfPJRazao.getText().trim()); c.setNomeFantasia(tfPJFantasia.getText().trim());
        c.setCnpj(tfPJCnpj.getText().trim()); c.setInscricaoEstadual(tfPJIE.getText().trim());
        c.setTelefone(tfPJTel.getText().trim()); c.setEmail(tfPJEmail.getText().trim());
        c.setContato(tfPJContato.getText().trim()); c.setCep(tfPJCep.getText().trim());
        c.setLogradouro(tfPJLog.getText().trim()); c.setNumero(tfPJNumero.getText().trim());
        c.setBairro(tfPJBairro.getText().trim()); c.setCidade(tfPJCidade.getText().trim());
        c.setUf(tfPJUf.getText().trim().toUpperCase());
        return c;
    }

    private Fornecedor buildFornecedor(Fornecedor f) {
        f.setRazaoSocial(tfFRazao.getText().trim()); f.setNomeFantasia(tfFFantasia.getText().trim());
        f.setCnpj(tfFCnpj.getText().trim()); f.setInscricaoEstadual(tfFIE.getText().trim());
        f.setTelefone(tfFTel.getText().trim()); f.setEmail(tfFEmail.getText().trim());
        f.setContato(tfFContato.getText().trim()); f.setCep(tfFCep.getText().trim());
        f.setLogradouro(tfFLog.getText().trim()); f.setNumero(tfFNumero.getText().trim());
        f.setBairro(tfFBairro.getText().trim()); f.setCidade(tfFCidade.getText().trim());
        f.setUf(tfFUf.getText().trim().toUpperCase());
        return f;
    }

    // ── Busca CEP e CNPJ ─────────────────────────────────────────────────────

    private void buscarCep(JTextField tfC, JTextField tfL, JTextField tfB,
                           JTextField tfCid, JTextField tfU) {
        String cep = tfC.getText().trim().replaceAll("[^0-9]", "");
        if (cep.isBlank()) { JOptionPane.showMessageDialog(this, "Informe o CEP."); return; }
        try {
            Endereco via = viaCepClient.buscarCep(cep);
            if (via != null) {
                tfL.setText(n(via.getLogradouro())); tfB.setText(n(via.getBairro()));
                tfCid.setText(n(via.getLocalidade())); tfU.setText(n(via.getUf()));
                tfC.setText(n(via.getCep()));
            } else JOptionPane.showMessageDialog(this, "CEP não encontrado.");
        } catch (Exception ex) { showError("Erro ao buscar CEP: " + ex.getMessage()); }
    }

    private void buscarCnpj(JTextField tfCnpjF, JTextField tfRaz, JTextField tfFant,
                            JTextField tfT, JTextField tfEm, JTextField tfC,
                            JTextField tfL, JTextField tfNum, JTextField tfB,
                            JTextField tfCid, JTextField tfU) {
        String cnpj = tfCnpjF.getText().trim();
        if (Validator.isBlank(cnpj)) { JOptionPane.showMessageDialog(this, "Informe o CNPJ."); return; }

        JDialog ag = new JDialog(SwingUtilities.getWindowAncestor(this), "Consultando...",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        ag.setSize(260, 70); ag.setLocationRelativeTo(this);
        ag.add(new JLabel("  Consultando CNPJ na BrasilAPI...", SwingConstants.LEFT));

        new Thread(() -> {
            try {
                CnpjClient.DadosEmpresa d = CnpjClient.consultar(cnpj);
                SwingUtilities.invokeLater(() -> {
                    ag.dispose();
                    if (d == null) { JOptionPane.showMessageDialog(this, "CNPJ não encontrado."); return; }
                    if (d.razaoSocial  != null && !d.razaoSocial.isEmpty())  tfRaz.setText(d.razaoSocial);
                    if (d.nomeFantasia != null && !d.nomeFantasia.isEmpty()) tfFant.setText(d.nomeFantasia);
                    if (d.telefone     != null && !d.telefone.isEmpty())     tfT.setText(d.telefone);
                    if (d.email        != null && !d.email.isEmpty())        tfEm.setText(d.email);
                    if (d.cep          != null && !d.cep.isEmpty())          tfC.setText(d.cep);
                    if (d.logradouro   != null && !d.logradouro.isEmpty())   tfL.setText(d.logradouro);
                    if (d.numero       != null && !d.numero.isEmpty())       tfNum.setText(d.numero);
                    if (d.bairro       != null && !d.bairro.isEmpty())       tfB.setText(d.bairro);
                    if (d.cidade       != null && !d.cidade.isEmpty())       tfCid.setText(d.cidade);
                    if (d.uf           != null && !d.uf.isEmpty())           tfU.setText(d.uf);
                    boolean ativa = d.situacao != null && d.situacao.toUpperCase().contains("ATIVA");
                    JOptionPane.showMessageDialog(this,
                            (ativa ? "✅" : "⚠") + " Dados preenchidos!\nSituação: " + n(d.situacao),
                            "CNPJ", ativa ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> { ag.dispose(); showError("Erro ao consultar CNPJ:\n" + ex.getMessage()); });
            }
        }).start();
        ag.setVisible(true);
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

    private JPanel form(String titulo) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(UIFactory.groupBorder(titulo));
        return p;
    }

    private JPanel wrap(JPanel form) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(UIFactory.XP_BG);
        outer.setBorder(new EmptyBorder(8, 8, 4, 4));
        outer.add(form, BorderLayout.CENTER);
        return outer;
    }

    private DefaultTableModel tableModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 6, 3, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String lbl, Component field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        p.add(UIFactory.labelLight(lbl), c);
        c.gridx = 1; c.weightx = 1; p.add(field, c);
    }

    private void addCepRow(JPanel p, GridBagConstraints c, int row,
                           JTextField tfC, JTextField tfL, JTextField tfB,
                           JTextField tfCid, JTextField tfU) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        p.add(UIFactory.labelLight("CEP:"), c);
        JButton btn = UIFactory.bigSmallButton("Buscar");
        JPanel row2 = new JPanel(new BorderLayout(4, 0));
        row2.setOpaque(false);
        row2.add(tfC, BorderLayout.CENTER);
        row2.add(btn, BorderLayout.EAST);
        c.gridx = 1; c.weightx = 1; p.add(row2, c);
        btn.addActionListener(e -> buscarCep(tfC, tfL, tfB, tfCid, tfU));
    }

    private void addCnpjRow(JPanel p, GridBagConstraints c, int row,
                            JTextField tfCnpjF, JTextField tfRaz, JTextField tfFant,
                            JTextField tfT, JTextField tfEm, JTextField tfCep2,
                            JTextField tfL, JTextField tfNum, JTextField tfB,
                            JTextField tfCid, JTextField tfU) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        p.add(UIFactory.labelLight("CNPJ:"), c);
        JButton btn = UIFactory.bigSmallButton("Buscar");
        btn.setBackground(new Color(0, 84, 166)); btn.setForeground(Color.WHITE);
        JPanel row2 = new JPanel(new BorderLayout(4, 0));
        row2.setOpaque(false);
        row2.add(tfCnpjF, BorderLayout.CENTER);
        row2.add(btn, BorderLayout.EAST);
        c.gridx = 1; c.weightx = 1; p.add(row2, c);
        btn.addActionListener(e -> buscarCnpj(tfCnpjF, tfRaz, tfFant, tfT, tfEm,
                tfCep2, tfL, tfNum, tfB, tfCid, tfU));
    }

    private boolean confirm(String msg) {
        return JOptionPane.showConfirmDialog(this, msg, "Confirmar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private String n(String s) { return s != null ? s : ""; }
    private void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE); }
}
