package br.carmel.ui.panels;

import br.carmel.model.Produto;
import br.carmel.service.ServiceLocator;
import br.carmel.util.UIFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Relatório de Estoque.
 * Delegado ao RelatorioService.
 */
public class RelatorioEstoquePanel extends JPanel {

    private final ServiceLocator services;
    private DefaultTableModel tableModel;
    private JLabel lblTotalProdutos, lblTotal, lblZerado, lblValorTotal;

    public RelatorioEstoquePanel(ServiceLocator services) {
        this.services = services;
        setLayout(new BorderLayout());
        setBackground(UIFactory.XP_BG);
        build();
    }

    public void carregarDados(boolean soEstoqueBaixo) {
        tableModel.setRowCount(0);
        List<Produto> lista = soEstoqueBaixo
                ? services.relatorios().buscarProdutosEstoqueBaixo(5)
                : services.relatorios().buscarTodosProdutosParaEstoque();

        int totalItens = 0, zerado = 0;
        BigDecimal valorTotalEstoque = BigDecimal.ZERO;

        for (Produto p : lista) {
            int est = p.getEstoque() != null ? p.getEstoque() : 0;
            totalItens += est;
            if (est <= 5) zerado++;

            String margem = "—";
            if (p.getValor() != null && p.getPrecoCusto() != null
                    && p.getPrecoCusto().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal m = p.getValor().subtract(p.getPrecoCusto())
                        .divide(p.getPrecoCusto(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                margem = String.format("%.1f%%", m);
            }

            BigDecimal base = p.getPrecoMedio() != null ? p.getPrecoMedio()
                    : p.getPrecoCusto() != null ? p.getPrecoCusto() : p.getValor();
            BigDecimal vEstoque = base != null ? base.multiply(BigDecimal.valueOf(est)) : BigDecimal.ZERO;
            valorTotalEstoque = valorTotalEstoque.add(vEstoque);

            tableModel.addRow(new Object[]{
                    p.getId(), p.getNome(), est,
                    String.format("R$ %.2f", p.getValor()),
                    p.getPrecoCusto()  != null ? String.format("R$ %.2f", p.getPrecoCusto())  : "—",
                    p.getPrecoMedio()  != null ? String.format("R$ %.2f", p.getPrecoMedio())  : "—",
                    margem,
                    String.format("R$ %.2f", vEstoque)
            });
        }

        lblTotalProdutos.setText(String.valueOf(lista.size()));
        lblTotal.setText(String.valueOf(totalItens));
        lblZerado.setText(String.valueOf(zerado));
        lblValorTotal.setText(String.format("R$ %.2f", valorTotalEstoque));
    }

    private void build() {
        add(UIFactory.xpTitleBar("Relatório de Estoque"), BorderLayout.NORTH);

        // Cards de resumo
        JPanel cards = new JPanel(new GridLayout(1, 4, 8, 0));
        cards.setBackground(UIFactory.XP_BG);
        cards.setBorder(new EmptyBorder(8, 8, 4, 8));

        lblTotalProdutos = new JLabel("0");
        lblTotal         = new JLabel("0");
        lblZerado        = new JLabel("0");
        lblValorTotal    = new JLabel("R$ 0,00");

        cards.add(card("Total de Produtos",   lblTotalProdutos, new Color(0, 84, 166)));
        cards.add(card("Total de Itens",      lblTotal,         new Color(0, 110, 0)));
        cards.add(card("Valor em Estoque",    lblValorTotal,    new Color(140, 60, 0)));
        cards.add(card("Zerado / Baixo (≤5)", lblZerado,        new Color(180, 0, 0)));
        add(cards, BorderLayout.NORTH);

        // Tabela
        tableModel = new DefaultTableModel(
                new String[]{"ID","Nome","Estoque","Preço Venda","Preço Custo","Preço Médio","Margem","Valor Estoque"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = UIFactory.styledTable();
        table.setModel(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(35);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setPreferredWidth(85);
        table.getColumnModel().getColumn(4).setPreferredWidth(85);
        table.getColumnModel().getColumn(5).setPreferredWidth(85);
        table.getColumnModel().getColumn(6).setPreferredWidth(65);
        table.getColumnModel().getColumn(7).setPreferredWidth(100);

        table.setDefaultRenderer(Object.class, (t, value, isSel, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(UIFactory.FONT_NORMAL);
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(1, 4, 1, 4));
            if (isSel) {
                cell.setBackground(UIFactory.XP_TABLE_SEL);
                cell.setForeground(Color.WHITE);
            } else {
                int est = 0;
                try { est = Integer.parseInt(tableModel.getValueAt(row, 2).toString()); } catch (Exception ignored) {}
                cell.setBackground(est <= 0 ? new Color(255,220,220) : est <= 5 ? new Color(255,248,200) : Color.WHITE);
                cell.setForeground(Color.BLACK);
                switch (col) {
                    case 2 -> {
                        cell.setFont(UIFactory.FONT_BOLD);
                        cell.setForeground(est <= 0 ? new Color(180,0,0) : est <= 5 ? new Color(140,100,0) : new Color(0,110,0));
                        cell.setHorizontalAlignment(SwingConstants.CENTER);
                    }
                    case 5 -> {
                        cell.setForeground(new Color(0, 84, 166));
                        cell.setHorizontalAlignment(SwingConstants.RIGHT);
                    }
                    case 6 -> {
                        try {
                            double m = Double.parseDouble(value.toString().replace("%","").replace(",","."));
                            cell.setFont(UIFactory.FONT_BOLD);
                            cell.setForeground(m >= 0 ? new Color(0,110,0) : new Color(180,0,0));
                        } catch (Exception ignored) { cell.setForeground(Color.GRAY); }
                    }
                    case 3, 4, 7 -> cell.setHorizontalAlignment(SwingConstants.RIGHT);
                }
            }
            return cell;
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        add(scroll, BorderLayout.CENTER);

        // Rodapé
        JPanel rodape = new JPanel(new BorderLayout(8, 0));
        rodape.setBackground(new Color(212, 208, 200));
        rodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)),
                new EmptyBorder(4, 8, 4, 8)));

        JPanel esq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        esq.setOpaque(false);
        JButton btnTodos  = UIFactory.bigActionButton("↻ Todos",        e -> carregarDados(false));
        JButton btnBaixos = UIFactory.bigActionButton("⚠ Estoque Baixo", e -> carregarDados(true));
        btnBaixos.setBackground(new Color(180, 100, 0)); btnBaixos.setForeground(Color.WHITE);
        esq.add(btnTodos); esq.add(btnBaixos);
        rodape.add(esq, BorderLayout.WEST);

        JButton btnImpr = UIFactory.bigActionButton("🖨 Imprimir Relatório", e -> imprimir());
        btnImpr.setBackground(new Color(0, 84, 166)); btnImpr.setForeground(Color.WHITE);
        rodape.add(btnImpr, BorderLayout.EAST);
        add(rodape, BorderLayout.SOUTH);
    }

    private JPanel card(String titulo, JLabel valor, Color cor) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cor, 1),
                new EmptyBorder(6, 10, 6, 10)));
        JLabel lTit = new JLabel(titulo);
        lTit.setFont(new Font("Tahoma", Font.PLAIN, 10));
        lTit.setForeground(new Color(100, 100, 100));
        valor.setFont(new Font("Tahoma", Font.BOLD, 16));
        valor.setForeground(cor);
        valor.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(lTit, BorderLayout.NORTH);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }

    private void imprimir() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Nenhum dado para imprimir."); return;
        }
        JPanel p = buildPainelImpressao();
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Relatório de Estoque");
        job.setPrintable((g, pf, pi) -> {
            if (pi > 0) return Printable.NO_SUCH_PAGE;
            p.setSize((int) pf.getImageableWidth(), (int) pf.getImageableHeight()); p.doLayout();
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            double scale = Math.min(pf.getImageableWidth() / p.getWidth(), pf.getImageableHeight() / p.getHeight());
            if (scale < 1.0) g2.scale(scale, scale);
            p.printAll(g2); return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try { job.print(); } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao imprimir: " + ex.getMessage());
            }
        }
    }

    private JPanel buildPainelImpressao() {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(16, 20, 16, 20));

        Font fTit  = new Font("Tahoma", Font.BOLD, 14);
        Font fSub  = new Font("Tahoma", Font.BOLD, 11);
        Font fNorm = new Font("Tahoma", Font.PLAIN, 9);
        Font fBold = new Font("Tahoma", Font.BOLD, 9);

        addL(p, "CARMEL SISTEMA DE GESTÃO", fTit, new Color(0,60,140), SwingConstants.CENTER);
        addL(p, "RELATÓRIO DE ESTOQUE", fSub, Color.BLACK, SwingConstants.CENTER);
        addL(p, "Produtos: " + lblTotalProdutos.getText() + "  |  Total itens: " + lblTotal.getText()
                + "  |  Valor total: " + lblValorTotal.getText(), fNorm, Color.GRAY, SwingConstants.CENTER);
        p.add(Box.createRigidArea(new Dimension(0, 8)));
        p.add(sep());

        JPanel header = new JPanel(new GridLayout(1, 6));
        header.setBackground(new Color(10, 36, 106)); header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        for (String h : new String[]{"Nome","Estoque","Preço Venda","Preço Custo","Preço Médio","Margem"}) {
            JLabel l = new JLabel("  " + h); l.setFont(fBold); l.setForeground(Color.WHITE); header.add(l);
        }
        p.add(header);

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            JPanel row = new JPanel(new GridLayout(1, 6));
            row.setBackground(i % 2 == 0 ? Color.WHITE : new Color(245, 245, 250));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
            for (int col : new int[]{1, 2, 3, 4, 5, 6}) {
                Object v = tableModel.getValueAt(i, col);
                JLabel l = new JLabel("  " + (v != null ? v : "")); l.setFont(fNorm); row.add(l);
            }
            p.add(row);
        }
        return p;
    }

    private void addL(JPanel p, String t, Font f, Color c, int align) {
        JLabel l = new JLabel(t, align); l.setFont(f); l.setForeground(c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height + 4));
        p.add(l);
    }

    private JSeparator sep() {
        JSeparator s = new JSeparator(); s.setForeground(new Color(180,180,180));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); return s;
    }
}