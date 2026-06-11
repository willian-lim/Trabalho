package br.carmel.ui.panels;

import br.carmel.model.Pagamento;
import br.carmel.service.RegraNegocioException;
import br.carmel.service.ServiceLocator;
import br.carmel.util.UIFactory;
import br.carmel.util.Validator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RelatorioVendasPanel extends JPanel {

    private final ServiceLocator services;
    private static final DateTimeFormatter FMT_IN  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_OUT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DefaultTableModel tableModel;
    private JTextField tfDataIni, tfDataFim;
    private JLabel lblResumo;

    public RelatorioVendasPanel(ServiceLocator services) {
        this.services = services;
        setLayout(new BorderLayout());
        setBackground(UIFactory.XP_BG);
        build();
    }

    private void build() {
        add(UIFactory.xpTitleBar("Relatório de Vendas"), BorderLayout.NORTH);

        
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        filtros.setBackground(UIFactory.XP_PANEL_BG);
        filtros.setBorder(UIFactory.groupBorder("Período"));

        filtros.add(UIFactory.labelLight("Data inicial:"));
        tfDataIni = UIFactory.styledField(""); tfDataIni.setPreferredSize(new Dimension(100, 22));
        filtros.add(tfDataIni);

        filtros.add(UIFactory.labelLight("Data final:"));
        tfDataFim = UIFactory.styledField(""); tfDataFim.setPreferredSize(new Dimension(100, 22));
        filtros.add(tfDataFim);

        JButton btnBuscar = UIFactory.bigActionButton("🔍 Buscar", e -> buscar());
        btnBuscar.setBackground(new Color(0, 84, 166)); btnBuscar.setForeground(Color.WHITE);
        filtros.add(btnBuscar);
        filtros.add(UIFactory.bigActionButton("Limpar", e -> { tfDataIni.setText(""); tfDataFim.setText(""); tableModel.setRowCount(0); lblResumo.setText(""); }));

        tfDataIni.addActionListener(e -> buscar());
        tfDataFim.addActionListener(e -> buscar());
        add(filtros, BorderLayout.NORTH);

        
        tableModel = new DefaultTableModel(
                new String[]{"#","Data/Hora","Cliente","Forma Pagamento","Subtotal (R$)","Desconto (R$)","Total (R$)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = UIFactory.styledTable();
        table.setModel(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        table.getColumnModel().getColumn(6).setPreferredWidth(100);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        right.setFont(UIFactory.FONT_BOLD);
        table.getColumnModel().getColumn(4).setCellRenderer(right);
        table.getColumnModel().getColumn(5).setCellRenderer(right);
        table.getColumnModel().getColumn(6).setCellRenderer(right);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        add(scroll, BorderLayout.CENTER);

        
        JPanel rodape = new JPanel(new BorderLayout(8, 0));
        rodape.setBackground(new Color(212, 208, 200));
        rodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)),
                new EmptyBorder(4, 8, 4, 8)));

        lblResumo = new JLabel("");
        lblResumo.setFont(UIFactory.FONT_BOLD);
        lblResumo.setForeground(new Color(0, 84, 166));
        rodape.add(lblResumo, BorderLayout.WEST);

        JButton btnImpr = UIFactory.bigActionButton("🖨 Imprimir Relatório", e -> imprimir());
        btnImpr.setBackground(new Color(0, 84, 166)); btnImpr.setForeground(Color.WHITE);
        rodape.add(btnImpr, BorderLayout.EAST);
        add(rodape, BorderLayout.SOUTH);
    }

    private void buscar() {
        if (Validator.isBlank(tfDataIni.getText()) || Validator.isBlank(tfDataFim.getText())) {
            JOptionPane.showMessageDialog(this, "Informe data inicial e final.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            LocalDateTime ini = LocalDate.parse(tfDataIni.getText().trim(), FMT_IN).atStartOfDay();
            LocalDateTime fim = LocalDate.parse(tfDataFim.getText().trim(), FMT_IN).atTime(23, 59, 59);

            List<Pagamento> vendas = services.relatorios().buscarVendasPorPeriodo(ini, fim);

            tableModel.setRowCount(0);
            for (Pagamento pag : vendas) {
                String cliente = pag.getPedido() != null && pag.getPedido().getCliente() != null
                        ? pag.getPedido().getCliente().getNome() : "—";
                BigDecimal desconto   = pag.getDesconto()   != null ? pag.getDesconto()   : BigDecimal.ZERO;
                BigDecimal valorFinal = pag.getValorFinal() != null ? pag.getValorFinal() : pag.getValorPago();
                tableModel.addRow(new Object[]{
                        pag.getPedido() != null ? "#" + pag.getPedido().getId() : "—",
                        pag.getDataPagamento().format(FMT_OUT),
                        cliente,
                        pag.getFormaPagamento().toString(),
                        String.format("R$ %.2f", pag.getValorPago()),
                        desconto.compareTo(BigDecimal.ZERO) > 0 ? String.format("- R$ %.2f", desconto) : "—",
                        String.format("R$ %.2f", valorFinal)
                });
            }

            BigDecimal totalVendas   = services.relatorios().calcularTotalVendas(vendas);
            BigDecimal totalDescontos = services.relatorios().calcularTotalDescontos(vendas);

            lblResumo.setText(String.format(
                    "Período: %s a %s  |  %d venda(s)  |  Descontos: R$ %.2f  |  Total: R$ %.2f",
                    tfDataIni.getText(), tfDataFim.getText(),
                    vendas.size(), totalDescontos, totalVendas));

        } catch (RegraNegocioException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Atenção", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void imprimir() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Nenhum dado para imprimir."); return;
        }
        JPanel p = buildPainelImpressao();
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

    private JPanel buildPainelImpressao() {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(16, 20, 16, 20));

        Font fTit  = new Font("Tahoma", Font.BOLD, 14);
        Font fSub  = new Font("Tahoma", Font.BOLD, 11);
        Font fNorm = new Font("Tahoma", Font.PLAIN, 10);
        Font fBold = new Font("Tahoma", Font.BOLD, 10);

        addL(p, "CARMEL SISTEMA DE GESTÃO", fTit, new Color(0,60,140), SwingConstants.CENTER);
        addL(p, "RELATÓRIO DE VENDAS", fSub, Color.BLACK, SwingConstants.CENTER);
        addL(p, lblResumo.getText(), fNorm, Color.GRAY, SwingConstants.CENTER);
        p.add(Box.createRigidArea(new Dimension(0, 8)));
        p.add(sep());

        JPanel header = new JPanel(new GridLayout(1, 6));
        header.setBackground(new Color(10, 36, 106)); header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        for (String h : new String[]{"Pedido","Data/Hora","Cliente","Forma Pag.","Desconto","Total (R$)"}) {
            JLabel l = new JLabel("  " + h); l.setFont(fBold); l.setForeground(Color.WHITE); header.add(l);
        }
        p.add(header);

        BigDecimal totalGeral = BigDecimal.ZERO;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            JPanel row = new JPanel(new GridLayout(1, 6));
            row.setBackground(i % 2 == 0 ? Color.WHITE : new Color(245, 245, 250));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
            for (int col : new int[]{0, 1, 2, 3, 5, 6}) {
                Object v = tableModel.getValueAt(i, col);
                JLabel l = new JLabel("  " + (v != null ? v : "")); l.setFont(fNorm); row.add(l);
            }
            p.add(row);
            try { totalGeral = totalGeral.add(new BigDecimal(tableModel.getValueAt(i, 6).toString().replace("R$ ","").replace(",","."))); } catch (Exception ignored) {}
        }

        p.add(sep());
        JPanel totalRow = new JPanel(new BorderLayout()); totalRow.setOpaque(false); totalRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel lTot = new JLabel("TOTAL GERAL:"); lTot.setFont(fBold);
        JLabel vTot = new JLabel(String.format("R$ %.2f", totalGeral)); vTot.setFont(fBold); vTot.setForeground(new Color(0,84,166)); vTot.setHorizontalAlignment(SwingConstants.RIGHT);
        totalRow.add(lTot, BorderLayout.WEST); totalRow.add(vTot, BorderLayout.EAST);
        p.add(totalRow);
        return p;
    }

    private void addL(JPanel p, String t, Font f, Color c, int align) {
        JLabel l = new JLabel(t, align); l.setFont(f); l.setForeground(c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT); l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height + 4));
        p.add(l);
    }

    private JSeparator sep() {
        JSeparator s = new JSeparator(); s.setForeground(new Color(180,180,180));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); return s;
    }
}