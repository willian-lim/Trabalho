package br.carmel.ui.panels;

import br.carmel.model.Cliente;
import br.carmel.model.Produto;
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
import java.util.ArrayList;
import java.util.List;

public class PedidoPanel extends JPanel {

    private final ServiceLocator services;
    private CaixaPanel caixaPanel;

    private final List<ItemRascunho> itensRascunho = new ArrayList<>();

    private final JTextField tfCliente   = UIFactory.styledField("");
    private Long clienteSelecionadoId    = null;

    private final JTextField tfProduto   = UIFactory.styledField("");
    private final JTextField tfQtd       = UIFactory.styledField("1");
    private final JTextField tfCodBarras = UIFactory.styledField("");
    private final JTextField tfPreco     = UIFactory.styledField("");
    private Long produtoSelecionadoId    = null;

    private final JTextField tfObs       = UIFactory.styledField("");

    private DefaultTableModel itensModel;
    private JTable itensTable;
    private JLabel lblTotal;

    public PedidoPanel(ServiceLocator services) {
        this.services = services;
        setLayout(new BorderLayout());
        setBackground(UIFactory.XP_BG);
        build();
    }

    public void setCaixaPanel(CaixaPanel cp) { this.caixaPanel = cp; }
    public void reloadCombos() {}
    public void reloadPedidos() { limparFormulario(); }

    

    private void build() {
        add(UIFactory.xpTitleBar("Novo Pedido  [Tab = avançar campo · Enter = confirmar lista · F5 = atualizar · 📂 Reabrir = editar pedido pendente]"), BorderLayout.NORTH);
        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildFormPanel() {
        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setBackground(UIFactory.XP_BG);

        
        JPanel secCliente = new JPanel(new GridBagLayout());
        secCliente.setBackground(UIFactory.XP_BG);
        secCliente.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(0, 84, 166), 1),
                        "Cliente",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        UIFactory.FONT_BOLD, new Color(0, 84, 166)),
                new EmptyBorder(4, 6, 6, 6)));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 4, 3, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        secCliente.add(UIFactory.labelLight("Cliente:"), gc);
        gc.gridx = 1; gc.weightx = 1;
        secCliente.add(tfCliente, gc);
        JButton btnBuscarCliente = UIFactory.bigActionButton("⊞", e -> buscarCliente());
        btnBuscarCliente.setPreferredSize(new Dimension(36, 24));
        gc.gridx = 2; gc.weightx = 0;
        secCliente.add(btnBuscarCliente, gc);

        
        JPanel secProduto = new JPanel(new GridBagLayout());
        secProduto.setBackground(UIFactory.XP_BG);
        secProduto.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(0, 84, 166), 1),
                        "Produto",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        UIFactory.FONT_BOLD, new Color(0, 84, 166)),
                new EmptyBorder(4, 6, 6, 6)));

        GridBagConstraints gp = new GridBagConstraints();
        gp.insets = new Insets(3, 4, 3, 4);
        gp.fill = GridBagConstraints.HORIZONTAL;

        
        gp.gridx = 0; gp.gridy = 0; gp.weightx = 0;
        secProduto.add(UIFactory.labelLight("Produto:"), gp);
        gp.gridx = 1; gp.weightx = 1;
        secProduto.add(tfProduto, gp);
        JButton btnBuscarProduto = UIFactory.bigActionButton("⊞", e -> buscarProduto());
        btnBuscarProduto.setPreferredSize(new Dimension(36, 24));
        gp.gridx = 2; gp.weightx = 0;
        secProduto.add(btnBuscarProduto, gp);

        
        gp.gridx = 0; gp.gridy = 1; gp.weightx = 0;
        secProduto.add(UIFactory.labelLight("Qtd:"), gp);
        gp.gridx = 1; gp.weightx = 1;
        secProduto.add(tfQtd, gp);
        JButton btnAdicionar = UIFactory.bigActionButton("+ Adicionar", e -> adicionarItem());
        btnAdicionar.setBackground(new Color(0, 110, 0));
        btnAdicionar.setForeground(Color.WHITE);
        gp.gridx = 2; gp.weightx = 0;
        secProduto.add(btnAdicionar, gp);

        
        gp.gridx = 0; gp.gridy = 2; gp.weightx = 0;
        secProduto.add(UIFactory.labelLight("Cód. Barras:"), gp);
        gp.gridx = 1; gp.weightx = 1;
        secProduto.add(tfCodBarras, gp);
        JButton btnAddBarras = UIFactory.bigActionButton("+ Add", e -> buscarPorCodBarras());
        gp.gridx = 2; gp.weightx = 0;
        secProduto.add(btnAddBarras, gp);

        
        gp.gridx = 0; gp.gridy = 3; gp.weightx = 0;
        JLabel lblPreco = UIFactory.labelLight("Preço (R$):");
        lblPreco.setForeground(new Color(0, 84, 166));
        secProduto.add(lblPreco, gp);
        gp.gridx = 1; gp.weightx = 1;
        secProduto.add(tfPreco, gp);
        JLabel lblPrecoHint = new JLabel("← editável antes de adicionar");
        lblPrecoHint.setFont(new Font("Tahoma", Font.ITALIC, 10));
        lblPrecoHint.setForeground(new Color(120, 120, 120));
        gp.gridx = 2; gp.weightx = 0;
        secProduto.add(lblPrecoHint, gp);

        
        gp.gridx = 0; gp.gridy = 4; gp.weightx = 0;
        secProduto.add(UIFactory.labelLight("Obs:"), gp);
        gp.gridx = 1; gp.gridwidth = 2; gp.weightx = 1;
        secProduto.add(tfObs, gp);
        gp.gridwidth = 1;

        
        JPanel topo = new JPanel(new BorderLayout(0, 4));
        topo.setBackground(UIFactory.XP_BG);
        topo.setBorder(new EmptyBorder(6, 8, 4, 8));
        topo.add(secCliente, BorderLayout.NORTH);
        topo.add(secProduto, BorderLayout.CENTER);

        
        JPanel secItens = new JPanel(new BorderLayout(0, 4));
        secItens.setBackground(UIFactory.XP_BG);
        secItens.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(0, 84, 166), 1),
                        "Itens Adicionados ao Pedido",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        UIFactory.FONT_BOLD, new Color(0, 84, 166)),
                new EmptyBorder(4, 6, 6, 6)));

        itensModel = new DefaultTableModel(
                new String[]{"#", "Produto", "Qtd", "Preço Unit.", "Subtotal"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        itensTable = UIFactory.styledTable();
        itensTable.setModel(itensModel);
        itensTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        itensTable.getColumnModel().getColumn(0).setMaxWidth(50);
        itensTable.getColumnModel().getColumn(1).setPreferredWidth(280);
        itensTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        itensTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        itensTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        itensTable.setDefaultRenderer(Object.class, (t, value, isSel, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(UIFactory.FONT_NORMAL);
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(2, 5, 2, 5));
            cell.setBackground(isSel ? UIFactory.XP_TABLE_SEL
                    : (row % 2 == 0 ? Color.WHITE : new Color(240, 248, 255)));
            cell.setForeground(isSel ? Color.WHITE : Color.BLACK);
            if (col >= 2) cell.setHorizontalAlignment(SwingConstants.RIGHT);
            return cell;
        });

        JScrollPane scrollItens = new JScrollPane(itensTable);
        scrollItens.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        secItens.add(scrollItens, BorderLayout.CENTER);

        
        JPanel rodItens = new JPanel(new BorderLayout());
        rodItens.setBackground(new Color(212, 208, 200));
        rodItens.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));

        JPanel rodLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        rodLeft.setOpaque(false);
        JButton btnRemover = UIFactory.bigActionButton("⊟ Remover Item", e -> removerItem());
        btnRemover.setBackground(new Color(180, 0, 0));
        btnRemover.setForeground(Color.WHITE);
        rodLeft.add(btnRemover);
        JLabel hintRemover = new JLabel("↑↓ para navegar · Del para remover");
        hintRemover.setFont(new Font("Tahoma", Font.ITALIC, 10));
        hintRemover.setForeground(new Color(80, 80, 80));
        rodLeft.add(hintRemover);
        rodItens.add(rodLeft, BorderLayout.WEST);

        JPanel rodRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        rodRight.setOpaque(false);
        lblTotal = new JLabel("Total: R$ 0,00");
        lblTotal.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblTotal.setForeground(new Color(0, 84, 166));
        rodRight.add(lblTotal);
        rodItens.add(rodRight, BorderLayout.EAST);

        secItens.add(rodItens, BorderLayout.SOUTH);

        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        btnPanel.setBackground(new Color(212, 208, 200));
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));

        JButton btnAtualizar   = UIFactory.bigActionButton("↻ Atualizar (F5)", e -> limparFormulario());
        JButton btnVerImprimir = UIFactory.bigActionButton("🖨 Imprimir Pedido", e -> {
            String input = JOptionPane.showInputDialog(this,
                    "Digite o número do pedido para imprimir:", "Imprimir Pedido", JOptionPane.QUESTION_MESSAGE);
            if (input != null && !input.isBlank()) {
                try { abrirRelatorioPedido(Long.parseLong(input.trim())); }
                catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Número inválido.", "Erro", JOptionPane.ERROR_MESSAGE); }
            }
        });
        JButton btnReabrir = UIFactory.bigActionButton("📂 Reabrir Pedido", e -> reabrirPedido());
        btnReabrir.setBackground(new Color(140, 80, 0));
        btnReabrir.setForeground(Color.WHITE);
        JButton btnSalvar      = UIFactory.bigActionButton("✔ Salvar Pedido", e -> salvarPedido());
        btnSalvar.setBackground(new Color(0, 110, 0));
        btnSalvar.setForeground(Color.WHITE);
        btnSalvar.setFont(new Font("Tahoma", Font.BOLD, 12));
        JButton btnRemoverRod  = UIFactory.bigActionButton("✖ Remover Item", e -> removerItem());
        btnRemoverRod.setBackground(new Color(180, 0, 0));
        btnRemoverRod.setForeground(Color.WHITE);

        btnPanel.add(btnAtualizar);
        btnPanel.add(btnVerImprimir);
        btnPanel.add(btnReabrir);
        btnPanel.add(btnSalvar);
        btnPanel.add(btnRemoverRod);

        
        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setBackground(UIFactory.XP_BG);

        JPanel itensWrapper = new JPanel(new BorderLayout());
        itensWrapper.setBackground(UIFactory.XP_BG);
        itensWrapper.setBorder(new EmptyBorder(0, 8, 0, 8));
        itensWrapper.add(secItens, BorderLayout.CENTER);

        center.add(topo, BorderLayout.NORTH);
        center.add(itensWrapper, BorderLayout.CENTER);
        center.add(btnPanel, BorderLayout.SOUTH);

        main.add(center, BorderLayout.CENTER);

        
        itensTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) removerItem();
            }
        });
        tfCodBarras.addActionListener(e -> buscarPorCodBarras());
        tfProduto.addActionListener(e -> buscarProduto());
        tfQtd.addActionListener(e -> adicionarItem());

        return main;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(new Color(212, 208, 200));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));
        JLabel h = new JLabel("Dc: F2 navega · Enter abre · F5 atualiza");
        h.setFont(new Font("Tahoma", Font.ITALIC, 10));
        h.setForeground(new Color(80, 80, 80));
        bar.add(h);
        return bar;
    }

    

    private void buscarCliente() {
        String filtro = tfCliente.getText().trim().toLowerCase();
        List<Cliente> todos = services.clientes().listarPF();
        List<Cliente> lista = filtro.isEmpty() ? todos
                : todos.stream()
                .filter(c -> c.getNome() != null && c.getNome().toLowerCase().contains(filtro))
                .collect(java.util.stream.Collectors.toList());

        if (lista.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum cliente encontrado.", "Busca", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (lista.size() == 1) { selecionarCliente(lista.get(0)); return; }

        DefaultTableModel m = new DefaultTableModel(new String[]{"ID", "Nome", "CPF", "Tel"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Cliente c : lista)
            m.addRow(new Object[]{c.getId(), c.getNome(), c.getCpf(), c.getTelefone()});

        JTable table = UIFactory.styledTable();
        table.setModel(m);
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(600, 300));

        int res = JOptionPane.showConfirmDialog(this, scroll, "Selecionar Cliente",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION && table.getSelectedRow() >= 0) {
            Long id = (Long) m.getValueAt(table.getSelectedRow(), 0);
            lista.stream().filter(c -> c.getId().equals(id)).findFirst().ifPresent(this::selecionarCliente);
        }
    }

    private void selecionarCliente(Cliente c) {
        clienteSelecionadoId = c.getId();
        tfCliente.setText(c.getNome());
    }

    private void buscarProduto() {
        String filtro = tfProduto.getText().trim().toLowerCase();
        List<Produto> todos = services.produtos().listarTodos();
        List<Produto> lista = filtro.isEmpty() ? todos
                : todos.stream()
                .filter(p -> p.getNome() != null && p.getNome().toLowerCase().contains(filtro))
                .collect(java.util.stream.Collectors.toList());

        if (lista.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum produto encontrado.", "Busca", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (lista.size() == 1) { selecionarProduto(lista.get(0)); return; }

        DefaultTableModel m = new DefaultTableModel(
                new String[]{"ID", "Nome", "Preço", "Estoque", "Cód. Barras"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Produto p : lista)
            m.addRow(new Object[]{
                    p.getId(), p.getNome(),
                    String.format("R$ %.2f", p.getValor()),
                    p.getEstoque(),
                    p.getCodBarras() != null ? p.getCodBarras() : ""});

        JTable table = UIFactory.styledTable();
        table.setModel(m);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(3).setMaxWidth(70);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(700, 300));

        int res = JOptionPane.showConfirmDialog(this, scroll, "Selecionar Produto",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION && table.getSelectedRow() >= 0) {
            Long id = (Long) m.getValueAt(table.getSelectedRow(), 0);
            lista.stream().filter(p -> p.getId().equals(id)).findFirst().ifPresent(this::selecionarProduto);
        }
    }

    private void selecionarProduto(Produto p) {
        produtoSelecionadoId = p.getId();
        tfProduto.setText(p.getNome());
        tfCodBarras.setText(p.getCodBarras() != null ? p.getCodBarras() : "");
        tfPreco.setText(p.getValor() != null ? String.format("%.2f", p.getValor()) : "0.00");
        tfQtd.requestFocus();
        tfQtd.selectAll();
    }

    private void buscarPorCodBarras() {
        String cod = tfCodBarras.getText().trim();
        if (cod.isEmpty()) return;
        services.produtos().buscarPorCodBarras(cod).ifPresentOrElse(
                p -> { selecionarProduto(p); adicionarItem(); },
                () -> JOptionPane.showMessageDialog(this,
                        "Produto com código '" + cod + "' não encontrado.",
                        "Não encontrado", JOptionPane.WARNING_MESSAGE));
    }

    private void adicionarItem() {
        if (produtoSelecionadoId == null) {
            JOptionPane.showMessageDialog(this, "Selecione um produto primeiro.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int qtd;
        try {
            qtd = Integer.parseInt(tfQtd.getText().trim());
            if (qtd <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Informe uma quantidade válida (número inteiro > 0).", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal preco;
        try {
            preco = Validator.parseBigDecimal(tfPreco.getText().trim());
            if (preco.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Informe um preço de venda válido (maior que zero).", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final Long prodId = produtoSelecionadoId;
        final String nomeProd = tfProduto.getText().trim();

        for (ItemRascunho item : itensRascunho) {
            if (item.produtoId.equals(prodId)) {
                item.quantidade += qtd;
                item.precoUnitario = preco;
                atualizarTabelaItens();
                limparCamposProduto();
                return;
            }
        }

        itensRascunho.add(new ItemRascunho(prodId, nomeProd, qtd, preco));
        atualizarTabelaItens();
        limparCamposProduto();
    }

    private void removerItem() {
        int row = itensTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um item para remover.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        itensRascunho.remove(row);
        atualizarTabelaItens();
    }

    private void atualizarTabelaItens() {
        itensModel.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        int seq = 1;
        for (ItemRascunho item : itensRascunho) {
            BigDecimal sub = item.precoUnitario.multiply(new BigDecimal(item.quantidade));
            total = total.add(sub);
            itensModel.addRow(new Object[]{
                    seq++,
                    item.nomeProduto,
                    item.quantidade,
                    String.format("R$ %.2f", item.precoUnitario),
                    String.format("R$ %.2f", sub)
            });
        }
        lblTotal.setText("Total: R$ " + String.format("%.2f", total));
    }

    private void limparCamposProduto() {
        produtoSelecionadoId = null;
        tfProduto.setText("");
        tfCodBarras.setText("");
        tfPreco.setText("");
        tfQtd.setText("1");
        tfProduto.requestFocus();
    }

    private void limparFormulario() {
        clienteSelecionadoId = null;
        tfCliente.setText("");
        tfObs.setText("");
        itensRascunho.clear();
        atualizarTabelaItens();
        limparCamposProduto();
    }

    private void salvarPedido() {
        if (clienteSelecionadoId == null) {
            JOptionPane.showMessageDialog(this, "Selecione um cliente.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (itensRascunho.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Adicione pelo menos um item.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<PedidoService.ItemPedidoDTO> dtos = new ArrayList<>();
        for (ItemRascunho r : itensRascunho)
            dtos.add(new PedidoService.ItemPedidoDTO(r.produtoId, r.quantidade, r.precoUnitario));

        try {
            var pedido = services.pedidos().criarPedidoPendente(
                    clienteSelecionadoId, dtos, tfObs.getText().trim());

            limparFormulario();

            int resp = JOptionPane.showConfirmDialog(this,
                    "✅ Pedido #" + pedido.getId() + " criado com sucesso!\n"
                            + "Status: PENDENTE\n"
                            + "Total: R$ " + String.format("%.2f", pedido.getValorTotal())
                            + "\n\nDeseja imprimir o comprovante agora?",
                    "Pedido Salvo", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

            if (resp == JOptionPane.YES_OPTION) {
                abrirRelatorioPedido(pedido.getId());
            }

        } catch (RegraNegocioException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro de Negócio", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar pedido: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void reabrirPedido() {
        String input = JOptionPane.showInputDialog(this,
                "Digite o número do pedido que deseja reabrir para alterações:",
                "Reabrir Pedido", JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.isBlank()) return;

        long pedidoId;
        try {
            pedidoId = Long.parseLong(input.trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Número de pedido inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            var pedido = services.pedidos().buscarComItens(pedidoId);

            
            if (pedido.getStatus() != br.carmel.model.StatusPedido.PENDENTE) {
                String motivo = switch (pedido.getStatus()) {
                    case CONFIRMADO    -> "já foi confirmado (nota emitida). Cancele a nota primeiro se precisar alterar.";
                    case EM_PREPARACAO -> "está em preparação e não pode ser editado.";
                    case ENVIADO       -> "já foi enviado e não pode ser editado.";
                    case ENTREGUE      -> "já foi entregue e não pode ser editado.";
                    case CANCELADO     -> "já está cancelado.";
                    default            -> "não está mais pendente.";
                };
                JOptionPane.showMessageDialog(this,
                        "O pedido #" + pedidoId + " " + motivo,
                        "Pedido Não Editável", JOptionPane.WARNING_MESSAGE);
                return;
            }

            
            int conf = JOptionPane.showConfirmDialog(this,
                    "Pedido #" + pedidoId + " — Cliente: " + pedido.getCliente().getNome()
                            + "\nTotal: R$ " + String.format("%.2f", pedido.getValorTotal())
                            + "\nItens: " + pedido.getItensPedidos().size()
                            + "\n\nO pedido será CANCELADO e um novo será gerado ao salvar.\nDeseja continuar?",
                    "Confirmar Reabertura", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (conf != JOptionPane.YES_OPTION) return;

            
            services.pedidos().cancelarPedido(pedidoId);

            
            String prefixo = "[Reaberto do pedido #" + pedidoId + "] ";
            String obsOriginal = pedido.getObservacoes() != null ? pedido.getObservacoes() : "";
            String obsNova = (prefixo + obsOriginal).length() <= 500
                    ? prefixo + obsOriginal
                    : (prefixo + obsOriginal).substring(0, 500);

            
            limparFormulario();
            clienteSelecionadoId = pedido.getCliente().getId();
            tfCliente.setText(pedido.getCliente().getNome());
            tfObs.setText(obsNova);

            for (br.carmel.model.ItensPedido item : pedido.getItensPedidos()) {
                itensRascunho.add(new ItemRascunho(
                        item.getProduto().getId(),
                        item.getProduto().getNome(),
                        item.getQuantidade(),
                        item.getPrecoUnitario()));
            }
            atualizarTabelaItens();

            JOptionPane.showMessageDialog(this,
                    "Pedido #" + pedidoId + " carregado para edição.\n"
                    + "Faça as alterações e clique em \"Salvar Pedido\" para criar o novo pedido.",
                    "Pedido Reaberto", JOptionPane.INFORMATION_MESSAGE);

        } catch (RegraNegocioException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro de Negócio", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao reabrir pedido: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirRelatorioPedido(Long pedidoId) {
        try {
            var pedido = services.pedidos().buscarComItens(pedidoId);
            new br.carmel.ui.dialogs.RelatorioPedidoDialog(
                    SwingUtilities.getWindowAncestor(this), pedido, null);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao abrir relatório: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    

    private static class ItemRascunho {
        Long produtoId;
        String nomeProduto;
        int quantidade;
        BigDecimal precoUnitario;

        ItemRascunho(Long produtoId, String nomeProduto, int quantidade, BigDecimal precoUnitario) {
            this.produtoId     = produtoId;
            this.nomeProduto   = nomeProduto;
            this.quantidade    = quantidade;
            this.precoUnitario = precoUnitario;
        }
    }
}