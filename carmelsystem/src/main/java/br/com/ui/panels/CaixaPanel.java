package br.carmel.ui.panels;

import br.carmel.model.*;
import br.carmel.service.CaixaService;
import br.carmel.service.RegraNegocioException;
import br.carmel.service.ServiceLocator;
import br.carmel.util.UIFactory;
import br.carmel.util.Validator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CaixaPanel extends JPanel {

    private final ServiceLocator services;

    private static final DateTimeFormatter FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    
    private CaixaService.EstadoCaixa estadoAtual;

    
    private JLabel lblStatus, lblSaldo, lblCaixaNum;
    private JButton btnAbrirCaixa, btnFecharCaixa, btnSangria, btnSuprimento, btnVerFechamento;

    
    private DefaultTableModel movModel, resumoModel, fechadosModel;

    
    private JTextField tfFiltroDe, tfFiltroAte;

    public CaixaPanel(ServiceLocator services) {
        this.services = services;
        setLayout(new BorderLayout());
        setBackground(UIFactory.XP_BG);
        build();
    }

    public boolean isCaixaAberto() {
        return estadoAtual != null && estadoAtual.aberto;
    }

    public void reloadCaixa() {
        estadoAtual = services.caixa().calcularEstado();
        atualizarStatus();
        reloadMovimentos();
        reloadResumo();
        reloadCaixasFechados();
    }

    

    private void build() {
        add(UIFactory.xpTitleBar("Caixa"), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIFactory.FONT_BOLD);
        tabs.setBackground(UIFactory.XP_BG);
        tabs.addTab("Caixa Atual",      buildAtualPanel());
        tabs.addTab("Caixas Fechados",  buildFechadosPanel());
        add(tabs, BorderLayout.CENTER);
        add(buildStatusBarHint(), BorderLayout.SOUTH);
    }

    private JPanel buildAtualPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 8, 8, 8));
        p.add(buildTopPanel(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildMovimentosPanel(), buildResumoPanel());
        split.setDividerLocation(620);
        split.setDividerSize(5);
        split.setBackground(UIFactory.XP_BG);
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildTopPanel() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(UIFactory.groupBorder("Status do Caixa"));

        JPanel info = new JPanel(new GridLayout(3, 1, 2, 2));
        info.setOpaque(false);

        lblStatus = new JLabel("● NENHUM CAIXA ABERTO");
        lblStatus.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblStatus.setForeground(new Color(180, 100, 0));

        lblSaldo = new JLabel("Saldo: R$ 0,00");
        lblSaldo.setFont(UIFactory.FONT_BOLD);
        lblSaldo.setForeground(new Color(120, 120, 120));

        lblCaixaNum = new JLabel("Caixa: —");
        lblCaixaNum.setFont(UIFactory.FONT_NORMAL);

        info.add(lblStatus);
        info.add(lblSaldo);
        info.add(lblCaixaNum);
        p.add(info, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        btns.setOpaque(false);
        btnAbrirCaixa    = UIFactory.bigActionButton("Abrir Caixa",        e -> abrirCaixa());
        btnFecharCaixa   = UIFactory.bigActionButton("Fechar Caixa",       e -> fecharCaixa());
        btnSangria       = UIFactory.bigActionButton("Sangria",             e -> sangria());
        btnSuprimento    = UIFactory.bigActionButton("Suprimento",          e -> suprimento());
        btnVerFechamento = UIFactory.bigActionButton("🖨 Rel. Fechamento",  e -> abrirRelatorioAtual());
        JButton btnAtualizar = UIFactory.bigActionButton("Atualizar",       e -> reloadCaixa());

        btnAbrirCaixa.setBackground(new Color(0, 128, 0));   btnAbrirCaixa.setForeground(Color.WHITE);
        btnFecharCaixa.setBackground(new Color(180, 0, 0));  btnFecharCaixa.setForeground(Color.WHITE);
        btnVerFechamento.setBackground(new Color(0, 84, 166)); btnVerFechamento.setForeground(Color.WHITE);

        btns.add(btnAbrirCaixa); btns.add(btnFecharCaixa);
        btns.add(btnSangria);    btns.add(btnSuprimento);
        btns.add(btnAtualizar);  btns.add(btnVerFechamento);
        p.add(btns, BorderLayout.EAST);
        p.setPreferredSize(new Dimension(0, 90));
        return p;
    }

    private JPanel buildMovimentosPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(0, 0, 0, 4));
        JLabel lbl = new JLabel("Movimentos do Caixa");
        lbl.setFont(UIFactory.FONT_BOLD); lbl.setForeground(new Color(0, 84, 166));
        lbl.setBorder(new EmptyBorder(4, 0, 4, 0));
        p.add(lbl, BorderLayout.NORTH);

        movModel = new DefaultTableModel(
                new String[]{"Hora", "Tipo", "Descrição", "Valor (R$)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = UIFactory.styledTable();
        t.setModel(movModel);
        t.getColumnModel().getColumn(0).setPreferredWidth(80);
        t.getColumnModel().getColumn(1).setPreferredWidth(100);
        t.getColumnModel().getColumn(2).setPreferredWidth(300);
        t.getColumnModel().getColumn(3).setPreferredWidth(100);

        t.setDefaultRenderer(Object.class, (tbl, value, isSel, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(UIFactory.FONT_NORMAL);
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(1, 4, 1, 4));
            if (isSel) { cell.setBackground(UIFactory.XP_TABLE_SEL); cell.setForeground(Color.WHITE); }
            else {
                String tipo = movModel.getValueAt(row, 1) != null ? movModel.getValueAt(row, 1).toString() : "";
                cell.setForeground(Color.BLACK);
                cell.setBackground(switch (tipo) {
                    case "Abertura"   -> new Color(220, 240, 220);
                    case "Venda"      -> new Color(220, 235, 255);
                    case "Sangria"    -> new Color(255, 220, 220);
                    case "Suprimento" -> new Color(255, 245, 200);
                    case "Fechamento" -> new Color(230, 230, 230);
                    default -> Color.WHITE;
                });
            }
            return cell;
        });

        JScrollPane scroll = new JScrollPane(t);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildResumoPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UIFactory.XP_BG);
        JLabel lbl = new JLabel("Resumo do Caixa");
        lbl.setFont(UIFactory.FONT_BOLD); lbl.setForeground(new Color(0, 84, 166));
        lbl.setBorder(new EmptyBorder(4, 0, 4, 0));
        p.add(lbl, BorderLayout.NORTH);

        resumoModel = new DefaultTableModel(
                new String[]{"Forma de Pagamento", "Total (R$)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = UIFactory.styledTable();
        t.setModel(resumoModel);
        JScrollPane scroll = new JScrollPane(t);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildFechadosPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filtros.setBackground(UIFactory.XP_PANEL_BG);
        filtros.setBorder(UIFactory.groupBorder("Pesquisar Caixas Fechados"));
        filtros.add(UIFactory.labelLight("Caixa de:"));
        tfFiltroDe  = UIFactory.styledField(""); tfFiltroDe.setPreferredSize(new Dimension(80, 22));
        filtros.add(tfFiltroDe);
        filtros.add(UIFactory.labelLight("até:"));
        tfFiltroAte = UIFactory.styledField(""); tfFiltroAte.setPreferredSize(new Dimension(80, 22));
        filtros.add(tfFiltroAte);
        JButton btnBuscar = UIFactory.bigActionButton("🔍 Buscar", e -> reloadCaixasFechados());
        JButton btnLimpar = UIFactory.bigActionButton("Limpar",    e -> { tfFiltroDe.setText(""); tfFiltroAte.setText(""); reloadCaixasFechados(); });
        btnBuscar.setBackground(new Color(0, 84, 166)); btnBuscar.setForeground(Color.WHITE);
        filtros.add(btnBuscar); filtros.add(btnLimpar);
        p.add(filtros, BorderLayout.NORTH);

        fechadosModel = new DefaultTableModel(
                new String[]{"Caixa #", "Data Abertura", "Data Fechamento",
                        "Abertura (R$)", "Vendas (R$)", "Sangrias (R$)", "Suprimentos (R$)",
                        "Sistema (R$)", "Informado (R$)", "Diferença (R$)", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable t = UIFactory.styledTable();
        t.setModel(fechadosModel);
        t.getColumnModel().getColumn(0).setPreferredWidth(65);   
        t.getColumnModel().getColumn(1).setPreferredWidth(120);  
        t.getColumnModel().getColumn(2).setPreferredWidth(120);  
        for (int i = 3; i <= 9; i++) t.getColumnModel().getColumn(i).setPreferredWidth(100);
        t.getColumnModel().getColumn(10).setPreferredWidth(90);  

        t.setDefaultRenderer(Object.class, (tbl, value, isSel, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(col == 10 ? UIFactory.FONT_BOLD : UIFactory.FONT_NORMAL);
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(1, 5, 1, 5));
            if (col >= 3) cell.setHorizontalAlignment(SwingConstants.RIGHT);
            if (col == 0) cell.setHorizontalAlignment(SwingConstants.CENTER);
            if (isSel) { cell.setBackground(UIFactory.XP_TABLE_SEL); cell.setForeground(Color.WHITE); }
            else {
                String st = fechadosModel.getValueAt(row, 10) != null ? fechadosModel.getValueAt(row, 10).toString() : "";
                cell.setBackground("✅ Correto".equals(st)   ? new Color(220, 255, 220)
                        : "⚠ Diferença".equals(st) ? new Color(255, 240, 200)
                        : (row % 2 == 0 ? Color.WHITE : new Color(248, 248, 252)));
                cell.setForeground(col == 9  
                        ? ("✅ Correto".equals(st) ? new Color(0, 110, 0) : new Color(160, 60, 0))
                        : Color.BLACK);
            }
            return cell;
        });

        t.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && t.getSelectedRow() >= 0) {
                    try {
                        Long num = Long.parseLong(fechadosModel.getValueAt(t.getSelectedRow(), 0).toString().replace("#", "").trim());
                        abrirRelatorioCaixaNumero(num);
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(t);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        p.add(scroll, BorderLayout.CENTER);

        JPanel rod = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        rod.setBackground(new Color(212, 208, 200));
        rod.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));
        rod.add(UIFactory.bigActionButton("↻ Atualizar", e -> reloadCaixasFechados()));
        JButton btnVer = UIFactory.bigActionButton("🖨 Ver / Imprimir", e -> {
            if (t.getSelectedRow() < 0) { JOptionPane.showMessageDialog(p, "Selecione um caixa."); return; }
            try {
                Long num = Long.parseLong(fechadosModel.getValueAt(t.getSelectedRow(), 0).toString().replace("#", "").trim());
                abrirRelatorioCaixaNumero(num);
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        btnVer.setBackground(new Color(0, 84, 166)); btnVer.setForeground(Color.WHITE);
        rod.add(btnVer);
        p.add(rod, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildStatusBarHint() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(new Color(212, 208, 200));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));
        JLabel h = new JLabel("Dica: Cada abertura gera um novo Caixa #N. O caixa deve estar aberto para registrar vendas.");
        h.setFont(new Font("Tahoma", Font.ITALIC, 10));
        h.setForeground(new Color(80, 80, 80));
        bar.add(h);
        return bar;
    }

    

    private void abrirCaixa() {
        String s = JOptionPane.showInputDialog(this, "Valor inicial do caixa (R$):", "Abrir Caixa", JOptionPane.QUESTION_MESSAGE);
        if (Validator.isBlank(s)) return;
        try {
            services.caixa().abrirCaixa(Validator.parseBigDecimal(s));
            CaixaService.EstadoCaixa novo = services.caixa().calcularEstado();
            JOptionPane.showMessageDialog(this,
                    "Caixa #" + (novo.numeroCaixa != null ? novo.numeroCaixa : "?") + " aberto com R$ " + s);
            reloadCaixa();
        } catch (RegraNegocioException e) { showError(e.getMessage()); }
    }

    private void fecharCaixa() {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Fechar Caixa",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(420, 210); dlg.setLocationRelativeTo(this); dlg.setResizable(false);
        dlg.setLayout(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG); form.setBorder(new EmptyBorder(14, 16, 8, 16));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(7, 6, 7, 6); gc.fill = GridBagConstraints.HORIZONTAL;

        String numStr = estadoAtual != null && estadoAtual.numeroCaixa != null
                ? " — Caixa #" + estadoAtual.numeroCaixa : "";
        JLabel aviso = new JLabel("Informe o valor contado fisicamente" + numStr + ":");
        aviso.setFont(UIFactory.FONT_BOLD); aviso.setForeground(new Color(0, 84, 166));
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2; form.add(aviso, gc);

        JLabel sub = new JLabel("O sistema verificará se o valor está correto após o fechamento.");
        sub.setFont(new Font("Tahoma", Font.ITALIC, 10)); sub.setForeground(Color.GRAY);
        gc.gridy = 1; form.add(sub, gc);
        gc.gridwidth = 1;
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; form.add(UIFactory.labelLight("Valor no caixa (R$):"), gc);
        JTextField tfVal = UIFactory.styledField(""); tfVal.setPreferredSize(new Dimension(160, 24));
        gc.gridx = 1; gc.weightx = 1; form.add(tfVal, gc);
        dlg.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setBackground(UIFactory.XP_BG);
        boolean[] ok = {false};
        JButton btnOk = UIFactory.bigActionButton("Fechar Caixa", e -> { ok[0] = true; dlg.dispose(); });
        btnOk.setBackground(new Color(180, 0, 0)); btnOk.setForeground(Color.WHITE);
        btns.add(btnOk); btns.add(UIFactory.bigActionButton("Cancelar", e -> dlg.dispose()));
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.getRootPane().setDefaultButton(btnOk);
        SwingUtilities.invokeLater(tfVal::requestFocusInWindow);
        dlg.setVisible(true);
        if (!ok[0]) return;

        try {
            BigDecimal valorInformado = Validator.parseBigDecimal(tfVal.getText());
            BigDecimal diferenca      = services.caixa().fecharCaixa(valorInformado);
            BigDecimal saldoSistema   = estadoAtual.saldoAtual;
            boolean correto = diferenca.abs().compareTo(new BigDecimal("0.01")) <= 0;
            String nCaixa = estadoAtual.numeroCaixa != null ? "#" + estadoAtual.numeroCaixa : "";
            String msg = (correto ? "✅ Caixa " + nCaixa + " fechado corretamente!\n\n"
                                  : "⚠ Caixa " + nCaixa + " fechado com diferença!\n\n")
                    + "Valor do sistema:    R$ " + String.format("%.2f", saldoSistema)
                    + "\nValor informado: R$ " + String.format("%.2f", valorInformado)
                    + "\nDiferença:           R$ " + String.format("%.2f", diferenca)
                    + (correto ? "" : "\n" + (diferenca.compareTo(BigDecimal.ZERO) > 0 ? "⬆ Sobrou dinheiro." : "⬇ Faltou dinheiro."));
            JOptionPane.showMessageDialog(this, msg, "Resultado do Fechamento",
                    correto ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            reloadCaixa();
        } catch (RegraNegocioException e) { showError(e.getMessage()); }
    }

    private void sangria() {
        String s = JOptionPane.showInputDialog(this, "Valor da sangria (R$):", "Sangria", JOptionPane.QUESTION_MESSAGE);
        if (Validator.isBlank(s)) return;
        String desc = JOptionPane.showInputDialog(this, "Motivo (opcional):");
        try {
            services.caixa().registrarSangria(Validator.parseBigDecimal(s), desc);
            JOptionPane.showMessageDialog(this, "Sangria registrada.");
            reloadCaixa();
        } catch (RegraNegocioException e) { showError(e.getMessage()); }
    }

    private void suprimento() {
        String s = JOptionPane.showInputDialog(this, "Valor do suprimento (R$):", "Suprimento", JOptionPane.QUESTION_MESSAGE);
        if (Validator.isBlank(s)) return;
        String desc = JOptionPane.showInputDialog(this, "Motivo (opcional):");
        try {
            services.caixa().registrarSuprimento(Validator.parseBigDecimal(s), desc);
            JOptionPane.showMessageDialog(this, "Suprimento registrado.");
            reloadCaixa();
        } catch (RegraNegocioException e) { showError(e.getMessage()); }
    }

    

    public void registrarVenda(Pagamento pagamento) {
        try {
            services.caixa().registrarVenda(pagamento);
            reloadCaixa();
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    

    private void atualizarStatus() {
        if (estadoAtual == null) return;
        String nCaixa = estadoAtual.numeroCaixa != null ? "Caixa #" + estadoAtual.numeroCaixa : "—";
        if (estadoAtual.aberto) {
            lblStatus.setText("● CAIXA ABERTO"); lblStatus.setForeground(new Color(0, 130, 0));
            lblSaldo.setText("Saldo: (visível após fechamento)"); lblSaldo.setForeground(new Color(120, 120, 120));
        } else if (estadoAtual.algumFechamento) {
            lblStatus.setText("● CAIXA FECHADO"); lblStatus.setForeground(new Color(180, 0, 0));
            lblSaldo.setText("Saldo do caixa: R$ " + String.format("%.2f", estadoAtual.saldoAtual));
            lblSaldo.setForeground(new Color(0, 100, 0));
        } else {
            lblStatus.setText("● NENHUM CAIXA ABERTO"); lblStatus.setForeground(new Color(180, 100, 0));
            lblSaldo.setText("Saldo: R$ 0,00"); lblSaldo.setForeground(new Color(120, 120, 120));
        }
        lblCaixaNum.setText(nCaixa);
        btnAbrirCaixa.setEnabled(!estadoAtual.aberto);
        btnFecharCaixa.setEnabled(estadoAtual.aberto);
        btnSangria.setEnabled(estadoAtual.aberto);
        btnSuprimento.setEnabled(estadoAtual.aberto);
        btnVerFechamento.setEnabled(estadoAtual.algumFechamento);
    }

    private void reloadMovimentos() {
        movModel.setRowCount(0);
        if (estadoAtual == null || estadoAtual.numeroCaixa == null) return;

        
        Long numAtual = estadoAtual.numeroCaixa;
        services.caixa().buscarMovimentosPorNumero(numAtual).forEach(m -> {
            String desc = m.getDescricao();
            if (desc != null && desc.startsWith("FECHAMENTO|")) desc = "Fechamento de caixa";
            String valor = (estadoAtual.aberto &&
                    (m.getTipo() == TipoCaixaMovimento.VENDA || m.getTipo() == TipoCaixaMovimento.ABERTURA))
                    ? "****" : String.format("R$ %.2f", m.getValor());
            movModel.addRow(new Object[]{
                    m.getDataMovimento().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    m.getTipo().toString(), desc, valor
            });
        });
    }

    private void reloadResumo() {
        resumoModel.setRowCount(0);
        if (estadoAtual != null && estadoAtual.aberto) {
            resumoModel.addRow(new Object[]{"Valores ocultos até fechamento", ""});
            return;
        }
        if (estadoAtual == null || estadoAtual.inicioTurno == null) return;
        resumoModel.addRow(new Object[]{"Ver relatório completo →", "Rel. Fechamento"});
    }

    private void reloadCaixasFechados() {
        if (fechadosModel == null) return;
        fechadosModel.setRowCount(0);

        Long filtroDE = null, filtroATE = null;
        try { if (!Validator.isBlank(tfFiltroDe.getText()))  filtroDE  = Long.parseLong(tfFiltroDe.getText().trim()); }  catch (Exception ignored) {}
        try { if (!Validator.isBlank(tfFiltroAte.getText())) filtroATE = Long.parseLong(tfFiltroAte.getText().trim()); } catch (Exception ignored) {}

        final Long fDE = filtroDE, fATE = filtroATE;

        services.caixa().buscarNumerosCaixaFechados().forEach(numCaixa -> {
            if (fDE  != null && numCaixa < fDE)  return;
            if (fATE != null && numCaixa > fATE) return;

            List<CaixaMovimento> movs = services.caixa().buscarMovimentosPorNumero(numCaixa);
            BigDecimal ab = BigDecimal.ZERO, sg = BigDecimal.ZERO, sp = BigDecimal.ZERO;
            BigDecimal infVal = null, difVal = null;
            LocalDateTime horaAb = null, horaFech = null;
            String status = "—";

            for (CaixaMovimento m : movs) {
                switch (m.getTipo()) {
                    case ABERTURA   -> { ab = m.getValor(); horaAb = m.getDataMovimento(); }
                    case SANGRIA    -> sg = sg.add(m.getValor().abs());
                    case SUPRIMENTO -> sp = sp.add(m.getValor());
                    case FECHAMENTO -> {
                        horaFech = m.getDataMovimento();
                        String desc = m.getDescricao();
                        if (desc != null && desc.startsWith("FECHAMENTO|")) {
                            try {
                                for (String part : desc.split("\\|")) {
                                    if (part.startsWith("informado=")) infVal = new BigDecimal(part.substring(10));
                                    if (part.startsWith("diferenca=")) difVal = new BigDecimal(part.substring(10));
                                    if (part.startsWith("status="))   status = part.substring(7);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    default -> {}
                }
            }

            BigDecimal vendas = movs.stream()
                    .filter(m -> m.getTipo() == TipoCaixaMovimento.VENDA)
                    .map(CaixaMovimento::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal saldoSistema = ab.add(vendas).add(sp).subtract(sg);
            String stLbl = "CORRETO".equals(status) ? "✅ Correto" : "DIFERENCA".equals(status) ? "⚠ Diferença" : "—";

            fechadosModel.addRow(new Object[]{
                    "#" + numCaixa,
                    horaAb   != null ? horaAb.format(FMT)   : "—",
                    horaFech != null ? horaFech.format(FMT) : "—",
                    String.format("R$ %.2f", ab),
                    String.format("R$ %.2f", vendas),
                    String.format("R$ %.2f", sg),
                    String.format("R$ %.2f", sp),
                    String.format("R$ %.2f", saldoSistema),
                    infVal != null ? String.format("R$ %.2f", infVal) : "—",
                    difVal != null ? String.format("R$ %.2f", difVal) : "—",
                    stLbl
            });
        });
    }

    

    private void abrirRelatorioAtual() {
        if (estadoAtual == null || estadoAtual.numeroCaixa == null) return;
        abrirRelatorioCaixaNumero(estadoAtual.numeroCaixa);
    }

    private void abrirRelatorioCaixaNumero(Long numCaixa) {
        List<CaixaMovimento> movs = services.caixa().buscarMovimentosPorNumero(numCaixa);
        if (movs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum movimento para o Caixa #" + numCaixa);
            return;
        }

        BigDecimal abertura = BigDecimal.ZERO, sangrias = BigDecimal.ZERO, suprimentos = BigDecimal.ZERO;
        BigDecimal valorInformado = null, diferenca = null;
        String statusFech = null;
        LocalDateTime horaAb = null, horaFech = null;

        for (CaixaMovimento m : movs) {
            switch (m.getTipo()) {
                case ABERTURA   -> { abertura = m.getValor(); horaAb = m.getDataMovimento(); }
                case SANGRIA    -> sangrias    = sangrias.add(m.getValor().abs());
                case SUPRIMENTO -> suprimentos = suprimentos.add(m.getValor());
                case FECHAMENTO -> {
                    horaFech = m.getDataMovimento();
                    String desc = m.getDescricao();
                    if (desc != null && desc.startsWith("FECHAMENTO|")) {
                        try {
                            for (String pt : desc.split("\\|")) {
                                if (pt.startsWith("informado=")) valorInformado = new BigDecimal(pt.substring(10));
                                if (pt.startsWith("diferenca=")) diferenca      = new BigDecimal(pt.substring(10));
                                if (pt.startsWith("status="))   statusFech      = pt.substring(7);
                            }
                        } catch (Exception ignored) {}
                    }
                }
                default -> {}
            }
        }

        LocalDateTime fimTurno = horaFech != null ? horaFech : LocalDateTime.now();
        BigDecimal vendas = horaAb != null
                ? services.caixa().buscarTotalVendasPorTurno(horaAb, fimTurno)
                : BigDecimal.ZERO;
        BigDecimal saldoSistema = abertura.add(vendas).add(suprimentos).subtract(sangrias);

        mostrarRelatorioDialog(numCaixa, horaAb, horaFech, abertura, vendas, sangrias,
                suprimentos, saldoSistema, valorInformado, diferenca, statusFech, movs);
    }

    private void mostrarRelatorioDialog(Long numCaixa,
                                        LocalDateTime horaAb, LocalDateTime horaFech,
                                        BigDecimal abertura, BigDecimal vendas, BigDecimal sangrias,
                                        BigDecimal suprimentos, BigDecimal saldoSistema,
                                        BigDecimal valorInformado, BigDecimal diferenca, String statusFech,
                                        List<CaixaMovimento> movs) {

        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Relatório de Caixa #" + numCaixa,
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(580, 580); dlg.setLocationRelativeTo(this); dlg.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(16, 20, 16, 20));

        Font fTit  = new Font("Tahoma", Font.BOLD, 14);
        Font fSub  = new Font("Tahoma", Font.BOLD, 11);
        Font fNorm = new Font("Tahoma", Font.PLAIN, 10);
        Font fBold = new Font("Tahoma", Font.BOLD, 10);

        addL(content, "CARMEL SISTEMA DE GESTÃO", fTit, new Color(0, 60, 140), SwingConstants.CENTER);
        addL(content, "RELATÓRIO DE FECHAMENTO DE CAIXA", fSub, Color.BLACK, SwingConstants.CENTER);
        addL(content, "Caixa #" + numCaixa, fSub, new Color(0, 84, 166), SwingConstants.CENTER);
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        content.add(sep());

        String haStr = horaAb   != null ? horaAb.format(FMT)   : "—";
        String hfStr = horaFech != null ? horaFech.format(FMT) : "Em aberto";
        addL(content, "Abertura: " + haStr + "   |   Fechamento: " + hfStr, fNorm, Color.BLACK, SwingConstants.LEFT);
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        addL(content, "RESUMO FINANCEIRO", fSub, new Color(0, 84, 166), SwingConstants.LEFT);
        content.add(sep());
        addValor(content, "Valor de Abertura:",     abertura,    fNorm, Color.BLACK);
        addValor(content, "+ Vendas do caixa:",     vendas,      fNorm, new Color(0, 110, 0));
        addValor(content, "+ Suprimentos:",         suprimentos, fNorm, new Color(0, 110, 0));
        addValor(content, "- Sangrias:",            sangrias,    fNorm, new Color(180, 0, 0));
        content.add(sep());
        addValor(content, "= Saldo do Sistema:",   saldoSistema, fBold, new Color(0, 60, 140));

        if (valorInformado != null) {
            addValor(content, "Valor Informado:", valorInformado, fBold, Color.BLACK);
            boolean ok = diferenca != null && diferenca.abs().compareTo(new BigDecimal("0.01")) <= 0;
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            JLabel lbl = new JLabel("Diferença:"); lbl.setFont(fBold);
            JLabel val = new JLabel((diferenca != null ? String.format("R$ %.2f", diferenca) : "—")
                    + (ok ? "  ✅ CORRETO" : "  ⚠ DIFERENÇA"));
            val.setFont(fBold); val.setForeground(ok ? new Color(0, 110, 0) : new Color(180, 0, 0));
            val.setHorizontalAlignment(SwingConstants.RIGHT);
            row.add(lbl, BorderLayout.WEST); row.add(val, BorderLayout.EAST);
            content.add(row);
        }

        content.add(Box.createRigidArea(new Dimension(0, 10)));
        addL(content, "MOVIMENTOS", fSub, new Color(0, 84, 166), SwingConstants.LEFT);
        content.add(sep());
        for (CaixaMovimento m : movs) {
            String desc = m.getDescricao();
            if (desc != null && desc.startsWith("FECHAMENTO|")) desc = "Fechamento de caixa";
            JPanel row = new JPanel(new BorderLayout()); row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
            JLabel lHora = new JLabel(m.getDataMovimento().format(DateTimeFormatter.ofPattern("HH:mm"))
                    + "  [" + m.getTipo() + "]  " + desc);
            lHora.setFont(fNorm);
            JLabel lVal = new JLabel(String.format("R$ %.2f", m.getValor()));
            lVal.setFont(fNorm); lVal.setHorizontalAlignment(SwingConstants.RIGHT);
            row.add(lHora, BorderLayout.WEST); row.add(lVal, BorderLayout.EAST);
            content.add(row);
        }

        JScrollPane scroll = new JScrollPane(content); scroll.setBorder(null);
        dlg.add(scroll, BorderLayout.CENTER);
        JPanel rod = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        rod.setBackground(UIFactory.XP_BG);
        rod.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));
        JButton btnImpr = UIFactory.bigActionButton("🖨 Imprimir", null);
        btnImpr.setBackground(new Color(0, 84, 166)); btnImpr.setForeground(Color.WHITE);
        btnImpr.addActionListener(e -> imprimir(content, "Caixa #" + numCaixa));
        rod.add(btnImpr); rod.add(UIFactory.bigActionButton("Fechar", e2 -> dlg.dispose()));
        dlg.add(rod, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void imprimir(JPanel panel, String titulo) {
        panel.setSize(panel.getPreferredSize()); panel.doLayout();
        PrinterJob job = PrinterJob.getPrinterJob(); job.setJobName(titulo);
        job.setPrintable((g, pf, pi) -> {
            if (pi > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(pf.getImageableX(), pf.getImageableY());
            double scale = Math.min(pf.getImageableWidth() / panel.getWidth(), pf.getImageableHeight() / panel.getHeight());
            if (scale < 1.0) g2.scale(scale, scale);
            panel.printAll(g2);
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) { try { job.print(); } catch (PrinterException ex) { showError("Erro ao imprimir: " + ex.getMessage()); } }
    }

    

    private void addL(JPanel p, String t, Font f, Color c, int align) {
        JLabel l = new JLabel(t, align); l.setFont(f); l.setForeground(c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height + 4));
        p.add(l);
    }

    private void addValor(JPanel p, String label, BigDecimal valor, Font font, Color cor) {
        JPanel row = new JPanel(new BorderLayout()); row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        JLabel lbl = new JLabel(label); lbl.setFont(font);
        JLabel val = new JLabel(String.format("R$ %.2f", valor));
        val.setFont(font); val.setForeground(cor); val.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(lbl, BorderLayout.WEST); row.add(val, BorderLayout.EAST);
        p.add(row);
    }

    private JSeparator sep() {
        JSeparator s = new JSeparator(); s.setForeground(new Color(180, 180, 180));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); return s;
    }

    private void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE); }
}
