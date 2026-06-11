package br.carmel.ui.panels;

import br.carmel.model.*;
import br.carmel.service.PedidoService;
import br.carmel.service.RegraNegocioException;
import br.carmel.service.ServiceLocator;
import br.carmel.util.UIFactory;
import br.carmel.util.Validator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Painel de Emissão de Nota — atende pedidos PENDENTES.
 * Toda lógica delegada ao PedidoService e CaixaService.
 */
public class EmitirNotaPanel extends JPanel {

    private final ServiceLocator services;
    private CaixaPanel caixaPanel;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DefaultTableModel pendentesModel;
    private JTable pendentesTable;
    private Long pedidoSelecionadoId = null;

    private DefaultTableModel itensModel;
    private JTable itensTable;

    private JLabel lblPedidoInfo, lblSubtotal, lblDesconto, lblTotal;
    private JTextField tfDesconto;
    private JComboBox<String> cbTipoDesconto;

    private BigDecimal descontoAplicado = BigDecimal.ZERO;
    private BigDecimal subtotalAtual    = BigDecimal.ZERO;

    public void setCaixaPanel(CaixaPanel cp) { this.caixaPanel = cp; }

    public EmitirNotaPanel(ServiceLocator services) {
        this.services = services;
        setLayout(new BorderLayout());
        setBackground(UIFactory.XP_BG);
        build();
    }

    public void reloadPendentes() {
        pedidoSelecionadoId = null;
        itensModel.setRowCount(0);
        lblPedidoInfo.setText("Selecione um pedido pendente na lista acima");
        subtotalAtual = BigDecimal.ZERO;
        descontoAplicado = BigDecimal.ZERO;
        tfDesconto.setText("0");
        atualizarLabels();
        carregarPendentes();
    }

    // ── Montagem ──────────────────────────────────────────────────────────────

    private void build() {
        add(UIFactory.xpTitleBar("Emitir Nota — Atendimento de Pedidos"), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildPendentesPanel(), buildAtendimentoPanel());
        split.setDividerLocation(200);
        split.setDividerSize(5);
        split.setBackground(UIFactory.XP_BG);
        add(split, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildPendentesPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 8, 4, 8));

        JLabel lbl = new JLabel("Pedidos Pendentes — Aguardando Atendimento");
        lbl.setFont(UIFactory.FONT_BOLD);
        lbl.setForeground(new Color(180, 60, 0));
        lbl.setBorder(new EmptyBorder(0, 0, 4, 0));
        p.add(lbl, BorderLayout.NORTH);

        pendentesModel = new DefaultTableModel(
                new String[]{"#","Data","Cliente","Itens","Total (R$)","Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        pendentesTable = UIFactory.styledTable();
        pendentesTable.setModel(pendentesModel);
        pendentesTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        pendentesTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        pendentesTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        pendentesTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        pendentesTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        pendentesTable.getColumnModel().getColumn(5).setPreferredWidth(90);

        pendentesTable.setDefaultRenderer(Object.class, (t, value, isSel, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(UIFactory.FONT_NORMAL); cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(2, 5, 2, 5));
            cell.setBackground(isSel ? UIFactory.XP_TABLE_SEL : new Color(255, 245, 220));
            cell.setForeground(isSel ? Color.WHITE : Color.BLACK);
            if (col == 4) cell.setHorizontalAlignment(SwingConstants.RIGHT);
            return cell;
        });

        pendentesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && pendentesTable.getSelectedRow() >= 0) {
                Long id = (Long) pendentesModel.getValueAt(pendentesTable.getSelectedRow(), 0);
                carregarPedido(id);
            }
        });

        JScrollPane scroll = new JScrollPane(pendentesTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        p.add(scroll, BorderLayout.CENTER);

        JPanel rod = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        rod.setBackground(new Color(212, 208, 200));
        rod.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));
        rod.add(UIFactory.bigActionButton("↻ Atualizar", e -> reloadPendentes()));
        p.add(rod, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildAtendimentoPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(4, 8, 8, 8));

        // Esquerda: itens somente leitura
        JPanel esquerda = new JPanel(new BorderLayout(0, 4));
        esquerda.setBackground(UIFactory.XP_BG);
        esquerda.setBorder(UIFactory.groupBorder("Itens do Pedido"));

        lblPedidoInfo = new JLabel("Selecione um pedido pendente na lista acima");
        lblPedidoInfo.setFont(UIFactory.FONT_BOLD);
        lblPedidoInfo.setForeground(new Color(0, 84, 166));
        lblPedidoInfo.setBorder(new EmptyBorder(2, 4, 4, 4));
        esquerda.add(lblPedidoInfo, BorderLayout.NORTH);

        itensModel = new DefaultTableModel(
                new String[]{"ID","Produto","Cód. Barras","Qtd","Preço Unit.","Subtotal"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        itensTable = UIFactory.styledTable();
        itensTable.setModel(itensModel);
        itensTable.getColumnModel().getColumn(0).setMaxWidth(0);
        itensTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        itensTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        itensTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        itensTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        itensTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        itensTable.getColumnModel().getColumn(5).setPreferredWidth(90);

        JScrollPane scrollItens = new JScrollPane(itensTable);
        scrollItens.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        esquerda.add(scrollItens, BorderLayout.CENTER);
        p.add(esquerda, BorderLayout.CENTER);

        // Direita: desconto + totais + botões
        JPanel direita = new JPanel(new BorderLayout(0, 6));
        direita.setBackground(UIFactory.XP_BG);
        direita.setPreferredSize(new Dimension(240, 0));

        // Desconto
        JPanel descPanel = new JPanel(new GridBagLayout());
        descPanel.setBackground(UIFactory.XP_BG);
        descPanel.setBorder(UIFactory.groupBorder("Desconto"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6); gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0; descPanel.add(UIFactory.labelLight("Tipo:"), gc);
        cbTipoDesconto = new JComboBox<>(new String[]{"R$ (Reais)","% (Percentual)"});
        cbTipoDesconto.setFont(UIFactory.FONT_NORMAL);
        gc.gridx = 1; gc.weightx = 1; descPanel.add(cbTipoDesconto, gc);

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; descPanel.add(UIFactory.labelLight("Valor:"), gc);
        tfDesconto = UIFactory.styledField("0");
        gc.gridx = 1; gc.weightx = 1; descPanel.add(tfDesconto, gc);

        JButton btnAplicar = UIFactory.bigActionButton("✔ Aplicar", e -> aplicarDesconto());
        btnAplicar.setBackground(new Color(0, 84, 166)); btnAplicar.setForeground(Color.WHITE);
        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 2; descPanel.add(btnAplicar, gc);
        tfDesconto.addActionListener(e -> aplicarDesconto());
        direita.add(descPanel, BorderLayout.NORTH);

        // Totais
        JPanel totaisPanel = new JPanel(new GridBagLayout());
        totaisPanel.setBackground(UIFactory.XP_BG);
        totaisPanel.setBorder(UIFactory.groupBorder("Resumo"));
        GridBagConstraints gt = new GridBagConstraints();
        gt.insets = new Insets(4, 8, 4, 8); gt.fill = GridBagConstraints.HORIZONTAL;

        lblSubtotal = new JLabel("R$ 0,00"); lblSubtotal.setHorizontalAlignment(SwingConstants.RIGHT);
        gt.gridx = 0; gt.gridy = 0; gt.weightx = 0; totaisPanel.add(UIFactory.labelLight("Subtotal:"), gt);
        gt.gridx = 1; gt.weightx = 1; totaisPanel.add(lblSubtotal, gt);

        lblDesconto = new JLabel("R$ 0,00");
        lblDesconto.setFont(UIFactory.FONT_BOLD); lblDesconto.setForeground(new Color(180, 0, 0));
        lblDesconto.setHorizontalAlignment(SwingConstants.RIGHT);
        gt.gridx = 0; gt.gridy = 1; gt.weightx = 0; totaisPanel.add(UIFactory.labelLight("Desconto:"), gt);
        gt.gridx = 1; gt.weightx = 1; totaisPanel.add(lblDesconto, gt);

        JSeparator sep = new JSeparator();
        GridBagConstraints sepGc = new GridBagConstraints();
        sepGc.gridx = 0; sepGc.gridy = 2; sepGc.gridwidth = 2; sepGc.fill = GridBagConstraints.HORIZONTAL;
        sepGc.insets = new Insets(2, 8, 2, 8);
        totaisPanel.add(sep, sepGc);

        lblTotal = new JLabel("R$ 0,00");
        lblTotal.setFont(new Font("Tahoma", Font.BOLD, 16));
        lblTotal.setForeground(new Color(0, 84, 166));
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        gt.gridx = 0; gt.gridy = 3; gt.weightx = 0;
        totaisPanel.add(new JLabel("TOTAL:") {{ setFont(UIFactory.FONT_BOLD); }}, gt);
        gt.gridx = 1; gt.weightx = 1; totaisPanel.add(lblTotal, gt);
        direita.add(totaisPanel, BorderLayout.CENTER);

        // Botões
        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 0, 6));
        btnPanel.setOpaque(false); btnPanel.setBorder(new EmptyBorder(0, 4, 4, 4));

        JButton btnEmitir = UIFactory.bigActionButton("✔ Emitir Nota / Pagar", e -> emitirNota());
        btnEmitir.setBackground(new Color(0, 110, 0)); btnEmitir.setForeground(Color.WHITE);
        btnEmitir.setFont(new Font("Tahoma", Font.BOLD, 12));

        JButton btnCancelar = UIFactory.bigActionButton("✖ Cancelar Pedido", e -> cancelarPedido());
        btnCancelar.setBackground(new Color(180, 0, 0)); btnCancelar.setForeground(Color.WHITE);

        btnPanel.add(btnEmitir); btnPanel.add(btnCancelar);
        direita.add(btnPanel, BorderLayout.SOUTH);
        p.add(direita, BorderLayout.EAST);
        return p;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(new Color(212, 208, 200));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));
        JLabel h = new JLabel("Dica: Selecione um pedido → aplique desconto se necessário → Emitir Nota para cobrar e débitar estoque.");
        h.setFont(new Font("Tahoma", Font.ITALIC, 10)); h.setForeground(new Color(80, 80, 80));
        bar.add(h);
        return bar;
    }

    // ── Carregar dados ────────────────────────────────────────────────────────

    private void carregarPendentes() {
        pendentesModel.setRowCount(0);
        services.pedidos().listarPendentes().forEach(ped -> {
            pendentesModel.addRow(new Object[]{
                    ped.getId(),
                    ped.getDataPedido().format(FMT),
                    ped.getCliente().getNome(),
                    ped.getItensPedidos() != null ? ped.getItensPedidos().size() : 0,
                    String.format("R$ %.2f", ped.getValorTotal() != null ? ped.getValorTotal() : BigDecimal.ZERO),
                    ped.getStatus().toString()
            });
        });
    }

    private void carregarPedido(Long pedidoId) {
        pedidoSelecionadoId = pedidoId;
        itensModel.setRowCount(0);
        descontoAplicado = BigDecimal.ZERO;
        tfDesconto.setText("0");

        try {
            Pedido ped = services.pedidos().buscarComItens(pedidoId);
            lblPedidoInfo.setText("Pedido #" + ped.getId() + " — " + ped.getCliente().getNome()
                    + "  |  " + ped.getDataPedido().format(FMT));

            subtotalAtual = BigDecimal.ZERO;
            for (ItensPedido item : ped.getItensPedidos()) {
                BigDecimal sub = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
                subtotalAtual = subtotalAtual.add(sub);
                itensModel.addRow(new Object[]{
                        item.getId(), item.getProduto().getNome(),
                        item.getProduto().getCodBarras() != null ? item.getProduto().getCodBarras() : "—",
                        item.getQuantidade(),
                        String.format("R$ %.2f", item.getPrecoUnitario()),
                        String.format("R$ %.2f", sub)
                });
            }
            atualizarLabels();
        } catch (RegraNegocioException e) { showError(e.getMessage()); }
    }

    // ── Desconto ──────────────────────────────────────────────────────────────

    private void aplicarDesconto() {
        if (pedidoSelecionadoId == null) return;
        try {
            BigDecimal val = Validator.parseBigDecimal(tfDesconto.getText());
            if (val.compareTo(BigDecimal.ZERO) < 0) { showError("Desconto não pode ser negativo."); return; }
            boolean pct = cbTipoDesconto.getSelectedIndex() == 1;
            if (pct) {
                if (val.compareTo(new BigDecimal("100")) > 0) { showError("Desconto não pode ser maior que 100%."); return; }
                descontoAplicado = subtotalAtual.multiply(val)
                        .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            } else {
                if (val.compareTo(subtotalAtual) > 0) { showError("Desconto não pode ser maior que o subtotal."); return; }
                descontoAplicado = val;
            }
            atualizarLabels();
        } catch (Exception e) { showError("Valor de desconto inválido."); }
    }

    private void atualizarLabels() {
        BigDecimal total = subtotalAtual.subtract(descontoAplicado).max(BigDecimal.ZERO);
        lblSubtotal.setText(String.format("R$ %.2f", subtotalAtual));
        lblDesconto.setText(descontoAplicado.compareTo(BigDecimal.ZERO) > 0
                ? "- R$ " + String.format("%.2f", descontoAplicado) : "R$ 0,00");
        lblTotal.setText(String.format("R$ %.2f", total));
        lblTotal.setForeground(descontoAplicado.compareTo(BigDecimal.ZERO) > 0
                ? new Color(0, 130, 0) : new Color(0, 84, 166));
    }

    // ── Emitir Nota ───────────────────────────────────────────────────────────

    private void emitirNota() {
        if (pedidoSelecionadoId == null) {
            JOptionPane.showMessageDialog(this, "Selecione um pedido pendente.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal totalFinal = subtotalAtual.subtract(descontoAplicado).max(BigDecimal.ZERO);

        // Diálogo de pagamento
        JPanel dlg = new JPanel(new GridBagLayout());
        dlg.setBackground(UIFactory.XP_BG);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 8, 5, 8); gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblSub = new JLabel("Subtotal: R$ " + String.format("%.2f", subtotalAtual));
        lblSub.setFont(UIFactory.FONT_NORMAL);
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2; dlg.add(lblSub, gc);

        if (descontoAplicado.compareTo(BigDecimal.ZERO) > 0) {
            JLabel lblD = new JLabel("Desconto: - R$ " + String.format("%.2f", descontoAplicado));
            lblD.setFont(UIFactory.FONT_BOLD); lblD.setForeground(new Color(180, 0, 0));
            gc.gridy = 1; dlg.add(lblD, gc);
        }

        JLabel lblTot = new JLabel("TOTAL: R$ " + String.format("%.2f", totalFinal));
        lblTot.setFont(new Font("Tahoma", Font.BOLD, 14)); lblTot.setForeground(new Color(0, 110, 0));
        gc.gridy = 2; dlg.add(lblTot, gc);

        gc.gridwidth = 1;
        gc.gridx = 0; gc.gridy = 3; dlg.add(UIFactory.labelLight("Forma de pagamento:"), gc);
        JComboBox<FormaPagamento> cbForma = new JComboBox<>(FormaPagamento.values());
        cbForma.setFont(UIFactory.FONT_NORMAL);
        gc.gridx = 1; dlg.add(cbForma, gc);

        gc.gridx = 0; gc.gridy = 4; dlg.add(UIFactory.labelLight("Valor recebido (R$):"), gc);
        JTextField tfPago = UIFactory.styledField(String.format("%.2f", totalFinal));
        gc.gridx = 1; dlg.add(tfPago, gc);

        JLabel lblTroco = new JLabel("Troco: R$ 0,00");
        lblTroco.setFont(UIFactory.FONT_BOLD); lblTroco.setForeground(new Color(0, 100, 0));
        gc.gridx = 0; gc.gridy = 5; gc.gridwidth = 2; dlg.add(lblTroco, gc);

        final BigDecimal tf = totalFinal;
        tfPago.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void up() {
                try {
                    BigDecimal tr = Validator.parseBigDecimal(tfPago.getText()).subtract(tf);
                    lblTroco.setText("Troco: R$ " + String.format("%.2f", tr.max(BigDecimal.ZERO)));
                    lblTroco.setForeground(tr.compareTo(BigDecimal.ZERO) >= 0 ? new Color(0,100,0) : new Color(180,0,0));
                } catch (Exception e) { lblTroco.setText("Troco: -"); }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { up(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { up(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { up(); }
        });

        int res = JOptionPane.showConfirmDialog(this, dlg, "Registrar Pagamento",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        try {
            BigDecimal valorPago = Validator.parseBigDecimal(tfPago.getText());
            FormaPagamento forma = (FormaPagamento) cbForma.getSelectedItem();
            boolean caixaAberto = caixaPanel != null && caixaPanel.isCaixaAberto();

            // Delega toda a lógica ao PedidoService
            PedidoService.ResultadoEmissao resultado = services.pedidos().emitirNota(
                    pedidoSelecionadoId, descontoAplicado, forma, valorPago, caixaAberto);

            // Registra no caixa
            if (caixaPanel != null) caixaPanel.registrarVenda(resultado.pagamento);

            String msg = "✅ Nota emitida!\n"
                    + "Subtotal: R$ " + String.format("%.2f", subtotalAtual);
            if (descontoAplicado.compareTo(BigDecimal.ZERO) > 0)
                msg += "\nDesconto: - R$ " + String.format("%.2f", descontoAplicado);
            msg += "\nTotal cobrado: R$ " + String.format("%.2f", resultado.totalFinal)
                    + "\nTroco: R$ " + String.format("%.2f", resultado.troco);
            JOptionPane.showMessageDialog(this, msg, "Nota Emitida", JOptionPane.INFORMATION_MESSAGE);
            reloadPendentes();

        } catch (RegraNegocioException e) { showError(e.getMessage()); }
        catch (Exception ex) { showError("Erro ao emitir nota: " + ex.getMessage()); ex.printStackTrace(); }
    }

    private void cancelarPedido() {
        if (pedidoSelecionadoId == null) { JOptionPane.showMessageDialog(this, "Selecione um pedido."); return; }
        if (JOptionPane.showConfirmDialog(this,
                "Confirma o CANCELAMENTO do Pedido #" + pedidoSelecionadoId + "?",
                "Cancelar Pedido", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        try {
            services.pedidos().cancelarPedido(pedidoSelecionadoId);
            JOptionPane.showMessageDialog(this, "Pedido #" + pedidoSelecionadoId + " cancelado.");
            reloadPendentes();
        } catch (RegraNegocioException e) { showError(e.getMessage()); }
    }

    private void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE); }
}