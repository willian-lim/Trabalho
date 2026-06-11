package br.carmel.ui.panels;

import br.carmel.model.Produto;
import br.carmel.service.ProdutoService;
import br.carmel.service.RegraNegocioException;
import br.carmel.service.ServiceLocator;
import br.carmel.util.UIFactory;
import br.carmel.util.Validator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Painel de Cadastro de Produtos — com aba "Impressão 3D".
 * Layout dividido: formulário/abas à esquerda, tabela à direita.
 */
public class ProdutoPanel extends JPanel {

    private final ServiceLocator services;

    // ── Campos principais ─────────────────────────────────────────────────────
    private final JTextField tfNome  = UIFactory.styledField("");
    private final JTextField tfVenda = UIFactory.styledField("");
    private final JTextField tfCusto = UIFactory.styledField("");
    private final JTextField tfCod   = UIFactory.styledField("");
    private final JTextField tfSerie = UIFactory.styledField("");
    private final JTextArea  taDesc  = UIFactory.styledTextArea(3, 18);
    private final JLabel lblEstoque   = new JLabel("0");
    private final JLabel lblPrecoMedio = new JLabel("—");
    private final JLabel lblMargem    = new JLabel("—");

    // ── Campos Impressão 3D ───────────────────────────────────────────────────
    private final JTextField tf3dFilamentoPorKg  = UIFactory.styledField("");
    private final JTextField tf3dGramas          = UIFactory.styledField("");
    private final JTextField tf3dHoras           = UIFactory.styledField("");
    private final JTextField tf3dTarifaEnergia   = UIFactory.styledField("");
    private final JTextField tf3dWatts           = UIFactory.styledField("");
    private final JTextField tf3dMargem          = UIFactory.styledField("500");
    private final JTextField tf3dMaoDeObra       = UIFactory.styledField("");
    private final JLabel lbl3dCustoFilamento     = new JLabel("R$ —");
    private final JLabel lbl3dCustoEnergia       = new JLabel("R$ —");
    private final JLabel lbl3dCustoMaoDeObra     = new JLabel("R$ —");
    private final JLabel lbl3dCustoTotal         = new JLabel("R$ —");
    private final JLabel lbl3dPrecoVenda         = new JLabel("R$ —");

    private DefaultTableModel tableModel;
    private JTable table;
    private Long idSelecionado = null;

    // ── Campos de busca ───────────────────────────────────────────────────────
    private final JTextField tfBuscaNome    = UIFactory.styledField("");
    private final JTextField tfBuscaPrecoMin = UIFactory.styledField("");
    private final JTextField tfBuscaPrecoMax = UIFactory.styledField("");
    private final JTextField tfBuscaCod     = UIFactory.styledField("");

    public ProdutoPanel(ServiceLocator services) {
        this.services = services;
        setLayout(new BorderLayout());
        setBackground(UIFactory.XP_BG);
        build();
    }

    // ── Montagem ──────────────────────────────────────────────────────────────

    private void build() {
        add(UIFactory.xpTitleBar("Cadastro de Produtos"), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildTablePanel());
        split.setDividerLocation(380);
        split.setDividerSize(5);
        split.setBackground(UIFactory.XP_BG);
        add(split, BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);
    }

    // ── Painel esquerdo com abas ──────────────────────────────────────────────

    private JPanel buildLeftPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(UIFactory.XP_BG);
        outer.setBorder(new EmptyBorder(8, 8, 8, 4));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(UIFactory.W11_SURFACE);
        tabs.setForeground(UIFactory.W11_TEXT);
        tabs.setFont(UIFactory.FONT_BOLD);
        tabs.addTab("📦  Dados do Produto", buildFormPanel());
        tabs.addTab("🖨️  Impressão 3D",    build3DPanel());

        outer.add(tabs, BorderLayout.CENTER);
        return outer;
    }

    // ── Aba 1: Dados do Produto ───────────────────────────────────────────────

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(new EmptyBorder(8, 6, 8, 6));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill   = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;
        addRow(form, c, row++, "Nome:",                tfNome);
        addRow(form, c, row++, "Preço de Venda (R$):", tfVenda);
        addRow(form, c, row++, "Preço de Custo (R$):", tfCusto);

        // Preço médio — somente leitura
        lblPrecoMedio.setFont(UIFactory.FONT_BOLD);
        lblPrecoMedio.setForeground(UIFactory.W11_ACCENT);
        addRow(form, c, row++, "Preço Médio (R$):", lblPrecoMedio);

        // Margem calculada
        lblMargem.setFont(UIFactory.FONT_BOLD);
        lblMargem.setForeground(UIFactory.W11_SUCCESS);
        addRow(form, c, row++, "Margem:", lblMargem);

        Runnable calcMargem = () -> {
            try {
                BigDecimal v  = Validator.parseBigDecimal(tfVenda.getText());
                BigDecimal cu = Validator.parseBigDecimal(tfCusto.getText());
                if (cu.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal m = v.subtract(cu)
                            .divide(cu, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    lblMargem.setText(String.format("%.1f%%", m));
                    lblMargem.setForeground(m.compareTo(BigDecimal.ZERO) >= 0
                            ? UIFactory.W11_SUCCESS : new Color(220, 60, 60));
                } else lblMargem.setText("—");
            } catch (Exception e) { lblMargem.setText("—"); }
        };
        tfVenda.addFocusListener(fl(calcMargem));
        tfCusto.addFocusListener(fl(calcMargem));

        // Estoque + botão
        JPanel estoqueRow = new JPanel(new BorderLayout(6, 0));
        estoqueRow.setOpaque(false);
        lblEstoque.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblEstoque.setForeground(UIFactory.W11_ACCENT);
        JButton btnAdd = UIFactory.bigActionButton("+ Adicionar Estoque", e -> adicionarEstoque());
        btnAdd.setPreferredSize(new Dimension(150, 24));
        estoqueRow.add(lblEstoque, BorderLayout.WEST);
        estoqueRow.add(btnAdd, BorderLayout.EAST);
        addRow(form, c, row++, "Estoque atual:", estoqueRow);

        addRow(form, c, row++, "Descrição:",      new JScrollPane(taDesc));
        addRow(form, c, row++, "Cód. de Barras:", tfCod);
        addRow(form, c, row,   "Nº de Série:",    tfSerie);

        c.gridx = 0; c.gridy = row + 1; c.gridwidth = 2; c.weighty = 1.0;
        form.add(Box.createVerticalGlue(), c);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UIFactory.XP_BG);
        wrapper.add(form, BorderLayout.CENTER);
        return wrapper;
    }

    // ── Aba 2: Impressão 3D ───────────────────────────────────────────────────

    private JPanel build3DPanel() {
        JPanel main = new JPanel(new BorderLayout(0, 8));
        main.setBackground(UIFactory.XP_BG);
        main.setBorder(new EmptyBorder(10, 8, 8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill   = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        int row = 0;

        // ── Filamento ─────────────────────────────────────────────────────────
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weightx = 1;
        form.add(sectionLabel("🧵  FILAMENTO"), c);
        row++;

        addRow3d(form, c, row++, "Preço do kg do filamento (R$):", tf3dFilamentoPorKg);
        addRow3d(form, c, row++, "Material usado na impressão (g):", tf3dGramas);

        // Custo filamento (readonly)
        lbl3dCustoFilamento.setForeground(UIFactory.W11_TEXT_SEC);
        lbl3dCustoFilamento.setFont(UIFactory.FONT_NORMAL);
        addRow3d(form, c, row++, "Custo do filamento:", lbl3dCustoFilamento);

        // ── Fatiador ─────────────────────────────────────────────────────────
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        form.add(sectionLabel("⏱️  FATIADOR"), c);
        row++;
        addRow3d(form, c, row++, "Duração da impressão (h):", tf3dHoras);

        // ── Energia ──────────────────────────────────────────────────────────
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        form.add(sectionLabel("⚡  ENERGIA"), c);
        row++;
        addRow3d(form, c, row++, "Tarifa da distribuidora (R$/kWh):", tf3dTarifaEnergia);

        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        form.add(UIFactory.labelLight("Consumo da impressora (W):"), c);
        c.gridx = 1; c.weightx = 1;
        JPanel wattsPanel = new JPanel(new BorderLayout(6, 0));
        wattsPanel.setOpaque(false);
        wattsPanel.add(tf3dWatts, BorderLayout.CENTER);
        JLabel hints = new JLabel("Ender 3: 110W | Prusa MK3: 180W");
        hints.setFont(UIFactory.FONT_SMALL);
        hints.setForeground(UIFactory.W11_TEXT_SEC);
        JPanel hintsWrap = new JPanel(new BorderLayout());
        hintsWrap.setOpaque(false);
        hintsWrap.add(wattsPanel, BorderLayout.NORTH);
        hintsWrap.add(hints, BorderLayout.SOUTH);
        form.add(hintsWrap, c);
        row++;

        lbl3dCustoEnergia.setForeground(UIFactory.W11_TEXT_SEC);
        lbl3dCustoEnergia.setFont(UIFactory.FONT_NORMAL);
        addRow3d(form, c, row++, "Custo de energia:", lbl3dCustoEnergia);

        // ── Mão de Obra ───────────────────────────────────────────────────────
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        form.add(sectionLabel("👷  MÃO DE OBRA"), c);
        row++;

        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        form.add(UIFactory.labelLight("Valor hora de trabalho (R$/h):"), c);
        c.gridx = 1; c.weightx = 1;
        JPanel maoPanel = new JPanel(new BorderLayout(6, 0));
        maoPanel.setOpaque(false);
        maoPanel.add(tf3dMaoDeObra, BorderLayout.CENTER);
        JLabel maoHint = new JLabel("Inclui pós-processamento, suporte e remoção");
        maoHint.setFont(UIFactory.FONT_SMALL);
        maoHint.setForeground(UIFactory.W11_TEXT_SEC);
        JPanel maoWrap = new JPanel(new BorderLayout());
        maoWrap.setOpaque(false);
        maoWrap.add(maoPanel, BorderLayout.NORTH);
        maoWrap.add(maoHint, BorderLayout.SOUTH);
        form.add(maoWrap, c);
        row++;

        lbl3dCustoMaoDeObra.setForeground(UIFactory.W11_TEXT_SEC);
        lbl3dCustoMaoDeObra.setFont(UIFactory.FONT_NORMAL);
        addRow3d(form, c, row++, "Custo mão de obra:", lbl3dCustoMaoDeObra);

        // ── Lucro ─────────────────────────────────────────────────────────────
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        form.add(sectionLabel("💰  LUCRO"), c);
        row++;

        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        form.add(UIFactory.labelLight("Margem de lucro (%):"), c);
        c.gridx = 1; c.weightx = 1;
        JPanel margemPanel = new JPanel(new BorderLayout(6, 0));
        margemPanel.setOpaque(false);
        margemPanel.add(tf3dMargem, BorderLayout.CENTER);
        JLabel margemHint = new JLabel("⚠ Mínimo recomendado: 500% para cobrir custos de tempo");
        margemHint.setFont(UIFactory.FONT_SMALL);
        margemHint.setForeground(new Color(200, 160, 60));
        JPanel margemWrap = new JPanel(new BorderLayout());
        margemWrap.setOpaque(false);
        margemWrap.add(margemPanel, BorderLayout.NORTH);
        margemWrap.add(margemHint, BorderLayout.SOUTH);
        form.add(margemWrap, c);
        row++;

        // espaçador
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weighty = 1.0;
        form.add(Box.createVerticalGlue(), c);

        main.add(form, BorderLayout.CENTER);
        main.add(buildResultadoPanel(), BorderLayout.SOUTH);

        // Listeners de cálculo automático
        FocusAdapter calcListener = new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { calcular3D(); }
        };
        tf3dFilamentoPorKg.addFocusListener(calcListener);
        tf3dGramas.addFocusListener(calcListener);
        tf3dHoras.addFocusListener(calcListener);
        tf3dTarifaEnergia.addFocusListener(calcListener);
        tf3dWatts.addFocusListener(calcListener);
        tf3dMargem.addFocusListener(calcListener);
        tf3dMaoDeObra.addFocusListener(calcListener);

        return main;
    }

    private JPanel buildResultadoPanel() {
        JPanel res = new JPanel(new GridLayout(1, 2, 12, 0));
        res.setBackground(UIFactory.XP_BG);
        res.setBorder(BorderFactory.createCompoundBorder(
                UIFactory.groupBorder("📊  Resultado da Calculadora"),
                new EmptyBorder(8, 8, 8, 8)));

        // Custo total
        JPanel custoCard = buildResultCard("Custo Total", lbl3dCustoTotal, UIFactory.W11_TEXT_SEC);
        // Preço de venda sugerido
        JPanel vendaCard = buildResultCard("Preço de Venda Sugerido", lbl3dPrecoVenda, UIFactory.W11_ACCENT);

        // Botão aplicar ao produto
        JButton btnAplicar = UIFactory.primaryButton("Aplicar ao Produto", e -> aplicarCalculo3D());
        btnAplicar.setPreferredSize(new Dimension(160, 32));
        JPanel botaoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        botaoPanel.setOpaque(false);
        botaoPanel.add(btnAplicar);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(vendaCard, BorderLayout.CENTER);
        right.add(botaoPanel, BorderLayout.SOUTH);

        res.add(custoCard);
        res.add(right);
        return res;
    }

    private JPanel buildResultCard(String titulo, JLabel lbl, Color cor) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(UIFactory.W11_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIFactory.W11_BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)));
        JLabel t = new JLabel(titulo);
        t.setFont(UIFactory.FONT_SMALL);
        t.setForeground(UIFactory.W11_TEXT_SEC);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(cor);
        card.add(t, BorderLayout.NORTH);
        card.add(lbl, BorderLayout.CENTER);
        return card;
    }

    // ── Calculadora 3D — delega ao ProdutoService ─────────────────────────────

    /** Último resultado calculado — usado pelo botão "Aplicar ao Produto". */
    private ProdutoService.Impressao3DResultado ultimo3DResultado = null;

    private void calcular3D() {
        try {
            ProdutoService.Impressao3DInput input = new ProdutoService.Impressao3DInput(
                    parseFieldDouble(tf3dFilamentoPorKg),
                    parseFieldDouble(tf3dGramas),
                    parseFieldDouble(tf3dHoras),
                    parseFieldDouble(tf3dTarifaEnergia),
                    parseFieldDouble(tf3dWatts),
                    parseFieldDouble(tf3dMaoDeObra),
                    parseFieldDouble(tf3dMargem)
            );

            // Toda a lógica fica no serviço
            ultimo3DResultado = services.produtos().calcularImpressao3D(input);

            lbl3dCustoFilamento.setText(String.format("R$ %.2f", ultimo3DResultado.custoFilamento()));
            lbl3dCustoEnergia.setText(String.format("R$ %.2f",   ultimo3DResultado.custoEnergia()));
            lbl3dCustoMaoDeObra.setText(String.format("R$ %.2f", ultimo3DResultado.custoMaoDeObra()));
            lbl3dCustoTotal.setText(String.format("R$ %.2f",     ultimo3DResultado.custoTotal()));
            lbl3dPrecoVenda.setText(String.format("R$ %.2f",     ultimo3DResultado.precoVendaSugerido()));

        } catch (RegraNegocioException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            // Campo ainda vazio/incompleto — aguarda o usuário terminar de preencher
        }
    }

    private void aplicarCalculo3D() {
        if (ultimo3DResultado == null) {
            JOptionPane.showMessageDialog(this,
                    "Preencha os campos da calculadora 3D e aguarde o cálculo.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        tfVenda.setText(ultimo3DResultado.precoVendaSugerido().toPlainString());
        tfCusto.setText(ultimo3DResultado.custoTotal().toPlainString());
        JOptionPane.showMessageDialog(this,
                "Valores aplicados ao produto!\nNão esqueça de salvar.",
                "Calculadora 3D", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Lê um JTextField como double; retorna 0.0 se vazio. Sem lógica de negócio. */
    private double parseFieldDouble(JTextField tf) {
        String text = tf.getText();
        if (text == null || text.isBlank()) return 0.0;
        return Double.parseDouble(text.trim().replace(",", "."));
    }

    // ── Painel direito: tabela ────────────────────────────────────────────────

    private JPanel buildTablePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 4, 8, 8));

        JLabel lbl = new JLabel("Produtos Cadastrados");
        lbl.setFont(UIFactory.FONT_BOLD);
        lbl.setForeground(UIFactory.W11_ACCENT);
        lbl.setBorder(new EmptyBorder(0, 0, 4, 0));
        p.add(lbl, BorderLayout.NORTH);

        // ── Painel de busca ───────────────────────────────────────────────────
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBackground(UIFactory.W11_SURFACE);
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UIFactory.W11_BORDER, 1),
                        "🔍  Consultar Produto",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        UIFactory.FONT_BOLD, UIFactory.W11_ACCENT),
                new EmptyBorder(4, 6, 6, 6)));

        GridBagConstraints sc = new GridBagConstraints();
        sc.insets = new Insets(3, 4, 3, 4);
        sc.fill = GridBagConstraints.HORIZONTAL;

        // Linha 0: Nome
        sc.gridx = 0; sc.gridy = 0; sc.weightx = 0;
        searchPanel.add(UIFactory.labelLight("Nome:"), sc);
        sc.gridx = 1; sc.weightx = 1; sc.gridwidth = 3;
        searchPanel.add(tfBuscaNome, sc);
        sc.gridwidth = 1;

        // Linha 1: Preço mín / máx
        sc.gridx = 0; sc.gridy = 1; sc.weightx = 0;
        searchPanel.add(UIFactory.labelLight("Preço mín (R$):"), sc);
        sc.gridx = 1; sc.weightx = 0.5;
        searchPanel.add(tfBuscaPrecoMin, sc);
        sc.gridx = 2; sc.weightx = 0;
        searchPanel.add(UIFactory.labelLight("Preço máx (R$):"), sc);
        sc.gridx = 3; sc.weightx = 0.5;
        searchPanel.add(tfBuscaPrecoMax, sc);

        // Linha 2: Código de barras + botões
        sc.gridx = 0; sc.gridy = 2; sc.weightx = 0;
        searchPanel.add(UIFactory.labelLight("Cód. de Barras:"), sc);
        sc.gridx = 1; sc.weightx = 1; sc.gridwidth = 2;
        searchPanel.add(tfBuscaCod, sc);
        sc.gridwidth = 1;

        JButton btnBuscar = UIFactory.bigActionButton("🔍 Buscar", e -> executarBusca());
        btnBuscar.setBackground(UIFactory.W11_ACCENT);
        btnBuscar.setForeground(Color.WHITE);
        JButton btnLimpar = UIFactory.bigActionButton("✖ Limpar", e -> limparBusca());

        sc.gridx = 3; sc.weightx = 0; sc.gridy = 2;
        JPanel btnsBusca = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnsBusca.setOpaque(false);
        btnsBusca.add(btnBuscar);
        btnsBusca.add(btnLimpar);
        searchPanel.add(btnsBusca, sc);

        // Enter nos campos dispara busca
        tfBuscaNome.addActionListener(e -> executarBusca());
        tfBuscaPrecoMin.addActionListener(e -> executarBusca());
        tfBuscaPrecoMax.addActionListener(e -> executarBusca());
        tfBuscaCod.addActionListener(e -> executarBusca());

        tableModel = new DefaultTableModel(
                new String[]{"ID","Nome","Venda (R$)","Custo (R$)","Médio (R$)","Estoque","Cód."}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = UIFactory.styledTable();
        table.setModel(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(60);
        table.getColumnModel().getColumn(6).setPreferredWidth(90);

        table.setDefaultRenderer(Object.class, (t, value, isSelected, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(UIFactory.FONT_NORMAL);
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(1, 4, 1, 4));
            if (isSelected) {
                cell.setBackground(UIFactory.W11_TABLE_SEL);
                cell.setForeground(UIFactory.W11_TABLE_SEL_FG);
            } else {
                cell.setBackground(row % 2 == 0 ? UIFactory.W11_SURFACE : UIFactory.W11_SURFACE2);
                if (col == 5) {
                    try {
                        int est = Integer.parseInt(value.toString());
                        cell.setForeground(est <= 0 ? new Color(220, 60, 60) : UIFactory.W11_SUCCESS);
                        cell.setFont(UIFactory.FONT_BOLD);
                    } catch (Exception e) { cell.setForeground(UIFactory.W11_TEXT); }
                } else { cell.setForeground(UIFactory.W11_TEXT); }
            }
            return cell;
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0)
                preencherPorId((Long) tableModel.getValueAt(table.getSelectedRow(), 0));
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIFactory.W11_BORDER));
        scroll.getViewport().setBackground(UIFactory.W11_SURFACE);

        JPanel tableArea = new JPanel(new BorderLayout(0, 4));
        tableArea.setBackground(UIFactory.XP_BG);
        tableArea.add(searchPanel, BorderLayout.NORTH);
        tableArea.add(scroll, BorderLayout.CENTER);

        p.add(tableArea, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        bar.setBackground(UIFactory.W11_PANEL_BG);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIFactory.W11_BORDER));
        bar.add(UIFactory.bigActionButton("Novo",            e -> limpar()));
        bar.add(UIFactory.bigActionButton("Salvar",          e -> salvar()));
        bar.add(UIFactory.bigActionButton("Atualizar",       e -> atualizar()));
        bar.add(UIFactory.bigActionButton("Excluir",         e -> excluir()));
        bar.add(UIFactory.bigActionButton("Atualizar Lista", e -> reloadTable()));
        return bar;
    }

    // ── Ações ─────────────────────────────────────────────────────────────────

    private void limpar() {
        idSelecionado = null;
        tfNome.setText(""); tfVenda.setText(""); tfCusto.setText("");
        tfCod.setText(""); tfSerie.setText(""); taDesc.setText("");
        lblEstoque.setText("0");
        lblPrecoMedio.setText("—"); lblPrecoMedio.setForeground(UIFactory.W11_TEXT_SEC);
        lblMargem.setText("—");
        table.clearSelection();
    }

    private void salvar() {
        try {
            Produto p = fromForm(new Produto());
            services.produtos().salvar(p);
            JOptionPane.showMessageDialog(this, "Produto salvo!");
            limpar(); reloadTable();
        } catch (RegraNegocioException e) {
            showError(e.getMessage());
        } catch (Exception ex) {
            showError("Erro ao salvar: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void atualizar() {
        if (idSelecionado == null) { JOptionPane.showMessageDialog(this, "Selecione um produto."); return; }
        try {
            Produto atual = services.produtos().buscarPorId(idSelecionado)
                    .orElseThrow(() -> new RegraNegocioException("Produto não encontrado."));
            BigDecimal novoCusto = Validator.isBlank(tfCusto.getText())
                    ? null : Validator.parseBigDecimal(tfCusto.getText());
            if (novoCusto != null) {
                atual = services.produtos().atualizarPrecoCusto(atual, novoCusto);
            }
            fromForm(atual);
            services.produtos().salvar(atual);
            JOptionPane.showMessageDialog(this, "Produto atualizado.");
            reloadTable();
            preencherPorId(idSelecionado);
        } catch (RegraNegocioException e) {
            showError(e.getMessage());
        } catch (Exception ex) {
            showError("Erro ao atualizar: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void excluir() {
        if (idSelecionado == null) { JOptionPane.showMessageDialog(this, "Selecione um produto."); return; }
        if (JOptionPane.showConfirmDialog(this, "Confirma exclusão?", "Confirmar",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            services.produtos().excluir(idSelecionado);
            JOptionPane.showMessageDialog(this, "Produto removido.");
            limpar(); reloadTable();
        } catch (RegraNegocioException e) {
            showError(e.getMessage());
        } catch (Exception ex) {
            showError("Erro ao excluir: " + ex.getMessage());
        }
    }

    private void adicionarEstoque() {
        if (idSelecionado == null) {
            JOptionPane.showMessageDialog(this, "Selecione um produto primeiro.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Adicionar Estoque", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(340, 190);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(new EmptyBorder(14, 16, 8, 16));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill   = GridBagConstraints.HORIZONTAL;

        JLabel lblProd = UIFactory.labelLight("Produto: " + tfNome.getText().trim());
        lblProd.setFont(UIFactory.FONT_BOLD);
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2; form.add(lblProd, gc);

        JTextField tfQtd = UIFactory.styledField("");
        gc.gridwidth = 1;
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; form.add(UIFactory.labelLight("Quantidade:"), gc);
        gc.gridx = 1; gc.weightx = 1; form.add(tfQtd, gc);

        JPasswordField tfSenha = UIFactory.styledPasswordField("");
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; form.add(UIFactory.labelLight("Senha do administrador:"), gc);
        gc.gridx = 1; gc.weightx = 1; form.add(tfSenha, gc);

        dlg.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setBackground(UIFactory.XP_BG);
        boolean[] ok = {false};
        JButton btnOk = UIFactory.bigActionButton("Confirmar", e -> { ok[0] = true; dlg.dispose(); });
        btnOk.setBackground(UIFactory.W11_SUCCESS); btnOk.setForeground(Color.WHITE);
        btns.add(btnOk); btns.add(UIFactory.bigActionButton("Cancelar", e -> dlg.dispose()));
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.getRootPane().setDefaultButton(btnOk);
        SwingUtilities.invokeLater(tfQtd::requestFocusInWindow);
        dlg.setVisible(true);

        if (!ok[0]) return;

        try {
            int qtd = Integer.parseInt(tfQtd.getText().trim());
            String senha = new String(tfSenha.getPassword());
            int estoqueAntes = Integer.parseInt(lblEstoque.getText());
            Produto atualizado = services.produtos().adicionarEstoque(idSelecionado, qtd, senha);
            lblEstoque.setText(String.valueOf(atualizado.getEstoque()));
            JOptionPane.showMessageDialog(this,
                    "Estoque atualizado!\nAntes: " + estoqueAntes +
                    "\nAdicionado: " + qtd + "\nTotal: " + atualizado.getEstoque(),
                    "Estoque Atualizado", JOptionPane.INFORMATION_MESSAGE);
            reloadTable();
        } catch (RegraNegocioException e) {
            showError(e.getMessage());
        } catch (NumberFormatException e) {
            showError("Quantidade inválida.");
        } catch (Exception ex) {
            showError("Erro: " + ex.getMessage());
        }
    }

    // ── Busca de produtos ─────────────────────────────────────────────────────

    private void executarBusca() {
        String nome    = tfBuscaNome.getText().trim();
        String codText = tfBuscaCod.getText().trim();
        BigDecimal pmin = null, pmax = null;
        try { if (!tfBuscaPrecoMin.getText().isBlank()) pmin = Validator.parseBigDecimal(tfBuscaPrecoMin.getText()); }
        catch (Exception ignored) {}
        try { if (!tfBuscaPrecoMax.getText().isBlank()) pmax = Validator.parseBigDecimal(tfBuscaPrecoMax.getText()); }
        catch (Exception ignored) {}

        java.util.List<br.carmel.model.Produto> resultado =
                services.produtos().buscarPorFiltros(
                        nome.isEmpty()    ? null : nome,
                        pmin, pmax,
                        codText.isEmpty() ? null : codText);

        tableModel.setRowCount(0);
        for (br.carmel.model.Produto prod : resultado) {
            String custo = prod.getPrecoCusto() != null ? String.format("R$ %.2f", prod.getPrecoCusto()) : "—";
            String medio = prod.getPrecoMedio()  != null ? String.format("R$ %.2f", prod.getPrecoMedio())  : "—";
            tableModel.addRow(new Object[]{
                    prod.getId(), prod.getNome(),
                    String.format("R$ %.2f", prod.getValor()),
                    custo, medio,
                    prod.getEstoque() != null ? prod.getEstoque() : 0,
                    prod.getCodBarras()
            });
        }

        if (resultado.isEmpty())
            JOptionPane.showMessageDialog(this, "Nenhum produto encontrado com os filtros informados.",
                    "Busca", JOptionPane.INFORMATION_MESSAGE);
    }

    private void limparBusca() {
        tfBuscaNome.setText("");
        tfBuscaPrecoMin.setText("");
        tfBuscaPrecoMax.setText("");
        tfBuscaCod.setText("");
        reloadTable();
    }

    // ── Reload e preenchimento ────────────────────────────────────────────────

    public void reloadTable() {
        tableModel.setRowCount(0);
        services.produtos().listarTodos().forEach(p -> {
            String custo = p.getPrecoCusto() != null ? String.format("R$ %.2f", p.getPrecoCusto()) : "—";
            String medio = p.getPrecoMedio() != null ? String.format("R$ %.2f", p.getPrecoMedio()) : "—";
            tableModel.addRow(new Object[]{
                    p.getId(), p.getNome(),
                    String.format("R$ %.2f", p.getValor()),
                    custo, medio,
                    p.getEstoque() != null ? p.getEstoque() : 0,
                    p.getCodBarras()
            });
        });
    }

    private void preencherPorId(Long id) {
        services.produtos().buscarPorId(id).ifPresent(p -> {
            idSelecionado = id;
            tfNome.setText(p.getNome());
            tfVenda.setText(p.getValor().toString());
            tfCusto.setText(p.getPrecoCusto() != null ? p.getPrecoCusto().toString() : "");
            taDesc.setText(p.getDescricao() != null ? p.getDescricao() : "");
            tfCod.setText(p.getCodBarras() != null ? p.getCodBarras() : "");
            tfSerie.setText(p.getNumeroSerie() != null ? p.getNumeroSerie() : "");
            lblEstoque.setText(String.valueOf(p.getEstoque() != null ? p.getEstoque() : 0));
            if (p.getPrecoMedio() != null) {
                lblPrecoMedio.setText(String.format("R$ %.2f", p.getPrecoMedio()));
                lblPrecoMedio.setForeground(UIFactory.W11_ACCENT);
            } else {
                lblPrecoMedio.setText("— (sem histórico)");
                lblPrecoMedio.setForeground(UIFactory.W11_TEXT_SEC);
            }
        });
    }

    private Produto fromForm(Produto p) {
        p.setNome(tfNome.getText().trim());
        p.setDescricao(taDesc.getText().trim());
        p.setValor(Validator.parseBigDecimal(tfVenda.getText()));
        p.setPrecoCusto(Validator.isBlank(tfCusto.getText())
                ? null : Validator.parseBigDecimal(tfCusto.getText()));
        p.setCodBarras(Validator.isBlank(tfCod.getText()) ? null : tfCod.getText().trim());
        p.setNumeroSerie(Validator.isBlank(tfSerie.getText()) ? null : tfSerie.getText().trim());
        if (p.getEstoque() == null) p.setEstoque(0);
        return p;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(UIFactory.W11_ACCENT);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIFactory.W11_BORDER),
                new EmptyBorder(6, 0, 2, 0)));
        return l;
    }

    private void addRow(JPanel f, GridBagConstraints c, int row, String lbl, Component field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        f.add(UIFactory.labelLight(lbl), c);
        c.gridx = 1; c.weightx = 1; f.add(field, c);
    }

    private void addRow(JPanel f, GridBagConstraints c, int row, JLabel lbl, Component field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0; f.add(lbl, c);
        c.gridx = 1; c.weightx = 1; f.add(field, c);
    }

    private void addRow3d(JPanel f, GridBagConstraints c, int row, String lbl, Component field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        f.add(UIFactory.labelLight(lbl), c);
        c.gridx = 1; c.weightx = 1; f.add(field, c);
    }

    private FocusAdapter fl(Runnable r) {
        return new FocusAdapter() {
            public void focusLost(FocusEvent e) { r.run(); }
        };
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
