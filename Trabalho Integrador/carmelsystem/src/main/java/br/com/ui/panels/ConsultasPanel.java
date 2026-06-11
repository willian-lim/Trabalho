package br.carmel.ui.panels;

import br.carmel.model.*;
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

/**
 * Tela unificada de Consultas:
 * - Aba 1: Relatório de Vendas (notas emitidas), com cancelamento de nota
 * - Aba 2: Consulta de Pedidos, com cancelamento de pedido
 */
public class ConsultasPanel extends JPanel {

    private final ServiceLocator services;
    private static final DateTimeFormatter FMT_IN  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_OUT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Aba Vendas ─────────────────────────────────────────────────────────
    private DefaultTableModel vendasModel;
    private JTable vendasTable;
    private JTextField tfVendasDataIni, tfVendasDataFim, tfVendasCliente;
    private JLabel lblResumoVendas;

    // ── Aba Pedidos ────────────────────────────────────────────────────────
    private DefaultTableModel pedidosModel;
    private JTable pedidosTable;
    private DefaultTableModel itensPedidoModel;
    private JTextField tfPedidoCliente, tfPedidoDataDe, tfPedidoDataAte;
    private JComboBox<String> cbPedidoStatus;

    public ConsultasPanel(ServiceLocator services) {
        this.services = services;
        setLayout(new BorderLayout());
        setBackground(UIFactory.XP_BG);
        build();
    }

    private void build() {
        add(UIFactory.xpTitleBar("Consultas — Pedidos e Relatório de Vendas"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIFactory.FONT_BOLD);
        tabs.setBackground(UIFactory.XP_BG);
        tabs.addTab("📊 Relatório de Vendas (Notas)", buildAbaVendas());
        tabs.addTab("📋 Consulta de Pedidos", buildAbaPedidos());

        add(tabs, BorderLayout.CENTER);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ABA 1 — RELATÓRIO DE VENDAS
    // ══════════════════════════════════════════════════════════════════════

    private JPanel buildAbaVendas() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Filtros
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filtros.setBackground(UIFactory.XP_PANEL_BG);
        filtros.setBorder(UIFactory.groupBorder("Filtros de Período"));

        filtros.add(UIFactory.labelLight("Cliente:"));
        tfVendasCliente = UIFactory.styledField("");
        tfVendasCliente.setPreferredSize(new Dimension(150, 22));
        filtros.add(tfVendasCliente);

        filtros.add(UIFactory.labelLight("De:"));
        tfVendasDataIni = UIFactory.styledField("");
        tfVendasDataIni.setPreferredSize(new Dimension(95, 22));
        tfVendasDataIni.setToolTipText("dd/MM/yyyy");
        filtros.add(tfVendasDataIni);

        filtros.add(UIFactory.labelLight("Até:"));
        tfVendasDataFim = UIFactory.styledField("");
        tfVendasDataFim.setPreferredSize(new Dimension(95, 22));
        tfVendasDataFim.setToolTipText("dd/MM/yyyy");
        filtros.add(tfVendasDataFim);

        JButton btnBuscar = UIFactory.bigActionButton("🔍 Buscar", e -> buscarVendas());
        btnBuscar.setBackground(new Color(0, 84, 166)); btnBuscar.setForeground(Color.WHITE);
        filtros.add(btnBuscar);

        JButton btnHoje = UIFactory.bigActionButton("Hoje", e -> {
            String hoje = LocalDate.now().format(FMT_IN);
            tfVendasDataIni.setText(hoje); tfVendasDataFim.setText(hoje); buscarVendas();
        });
        filtros.add(btnHoje);

        JButton btnMes = UIFactory.bigActionButton("Este Mês", e -> {
            LocalDate ini = LocalDate.now().withDayOfMonth(1);
            tfVendasDataIni.setText(ini.format(FMT_IN));
            tfVendasDataFim.setText(LocalDate.now().format(FMT_IN));
            buscarVendas();
        });
        filtros.add(btnMes);

        filtros.add(UIFactory.bigActionButton("Limpar", e -> {
            tfVendasCliente.setText(""); tfVendasDataIni.setText(""); tfVendasDataFim.setText("");
            vendasModel.setRowCount(0); lblResumoVendas.setText("");
        }));
        p.add(filtros, BorderLayout.NORTH);

        // Tabela de vendas
        vendasModel = new DefaultTableModel(
                new String[]{"Pedido","Data/Hora","Cliente","Forma Pag.","Subtotal (R$)","Desconto (R$)","Total (R$)","ID_PAG"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) { return c == 7 ? Long.class : String.class; }
        };
        vendasTable = UIFactory.styledTable();
        vendasTable.setModel(vendasModel);
        vendasTable.setDefaultRenderer(Object.class, (t, val, isSel, hasFocus, row, col) -> {
            JLabel cell = new JLabel(val != null ? val.toString() : "");
            cell.setFont(UIFactory.FONT_NORMAL); cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(1, 4, 1, 4));
            if (isSel) { cell.setBackground(UIFactory.XP_TABLE_SEL); cell.setForeground(Color.WHITE); }
            else { cell.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245,245,250)); cell.setForeground(Color.BLACK); }
            if (col == 4 || col == 5 || col == 6) cell.setHorizontalAlignment(SwingConstants.RIGHT);
            return cell;
        });
        vendasTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        vendasTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        vendasTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        vendasTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        vendasTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        vendasTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        vendasTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        // Esconde coluna ID_PAG
        vendasTable.getColumnModel().getColumn(7).setMinWidth(0);
        vendasTable.getColumnModel().getColumn(7).setMaxWidth(0);
        vendasTable.getColumnModel().getColumn(7).setWidth(0);

        JScrollPane scroll = new JScrollPane(vendasTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        p.add(scroll, BorderLayout.CENTER);

        // Rodapé
        JPanel rodape = new JPanel(new BorderLayout(8, 0));
        rodape.setBackground(UIFactory.XP_PANEL_BG);
        rodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)),
                new EmptyBorder(4, 8, 4, 8)));

        lblResumoVendas = new JLabel("");
        lblResumoVendas.setFont(UIFactory.FONT_BOLD);
        lblResumoVendas.setForeground(new Color(0, 84, 166));
        rodape.add(lblResumoVendas, BorderLayout.WEST);

        JPanel botoesRodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        botoesRodape.setOpaque(false);

        JButton btnCancelarNota = UIFactory.bigActionButton("❌ Cancelar Nota Selecionada", e -> cancelarNotaSelecionada());
        btnCancelarNota.setBackground(new Color(180, 30, 30)); btnCancelarNota.setForeground(Color.WHITE);
        botoesRodape.add(btnCancelarNota);

        JButton btnImpr = UIFactory.bigActionButton("🖨 Imprimir Relatório", e -> imprimirVendas());
        btnImpr.setBackground(new Color(0, 84, 166)); btnImpr.setForeground(Color.WHITE);
        botoesRodape.add(btnImpr);

        rodape.add(botoesRodape, BorderLayout.EAST);
        p.add(rodape, BorderLayout.SOUTH);
        return p;
    }

    private void buscarVendas() {
        try {
            LocalDateTime ini = null, fim = null;
            if (!Validator.isBlank(tfVendasDataIni.getText()))
                ini = LocalDate.parse(tfVendasDataIni.getText().trim(), FMT_IN).atStartOfDay();
            if (!Validator.isBlank(tfVendasDataFim.getText()))
                fim = LocalDate.parse(tfVendasDataFim.getText().trim(), FMT_IN).atTime(23, 59, 59);

            if (ini == null) ini = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
            if (fim == null) fim = LocalDateTime.now();

            String filtroCliente = Validator.isBlank(tfVendasCliente.getText()) ? null : tfVendasCliente.getText().trim().toLowerCase();

            List<Pagamento> vendas = services.relatorios().buscarVendasPorPeriodo(ini, fim);
            vendasModel.setRowCount(0);

            BigDecimal totalGeral = BigDecimal.ZERO, totalDescontos = BigDecimal.ZERO;
            int count = 0;
            for (Pagamento pag : vendas) {
                String cliente = pag.getPedido() != null && pag.getPedido().getCliente() != null
                        ? pag.getPedido().getCliente().getNome() : "—";
                if (filtroCliente != null && !cliente.toLowerCase().contains(filtroCliente)) continue;

                BigDecimal desconto   = pag.getDesconto()   != null ? pag.getDesconto()   : BigDecimal.ZERO;
                BigDecimal valorFinal = pag.getValorFinal() != null ? pag.getValorFinal() : pag.getValorPago();
                vendasModel.addRow(new Object[]{
                        pag.getPedido() != null ? "#" + pag.getPedido().getId() : "—",
                        pag.getDataPagamento().format(FMT_OUT),
                        cliente,
                        pag.getFormaPagamento().toString(),
                        String.format("R$ %.2f", pag.getValorPago()),
                        desconto.compareTo(BigDecimal.ZERO) > 0 ? String.format("- R$ %.2f", desconto) : "—",
                        String.format("R$ %.2f", valorFinal),
                        pag.getId()
                });
                totalGeral = totalGeral.add(valorFinal);
                totalDescontos = totalDescontos.add(desconto);
                count++;
            }
            lblResumoVendas.setText(String.format(
                    "%d venda(s) | Descontos: R$ %.2f | Total: R$ %.2f", count, totalDescontos, totalGeral));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelarNotaSelecionada() {
        int row = vendasTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione uma nota para cancelar.", "Atenção", JOptionPane.WARNING_MESSAGE); return; }

        String pedidoStr = (String) vendasModel.getValueAt(row, 0);
        String cliente   = (String) vendasModel.getValueAt(row, 2);
        String total     = (String) vendasModel.getValueAt(row, 6);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Cancelar a nota do pedido " + pedidoStr + "?\n" +
                        "Cliente: " + cliente + "\nTotal: " + total + "\n\n" +
                        "O estoque será estornado e o pedido voltará para PENDENTE.",
                "Confirmar Cancelamento de Nota", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            // Extrai ID do pedido (ex: "#42" → 42)
            Long pedidoId = Long.parseLong(pedidoStr.replace("#", "").trim());
            services.pedidos().cancelarNota(pedidoId);
            JOptionPane.showMessageDialog(this, "Nota cancelada com sucesso!\nO pedido voltou para status PENDENTE.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            buscarVendas();
        } catch (RegraNegocioException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Atenção", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao cancelar nota: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void imprimirVendas() {
        if (vendasModel.getRowCount() == 0) { JOptionPane.showMessageDialog(this, "Nenhum dado para imprimir."); return; }
        JPanel p = buildPainelImpressaoVendas();
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Relatório de Vendas");
        job.setPrintable((g, pf, pi) -> {
            if (pi > 0) return Printable.NO_SUCH_PAGE;
            p.setSize((int) pf.getImageableWidth(), (int) pf.getImageableHeight()); p.doLayout();
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            double scale = Math.min(pf.getImageableWidth() / p.getWidth(), pf.getImageableHeight() / p.getHeight());
            if (scale < 1.0) g2.scale(scale, scale);
            p.printAll(g2); return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) { try { job.print(); } catch (PrinterException ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage()); } }
    }

    private JPanel buildPainelImpressaoVendas() {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(16, 20, 16, 20));
        Font fTit = new Font("Tahoma", Font.BOLD, 14);
        Font fSub = new Font("Tahoma", Font.BOLD, 11);
        Font fNorm = new Font("Tahoma", Font.PLAIN, 10);
        Font fBold = new Font("Tahoma", Font.BOLD, 10);
        addL(p, "CARMEL SISTEMA DE GESTÃO", fTit, new Color(0,60,140), SwingConstants.CENTER);
        addL(p, "RELATÓRIO DE VENDAS", fSub, Color.BLACK, SwingConstants.CENTER);
        addL(p, lblResumoVendas.getText(), fNorm, Color.GRAY, SwingConstants.CENTER);
        p.add(Box.createRigidArea(new Dimension(0, 8)));
        p.add(sep());
        JPanel header = new JPanel(new GridLayout(1, 6));
        header.setBackground(new Color(10, 36, 106)); header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        for (String h : new String[]{"Pedido","Data/Hora","Cliente","Forma Pag.","Desconto","Total (R$)"}) {
            JLabel l = new JLabel("  " + h); l.setFont(fBold); l.setForeground(Color.WHITE); header.add(l);
        }
        p.add(header);
        BigDecimal totalGeral = BigDecimal.ZERO;
        for (int i = 0; i < vendasModel.getRowCount(); i++) {
            JPanel row = new JPanel(new GridLayout(1, 6));
            row.setBackground(i % 2 == 0 ? Color.WHITE : new Color(245, 245, 250));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
            for (int col : new int[]{0, 1, 2, 3, 5, 6}) {
                Object v = vendasModel.getValueAt(i, col);
                JLabel l = new JLabel("  " + (v != null ? v : "")); l.setFont(fNorm); row.add(l);
            }
            p.add(row);
            try { totalGeral = totalGeral.add(new BigDecimal(vendasModel.getValueAt(i, 6).toString().replace("R$ ","").replace(",","."))); } catch (Exception ignored) {}
        }
        p.add(sep());
        JPanel totalRow = new JPanel(new BorderLayout()); totalRow.setOpaque(false); totalRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel lTot = new JLabel("TOTAL GERAL:"); lTot.setFont(fBold);
        JLabel vTot = new JLabel(String.format("R$ %.2f", totalGeral)); vTot.setFont(fBold); vTot.setForeground(new Color(0,84,166)); vTot.setHorizontalAlignment(SwingConstants.RIGHT);
        totalRow.add(lTot, BorderLayout.WEST); totalRow.add(vTot, BorderLayout.EAST);
        p.add(totalRow);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════
    // ABA 2 — CONSULTA DE PEDIDOS
    // ══════════════════════════════════════════════════════════════════════

    private JPanel buildAbaPedidos() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Filtros
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filtros.setBackground(UIFactory.XP_PANEL_BG);
        filtros.setBorder(UIFactory.groupBorder("Filtros de Pedidos"));

        filtros.add(UIFactory.labelLight("Cliente:"));
        tfPedidoCliente = UIFactory.styledField("");
        tfPedidoCliente.setPreferredSize(new Dimension(150, 22));
        filtros.add(tfPedidoCliente);

        filtros.add(UIFactory.labelLight("De:"));
        tfPedidoDataDe = UIFactory.styledField("");
        tfPedidoDataDe.setPreferredSize(new Dimension(95, 22));
        tfPedidoDataDe.setToolTipText("dd/MM/yyyy");
        filtros.add(tfPedidoDataDe);

        filtros.add(UIFactory.labelLight("Até:"));
        tfPedidoDataAte = UIFactory.styledField("");
        tfPedidoDataAte.setPreferredSize(new Dimension(95, 22));
        tfPedidoDataAte.setToolTipText("dd/MM/yyyy");
        filtros.add(tfPedidoDataAte);

        filtros.add(UIFactory.labelLight("Status:"));
        cbPedidoStatus = new JComboBox<>(new String[]{"TODOS", "PENDENTE", "CONFIRMADO", "CANCELADO"});
        cbPedidoStatus.setFont(UIFactory.FONT_NORMAL);
        filtros.add(cbPedidoStatus);

        JButton btnBuscar = UIFactory.bigActionButton("🔍 Buscar", e -> buscarPedidos());
        btnBuscar.setBackground(new Color(0, 84, 166)); btnBuscar.setForeground(Color.WHITE);
        filtros.add(btnBuscar);

        filtros.add(UIFactory.bigActionButton("Limpar", e -> {
            tfPedidoCliente.setText(""); tfPedidoDataDe.setText(""); tfPedidoDataAte.setText("");
            cbPedidoStatus.setSelectedIndex(0); pedidosModel.setRowCount(0); itensPedidoModel.setRowCount(0);
        }));
        p.add(filtros, BorderLayout.NORTH);

        // Tabela de pedidos
        pedidosModel = new DefaultTableModel(
                new String[]{"ID","Data/Hora","Cliente","Total (R$)","Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        pedidosTable = UIFactory.styledTable();
        pedidosTable.setModel(pedidosModel);
        pedidosTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        pedidosTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        pedidosTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        pedidosTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        pedidosTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        pedidosTable.setDefaultRenderer(Object.class, (t, value, isSel, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(UIFactory.FONT_NORMAL); cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(1, 4, 1, 4));
            if (isSel) { cell.setBackground(UIFactory.XP_TABLE_SEL); cell.setForeground(Color.WHITE); }
            else {
                String st = (String) pedidosModel.getValueAt(row, 4);
                cell.setBackground("PENDENTE".equals(st) ? new Color(255, 245, 220)
                        : "CONFIRMADO".equals(st) ? new Color(220, 255, 220)
                        : "CANCELADO".equals(st) ? new Color(255, 220, 220) : Color.WHITE);
                cell.setForeground(Color.BLACK);
                if (col == 3) cell.setHorizontalAlignment(SwingConstants.RIGHT);
            }
            return cell;
        });

        // Tabela de itens do pedido selecionado
        itensPedidoModel = new DefaultTableModel(
                new String[]{"Produto","Qtd","Preço Unit.","Subtotal"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable itensTable = UIFactory.styledTable();
        itensTable.setModel(itensPedidoModel);

        pedidosTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && pedidosTable.getSelectedRow() >= 0)
                carregarItensPedido((Long) pedidosModel.getValueAt(pedidosTable.getSelectedRow(), 0));
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(pedidosTable), new JScrollPane(itensTable));
        split.setDividerLocation(560);
        split.setDividerSize(5);
        p.add(split, BorderLayout.CENTER);

        // Rodapé com ações
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        rodape.setBackground(UIFactory.XP_PANEL_BG);
        rodape.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));

        rodape.add(UIFactory.bigActionButton("↻ Atualizar", e -> buscarPedidos()));

        JButton btnImprPedido = UIFactory.bigActionButton("🖨 Ver / Imprimir Pedido", e -> {
            if (pedidosTable.getSelectedRow() < 0) { JOptionPane.showMessageDialog(p, "Selecione um pedido."); return; }
            abrirImpressaoPedido((Long) pedidosModel.getValueAt(pedidosTable.getSelectedRow(), 0));
        });
        btnImprPedido.setBackground(new Color(0, 84, 166)); btnImprPedido.setForeground(Color.WHITE);
        rodape.add(btnImprPedido);

        JButton btnCancelarPedido = UIFactory.bigActionButton("❌ Cancelar Pedido", e -> cancelarPedidoSelecionado());
        btnCancelarPedido.setBackground(new Color(180, 30, 30)); btnCancelarPedido.setForeground(Color.WHITE);
        rodape.add(btnCancelarPedido);

        JLabel dica = new JLabel("  Legenda: Amarelo=Pendente | Verde=Confirmado | Vermelho=Cancelado");
        dica.setFont(new Font("Tahoma", Font.ITALIC, 10)); dica.setForeground(new Color(80, 80, 80));
        rodape.add(dica);

        p.add(rodape, BorderLayout.SOUTH);
        return p;
    }

    private void buscarPedidos() {
        pedidosModel.setRowCount(0);
        itensPedidoModel.setRowCount(0);

        String cliente = Validator.isBlank(tfPedidoCliente.getText()) ? null : tfPedidoCliente.getText().trim();
        LocalDateTime de = null, ate = null;
        try { if (!Validator.isBlank(tfPedidoDataDe.getText())) de = LocalDate.parse(tfPedidoDataDe.getText().trim(), FMT_IN).atStartOfDay(); } catch (Exception ignored) {}
        try { if (!Validator.isBlank(tfPedidoDataAte.getText())) ate = LocalDate.parse(tfPedidoDataAte.getText().trim(), FMT_IN).atTime(23,59,59); } catch (Exception ignored) {}

        String statusFiltro = (String) cbPedidoStatus.getSelectedItem();

        services.pedidos().buscarComFiltros(cliente, de, ate, null, null).forEach(ped -> {
            if (!"TODOS".equals(statusFiltro) && !ped.getStatus().toString().equals(statusFiltro)) return;
            pedidosModel.addRow(new Object[]{
                    ped.getId(),
                    ped.getDataPedido().format(FMT_OUT),
                    ped.getCliente().getNome(),
                    String.format("R$ %.2f", ped.getValorTotal() != null ? ped.getValorTotal() : BigDecimal.ZERO),
                    ped.getStatus().toString()
            });
        });
    }

    private void carregarItensPedido(Long pedidoId) {
        itensPedidoModel.setRowCount(0);
        try {
            Pedido ped = services.pedidos().buscarComItens(pedidoId);
            ped.getItensPedidos().forEach(item ->
                    itensPedidoModel.addRow(new Object[]{
                            item.getProduto().getNome(),
                            item.getQuantidade(),
                            String.format("R$ %.2f", item.getPrecoUnitario()),
                            String.format("R$ %.2f", item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO)
                    })
            );
        } catch (Exception ignored) {}
    }

    private void cancelarPedidoSelecionado() {
        int row = pedidosTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione um pedido para cancelar.", "Atenção", JOptionPane.WARNING_MESSAGE); return; }

        Long pedidoId = (Long) pedidosModel.getValueAt(row, 0);
        String status   = (String) pedidosModel.getValueAt(row, 4);
        String cliente  = (String) pedidosModel.getValueAt(row, 2);

        if ("CANCELADO".equals(status)) { JOptionPane.showMessageDialog(this, "Este pedido já está cancelado."); return; }
        if ("CONFIRMADO".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "Este pedido está CONFIRMADO (nota emitida).\nPara cancelá-lo, primeiro cancele a nota na aba 'Relatório de Vendas'.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Cancelar pedido #" + pedidoId + " de " + cliente + "?\nEsta ação não pode ser desfeita.",
                "Confirmar Cancelamento", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            services.pedidos().cancelarPedido(pedidoId);
            JOptionPane.showMessageDialog(this, "Pedido cancelado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            buscarPedidos();
        } catch (RegraNegocioException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Atenção", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirImpressaoPedido(Long pedidoId) {
        try {
            Pedido ped = services.pedidos().buscarComItens(pedidoId);
            // Reutiliza o layout de folha do PedidoPanel via reflexão simples — monta aqui mesmo
            JPanel content = buildFolhaPedido(ped);
            java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
            job.setJobName("Pedido #" + ped.getId());
            java.awt.print.PageFormat pf = job.defaultPage();
            java.awt.print.Paper paper = new java.awt.print.Paper();
            double w = 595, h = 842;
            paper.setSize(w, h); paper.setImageableArea(36, 36, w - 72, h - 72);
            pf.setPaper(paper); pf.setOrientation(java.awt.print.PageFormat.PORTRAIT);
            job.setPrintable((g, pageFormat, pi) -> {
                if (pi > 0) return java.awt.print.Printable.NO_SUCH_PAGE;
                content.setSize((int) pageFormat.getImageableWidth(), (int) pageFormat.getImageableHeight());
                content.doLayout();
                Graphics2D g2 = (Graphics2D) g;
                g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                content.printAll(g2);
                return java.awt.print.Printable.PAGE_EXISTS;
            }, pf);

            JDialog preview = new JDialog(SwingUtilities.getWindowAncestor(this),
                    "Pedido #" + ped.getId() + " — " + ped.getStatus(), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
            preview.setSize(620, 880); preview.setLocationRelativeTo(this);
            preview.setLayout(new BorderLayout());
            preview.add(new JScrollPane(content), BorderLayout.CENTER);

            JPanel rod = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
            rod.setBackground(UIFactory.XP_BG);
            JButton btnImpr = UIFactory.bigActionButton("🖨 Imprimir", null);
            btnImpr.setBackground(new Color(0, 84, 166)); btnImpr.setForeground(Color.WHITE);
            btnImpr.addActionListener(e -> { if (job.printDialog()) { try { job.print(); } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage()); } } });
            rod.add(btnImpr); rod.add(UIFactory.bigActionButton("Fechar", e -> preview.dispose()));
            preview.add(rod, BorderLayout.SOUTH);
            preview.setVisible(true);
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE); }
    }

    private JPanel buildFolhaPedido(Pedido ped) {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(20, 30, 20, 30));
        Font fTit  = new Font("Tahoma", Font.BOLD, 16);
        Font fSub  = new Font("Tahoma", Font.BOLD, 12);
        Font fNorm = new Font("Tahoma", Font.PLAIN, 11);
        Font fBold = new Font("Tahoma", Font.BOLD, 11);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        addL(p, "CARMEL SISTEMA DE GESTÃO", fTit, new Color(0,60,140), SwingConstants.CENTER);
        addL(p, "PEDIDO DE VENDA", fSub, Color.BLACK, SwingConstants.CENTER);
        Color statusColor = ped.getStatus() == StatusPedido.PENDENTE ? new Color(180,60,0)
                : ped.getStatus() == StatusPedido.CONFIRMADO ? new Color(0,120,0) : new Color(180,0,0);
        addL(p, "Status: " + ped.getStatus().toString(), new Font("Tahoma",Font.BOLD,11), statusColor, SwingConstants.CENTER);
        p.add(Box.createRigidArea(new Dimension(0, 8)));
        p.add(sep());
        addL(p, "Pedido Nº: " + ped.getId() + "   |   Data: " + ped.getDataPedido().format(fmt), fNorm, Color.BLACK, SwingConstants.LEFT);
        addL(p, "Cliente: " + ped.getCliente().getNome(), fBold, Color.BLACK, SwingConstants.LEFT);
        if (ped.getObservacoes() != null && !ped.getObservacoes().isEmpty())
            addL(p, "Obs: " + ped.getObservacoes(), fNorm, Color.GRAY, SwingConstants.LEFT);
        p.add(Box.createRigidArea(new Dimension(0, 10)));
        addL(p, "ITENS DO PEDIDO", fSub, new Color(0,84,166), SwingConstants.LEFT);
        p.add(sep());
        JPanel header = new JPanel(new GridLayout(1, 4));
        header.setBackground(new Color(10, 36, 106)); header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        for (String h : new String[]{"Produto","Qtd","Preço Unit.","Subtotal"}) {
            JLabel l = new JLabel("  " + h); l.setFont(fBold); l.setForeground(Color.WHITE); header.add(l);
        }
        p.add(header);
        BigDecimal total = BigDecimal.ZERO; int idx = 0;
        for (ItensPedido item : ped.getItensPedidos()) {
            JPanel row = new JPanel(new GridLayout(1, 4));
            row.setBackground(idx++ % 2 == 0 ? Color.WHITE : new Color(245,245,250));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
            BigDecimal sub = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
            total = total.add(sub);
            addCell(row, item.getProduto().getNome(), fNorm);
            addCell(row, String.valueOf(item.getQuantidade()), fNorm);
            addCell(row, String.format("R$ %.2f", item.getPrecoUnitario()), fNorm);
            addCell(row, String.format("R$ %.2f", sub), fNorm);
            p.add(row);
        }
        p.add(sep());
        JPanel totalRow = new JPanel(new BorderLayout()); totalRow.setOpaque(false); totalRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel lTot = new JLabel("TOTAL DO PEDIDO:"); lTot.setFont(fBold);
        JLabel vTot = new JLabel(String.format("R$ %.2f", total));
        vTot.setFont(new Font("Tahoma", Font.BOLD, 13)); vTot.setForeground(new Color(0,84,166)); vTot.setHorizontalAlignment(SwingConstants.RIGHT);
        totalRow.add(lTot, BorderLayout.WEST); totalRow.add(vTot, BorderLayout.EAST);
        p.add(totalRow);
        p.add(Box.createRigidArea(new Dimension(0, 30)));
        addL(p, "_________________________________          _________________________________", fNorm, Color.BLACK, SwingConstants.CENTER);
        addL(p, "          Atendente                                   Cliente / Responsável", fNorm, Color.GRAY, SwingConstants.CENTER);
        return p;
    }

    // ── Utilitários ────────────────────────────────────────────────────────

    public void reloadConsultas() {
        // Carrega vendas do dia ao abrir
        String hoje = LocalDate.now().format(FMT_IN);
        tfVendasDataIni.setText(hoje); tfVendasDataFim.setText(hoje);
        buscarVendas();
    }

    private void addL(JPanel p, String t, Font f, Color c, int align) {
        JLabel l = new JLabel(t, align); l.setFont(f); l.setForeground(c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT); l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height + 4));
        p.add(l);
    }

    private void addCell(JPanel row, String t, Font f) { JLabel l = new JLabel("  " + t); l.setFont(f); row.add(l); }

    private JSeparator sep() {
        JSeparator s = new JSeparator(); s.setForeground(new Color(180,180,180));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); return s;
    }
}