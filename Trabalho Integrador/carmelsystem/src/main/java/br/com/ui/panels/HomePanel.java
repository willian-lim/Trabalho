package br.carmel.ui.panels;

import br.carmel.service.RelatorioService;
import br.carmel.service.ServiceLocator;
import br.carmel.util.UIFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Painel Inicial — Dashboard com rankings do mês.
 * Delegado ao RelatorioService.
 */
public class HomePanel extends JPanel {

    private final ServiceLocator services;
    private DefaultTableModel modelProdutos, modelClientes;
    private JLabel lblMes;
    private BufferedImage bgImage;

    public HomePanel(ServiceLocator services) {
        this.services = services;
        try {
            java.net.URL url = getClass().getResource("/fundo.jpg");
            if (url != null) bgImage = ImageIO.read(url);
        } catch (Exception ignored) {}
        setLayout(new BorderLayout());
        setOpaque(false);
        build();
        carregarDados();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (bgImage != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
            g2.setColor(new Color(18, 18, 24, 180));
            g2.fillRect(0, 0, getWidth(), getHeight());
        } else {
            setOpaque(true); super.paintComponent(g);
        }
    }

    public void carregarDados() {
        modelProdutos.setRowCount(0);
        modelClientes.setRowCount(0);

        LocalDate hoje  = LocalDate.now();
        LocalDate inicio = hoje.withDayOfMonth(1);
        LocalDate fim    = hoje.withDayOfMonth(hoje.lengthOfMonth());
        lblMes.setText("Resumo de " + hoje.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy",
                new java.util.Locale("pt","BR"))).substring(0,1).toUpperCase()
                + hoje.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy",
                new java.util.Locale("pt","BR"))).substring(1));

        List<RelatorioService.ProdutoRanking> topProd = services.relatorios()
                .topProdutosMes(inicio.atStartOfDay(), fim.atTime(23,59,59));
        int rank = 1;
        for (RelatorioService.ProdutoRanking pr : topProd) {
            String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : String.valueOf(rank);
            modelProdutos.addRow(new Object[]{medal, pr.nome(), pr.quantidade(),
                    String.format("R$ %.2f", pr.total() != null ? pr.total() : BigDecimal.ZERO)});
            rank++;
        }

        List<RelatorioService.ClienteRanking> topCli = services.relatorios()
                .topClientesMes(inicio.atStartOfDay(), fim.atTime(23,59,59));
        rank = 1;
        for (RelatorioService.ClienteRanking cr : topCli) {
            String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : String.valueOf(rank);
            modelClientes.addRow(new Object[]{medal, cr.nome(), cr.pedidos() + " pedido(s)",
                    String.format("R$ %.2f", cr.total() != null ? cr.total() : BigDecimal.ZERO)});
            rank++;
        }
    }

    private void build() {
        // Topo
        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.setBorder(new EmptyBorder(10, 12, 6, 12));
        lblMes = new JLabel("Resumo do Mês");
        lblMes.setFont(new Font("Tahoma", Font.BOLD, 16));
        lblMes.setForeground(UIFactory.W11_ACCENT);
        JButton btnAtualizar = UIFactory.bigActionButton("↻ Atualizar", e -> carregarDados());
        btnAtualizar.setBackground(UIFactory.W11_ACCENT_DARK); btnAtualizar.setForeground(Color.WHITE);
        topo.add(lblMes, BorderLayout.WEST);
        topo.add(btnAtualizar, BorderLayout.EAST);
        add(topo, BorderLayout.NORTH);

        // Rankings
        JPanel rankings = new JPanel(new GridLayout(1, 2, 10, 0));
        rankings.setOpaque(false);
        rankings.setBorder(new EmptyBorder(0, 10, 10, 10));
        rankings.add(buildRankingProdutos());
        rankings.add(buildRankingClientes());
        add(rankings, BorderLayout.CENTER);
    }

    private JPanel buildRankingProdutos() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.setBorder(UIFactory.groupBorder("🏆 Produtos Mais Vendidos no Mês"));

        modelProdutos = new DefaultTableModel(
                new String[]{"#","Produto","Qtd Vendida","Total (R$)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = buildRankingTable(modelProdutos);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);

        table.setOpaque(false);
        table.setBackground(new Color(28, 28, 36, 200));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(new Color(28, 28, 36, 200));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildRankingClientes() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.setBorder(UIFactory.groupBorder("👤 Clientes que Mais Compraram no Mês"));

        modelClientes = new DefaultTableModel(
                new String[]{"#","Cliente","Pedidos","Total (R$)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = buildRankingTable(modelClientes);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);

        table.setOpaque(false);
        table.setBackground(new Color(28, 28, 36, 200));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(new Color(28, 28, 36, 200));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JTable buildRankingTable(DefaultTableModel model) {
        JTable table = UIFactory.styledTable();
        table.setModel(model);
        table.setDefaultRenderer(Object.class, (t, value, isSel, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(UIFactory.FONT_NORMAL); cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(2, 5, 2, 5));
            if (isSel) {
                cell.setBackground(UIFactory.XP_TABLE_SEL); cell.setForeground(Color.WHITE);
            } else {
                cell.setForeground(UIFactory.W11_TEXT);
                cell.setBackground(switch (row) {
                    case 0 -> new Color(50, 48, 20, 220);
                    case 1 -> new Color(32, 32, 44, 220);
                    case 2 -> new Color(40, 30, 30, 220);
                    default -> row % 2 == 0 ? new Color(28, 28, 36, 200) : new Color(34, 34, 46, 200);
                });
                if (col == 3) cell.setHorizontalAlignment(SwingConstants.RIGHT);
            }
            return cell;
        });
        return table;
    }
}