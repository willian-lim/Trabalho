package br.carmel.ui.panels;

import br.carmel.model.*;
import br.carmel.service.EstoqueService;
import br.carmel.service.RegraNegocioException;
import br.carmel.service.ServiceLocator;
import br.carmel.util.UIFactory;
import br.carmel.util.Validator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class NotaTransferenciaPanel extends JPanel {

    private final ServiceLocator services;

    private static final DateTimeFormatter FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    
    private JComboBox<String>     cbTipo;
    private JComboBox<Fornecedor> cbFornecedor;
    private JTextField            tfNumeroNota;
    private JTextArea             taObs;
    private JComboBox<Produto>    cbProduto;
    private JTextField            tfQtd, tfPreco;
    private DefaultListModel<EstoqueService.ItemNotaDTO> itensModel = new DefaultListModel<>();
    private JList<EstoqueService.ItemNotaDTO> lstItens;
    private List<Produto> produtosList = new ArrayList<>();

    
    private JComboBox<String> cbFiltroTipo;
    private JTextField        tfFiltroDe, tfFiltroAte;
    private DefaultTableModel notasModel;
    private List<NotaTransferencia> notasCarregadas = new ArrayList<>();

    public NotaTransferenciaPanel(ServiceLocator services) {
        this.services = services;
        setLayout(new BorderLayout());
        setBackground(UIFactory.XP_BG);
        build();
    }

    public void carregarFornecedores() {
        cbFornecedor.removeAllItems();
        cbFornecedor.addItem(null);
        services.clientes().listarFornecedores().forEach(cbFornecedor::addItem);

        cbProduto.removeAllItems();
        produtosList = services.produtos().listarTodos();
        produtosList.forEach(cbProduto::addItem);
    }

    

    private void build() {
        add(UIFactory.xpTitleBar("Notas de Transferência — Entrada / Saída de Estoque"), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIFactory.FONT_BOLD);
        tabs.setBackground(UIFactory.XP_BG);
        tabs.addTab("📥 Emitir Nota",        buildEmitirPanel());
        tabs.addTab("🔍 Consultar Notas",    buildConsultaPanel());
        add(tabs, BorderLayout.CENTER);
    }

    

    private JPanel buildEmitirPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(UIFactory.groupBorder("Dados da Nota"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6); c.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(UIFactory.labelLight("Tipo:"), c);
        cbTipo = new JComboBox<>(new String[]{"ENTRADA", "SAIDA"});
        cbTipo.setFont(UIFactory.FONT_NORMAL);
        c.gridx = 1; c.weightx = 1; form.add(cbTipo, c); row++;

        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(UIFactory.labelLight("Fornecedor (opcional):"), c);
        cbFornecedor = new JComboBox<>();
        cbFornecedor.setFont(UIFactory.FONT_NORMAL);
        cbFornecedor.setRenderer((list, value, index, isSel, cellHasFocus) -> {
            JLabel l = new JLabel(value != null ? value.getRazaoSocial() : "— Sem fornecedor —");
            l.setFont(UIFactory.FONT_NORMAL); return l;
        });
        c.gridx = 1; c.weightx = 1; form.add(cbFornecedor, c); row++;

        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(UIFactory.labelLight("Nº Referência:"), c);
        tfNumeroNota = UIFactory.styledField("");
        c.gridx = 1; c.weightx = 1; form.add(tfNumeroNota, c); row++;

        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(UIFactory.labelLight("Observações:"), c);
        taObs = UIFactory.styledTextArea(3, 20);
        c.gridx = 1; c.weightx = 1; form.add(new JScrollPane(taObs), c); row++;

        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(UIFactory.labelLight("Produto:"), c);
        cbProduto = new JComboBox<>(); cbProduto.setFont(UIFactory.FONT_NORMAL);
        c.gridx = 1; c.weightx = 1; form.add(cbProduto, c); row++;

        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(UIFactory.labelLight("Quantidade:"), c);
        tfQtd = UIFactory.styledField("1");
        c.gridx = 1; c.weightx = 1; form.add(tfQtd, c); row++;

        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(UIFactory.labelLight("Preço Unit. (R$):"), c);
        tfPreco = UIFactory.styledField(""); tfPreco.setToolTipText("Opcional — atualiza preço médio na entrada");
        c.gridx = 1; c.weightx = 1; form.add(tfPreco, c); row++;

        JButton btnAddItem = UIFactory.bigActionButton("+ Adicionar Item", e -> adicionarItem());
        btnAddItem.setBackground(new Color(0, 84, 166)); btnAddItem.setForeground(Color.WHITE);
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; form.add(btnAddItem, c);
        p.add(form, BorderLayout.WEST);

        JPanel direita = new JPanel(new BorderLayout(0, 6));
        direita.setBackground(UIFactory.XP_BG);
        direita.setBorder(UIFactory.groupBorder("Itens da Nota"));

        itensModel = new DefaultListModel<>();
        lstItens = new JList<>(itensModel);
        lstItens.setFont(UIFactory.FONT_NORMAL);
        lstItens.setBackground(Color.WHITE);
        lstItens.setCellRenderer((list, value, index, isSel, hasFocus) -> {
            Produto prod = produtosList.stream()
                    .filter(pr -> pr.getId().equals(value.produtoId()))
                    .findFirst().orElse(null);
            String nome = prod != null ? prod.getNome() : "ID:" + value.produtoId();
            String preco = value.precoUnitario() != null
                    ? String.format(" @ R$ %.2f", value.precoUnitario()) : "";
            JLabel l = new JLabel(String.format("  %s  x%d%s", nome, value.quantidade(), preco));
            l.setFont(UIFactory.FONT_NORMAL); l.setOpaque(true);
            l.setBackground(isSel ? UIFactory.XP_TABLE_SEL : Color.WHITE);
            l.setForeground(isSel ? Color.WHITE : Color.BLACK);
            l.setBorder(new EmptyBorder(2, 4, 2, 4));
            return l;
        });

        JScrollPane scrollItens = new JScrollPane(lstItens);
        scrollItens.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        direita.add(scrollItens, BorderLayout.CENTER);

        JPanel btnItens = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        btnItens.setOpaque(false);
        JButton btnRemover = UIFactory.bigActionButton("− Remover", e -> {
            int sel = lstItens.getSelectedIndex(); if (sel >= 0) itensModel.remove(sel);
        });
        JButton btnLimpar = UIFactory.bigActionButton("Limpar Tudo", e -> itensModel.clear());
        btnItens.add(btnRemover); btnItens.add(btnLimpar);
        direita.add(btnItens, BorderLayout.NORTH);

        JButton btnEmitir = UIFactory.bigActionButton("✔ Emitir Nota", e -> emitirNota());
        btnEmitir.setBackground(new Color(0, 110, 0)); btnEmitir.setForeground(Color.WHITE);
        btnEmitir.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnEmitir.setPreferredSize(new Dimension(0, 40));
        direita.add(btnEmitir, BorderLayout.SOUTH);

        p.add(direita, BorderLayout.CENTER);
        return p;
    }

    

    private JPanel buildConsultaPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filtros.setBackground(UIFactory.XP_PANEL_BG);
        filtros.setBorder(UIFactory.groupBorder("Filtros"));

        filtros.add(UIFactory.labelLight("Tipo:"));
        cbFiltroTipo = new JComboBox<>(new String[]{"TODOS", "ENTRADA", "SAÍDA"});
        cbFiltroTipo.setFont(UIFactory.FONT_NORMAL);
        cbFiltroTipo.setPreferredSize(new Dimension(100, 22));
        filtros.add(cbFiltroTipo);

        filtros.add(UIFactory.labelLight("De:"));
        tfFiltroDe = UIFactory.styledField("");
        tfFiltroDe.setPreferredSize(new Dimension(95, 22));
        tfFiltroDe.setToolTipText("dd/MM/yyyy");
        filtros.add(tfFiltroDe);

        filtros.add(UIFactory.labelLight("Até:"));
        tfFiltroAte = UIFactory.styledField("");
        tfFiltroAte.setPreferredSize(new Dimension(95, 22));
        tfFiltroAte.setToolTipText("dd/MM/yyyy");
        filtros.add(tfFiltroAte);

        JButton btnBuscar = UIFactory.bigActionButton("🔍 Buscar", e -> buscarNotas());
        JButton btnLimpar = UIFactory.bigActionButton("Limpar", e -> {
            cbFiltroTipo.setSelectedIndex(0);
            tfFiltroDe.setText(""); tfFiltroAte.setText("");
            buscarNotas();
        });
        btnBuscar.setBackground(new Color(0, 84, 166)); btnBuscar.setForeground(Color.WHITE);
        filtros.add(btnBuscar); filtros.add(btnLimpar);
        p.add(filtros, BorderLayout.NORTH);

        
        notasModel = new DefaultTableModel(
                new String[]{"ID", "Tipo", "Data", "Nº Referência", "Fornecedor", "Qtd Itens", "Total (R$)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable tabela = UIFactory.styledTable();
        tabela.setModel(notasModel);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(50);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(70);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(130);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(120);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(180);
        tabela.getColumnModel().getColumn(5).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(6).setPreferredWidth(100);

        tabela.setDefaultRenderer(Object.class, (tbl, value, isSel, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(UIFactory.FONT_NORMAL);
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(1, 5, 1, 5));
            if (col >= 5) cell.setHorizontalAlignment(SwingConstants.RIGHT);
            if (isSel) {
                cell.setBackground(UIFactory.XP_TABLE_SEL); cell.setForeground(Color.WHITE);
            } else {
                String tipo = notasModel.getValueAt(row, 1) != null ? notasModel.getValueAt(row, 1).toString() : "";
                cell.setBackground("Entrada".equals(tipo)
                        ? new Color(220, 240, 220)
                        : "Saída".equals(tipo) ? new Color(255, 225, 220)
                        : (row % 2 == 0 ? Color.WHITE : new Color(248, 248, 252)));
                cell.setForeground(Color.BLACK);
            }
            return cell;
        });

        
        tabela.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && tabela.getSelectedRow() >= 0) {
                    int idx = tabela.getSelectedRow();
                    if (idx < notasCarregadas.size())
                        abrirDetalhes(notasCarregadas.get(idx));
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        p.add(scroll, BorderLayout.CENTER);

        
        JPanel rod = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        rod.setBackground(new Color(212, 208, 200));
        rod.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));

        JButton btnVer = UIFactory.bigActionButton("🔎 Ver Detalhes", e -> {
            int sel = tabela.getSelectedRow();
            if (sel < 0) { JOptionPane.showMessageDialog(p, "Selecione uma nota."); return; }
            if (sel < notasCarregadas.size()) abrirDetalhes(notasCarregadas.get(sel));
        });
        btnVer.setBackground(new Color(0, 84, 166)); btnVer.setForeground(Color.WHITE);

        rod.add(UIFactory.bigActionButton("↻ Atualizar", e -> buscarNotas()));
        rod.add(btnVer);

        JLabel dica = new JLabel("  Dica: duplo clique na linha para ver os itens da nota.");
        dica.setFont(new Font("Tahoma", Font.ITALIC, 10));
        dica.setForeground(new Color(80, 80, 80));
        rod.add(dica);

        p.add(rod, BorderLayout.SOUTH);
        return p;
    }

    

    private void buscarNotas() {
        if (notasModel == null) return;
        notasModel.setRowCount(0);
        notasCarregadas.clear();

        
        TipoNota tipo = null;
        String tipoSel = (String) cbFiltroTipo.getSelectedItem();
        if ("ENTRADA".equals(tipoSel)) tipo = TipoNota.ENTRADA;
        else if ("SAÍDA".equals(tipoSel)) tipo = TipoNota.SAIDA;

        
        LocalDateTime de = null, ate = null;
        try {
            if (!Validator.isBlank(tfFiltroDe.getText()))
                de = LocalDate.parse(tfFiltroDe.getText().trim(), FMT_DIA).atStartOfDay();
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Data 'De' inválida. Use dd/MM/yyyy."); return;
        }
        try {
            if (!Validator.isBlank(tfFiltroAte.getText()))
                ate = LocalDate.parse(tfFiltroAte.getText().trim(), FMT_DIA).atTime(LocalTime.MAX);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Data 'Até' inválida. Use dd/MM/yyyy."); return;
        }

        try {
            List<NotaTransferencia> notas = services.estoque().listarNotas(tipo, de, ate);
            notasCarregadas.addAll(notas);
            for (NotaTransferencia n : notas) {
                String fornNome = n.getFornecedor() != null ? n.getFornecedor().getRazaoSocial() : "—";
                BigDecimal total = n.getValorTotal() != null ? n.getValorTotal() : BigDecimal.ZERO;
                notasModel.addRow(new Object[]{
                        n.getId(),
                        n.getTipo().toString(),
                        n.getDataNota().format(FMT),
                        n.getNumeroNota() != null && !n.getNumeroNota().isBlank() ? n.getNumeroNota() : "—",
                        fornNome,
                        n.getItens().size(),
                        String.format("R$ %.2f", total)
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao buscar notas: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void abrirDetalhes(NotaTransferencia nota) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Detalhes da Nota #" + nota.getId() + " — " + nota.getTipo(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(600, 440); dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        
        JPanel header = new JPanel(new GridLayout(0, 2, 4, 2));
        header.setBackground(UIFactory.XP_BG);
        header.setBorder(UIFactory.groupBorder("Dados da Nota"));

        addDetalhe(header, "ID:",            "#" + nota.getId());
        addDetalhe(header, "Tipo:",          nota.getTipo().toString());
        addDetalhe(header, "Data:",          nota.getDataNota().format(FMT));
        addDetalhe(header, "Nº Referência:", nota.getNumeroNota() != null && !nota.getNumeroNota().isBlank()
                                              ? nota.getNumeroNota() : "—");
        addDetalhe(header, "Fornecedor:",    nota.getFornecedor() != null
                                              ? nota.getFornecedor().getRazaoSocial() : "—");
        addDetalhe(header, "Observações:",   nota.getObservacoes() != null && !nota.getObservacoes().isBlank()
                                              ? nota.getObservacoes() : "—");
        BigDecimal total = nota.getValorTotal() != null ? nota.getValorTotal() : BigDecimal.ZERO;
        addDetalhe(header, "Total:",         String.format("R$ %.2f", total));
        dlg.add(header, BorderLayout.NORTH);

        
        DefaultTableModel itensTabModel = new DefaultTableModel(
                new String[]{"Produto", "Quantidade", "Preço Unit. (R$)", "Subtotal (R$)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        for (ItemNota item : nota.getItens()) {
            String nomeProd = item.getProduto() != null ? item.getProduto().getNome() : "—";
            String precoStr = item.getPrecoUnitario() != null
                    ? String.format("R$ %.2f", item.getPrecoUnitario()) : "—";
            String subStr   = item.getSubtotal() != null
                    ? String.format("R$ %.2f", item.getSubtotal()) : "—";
            itensTabModel.addRow(new Object[]{ nomeProd, item.getQuantidade(), precoStr, subStr });
        }

        JTable tabItens = UIFactory.styledTable();
        tabItens.setModel(itensTabModel);
        tabItens.getColumnModel().getColumn(0).setPreferredWidth(240);
        tabItens.getColumnModel().getColumn(1).setPreferredWidth(80);
        tabItens.getColumnModel().getColumn(2).setPreferredWidth(110);
        tabItens.getColumnModel().getColumn(3).setPreferredWidth(110);

        tabItens.setDefaultRenderer(Object.class, (tbl, value, isSel, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(UIFactory.FONT_NORMAL); cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(2, 6, 2, 6));
            if (col >= 1) cell.setHorizontalAlignment(SwingConstants.RIGHT);
            cell.setBackground(isSel ? UIFactory.XP_TABLE_SEL : (row % 2 == 0 ? Color.WHITE : new Color(248, 248, 252)));
            cell.setForeground(isSel ? Color.WHITE : Color.BLACK);
            return cell;
        });

        JPanel itensPanel = new JPanel(new BorderLayout(0, 4));
        itensPanel.setBackground(UIFactory.XP_BG);
        itensPanel.setBorder(UIFactory.groupBorder("Itens"));
        JScrollPane scrollItens = new JScrollPane(tabItens);
        scrollItens.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        itensPanel.add(scrollItens, BorderLayout.CENTER);
        dlg.add(itensPanel, BorderLayout.CENTER);

        
        JPanel rod = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        rod.setBackground(UIFactory.XP_BG);
        rod.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));
        JButton btnFechar = UIFactory.bigActionButton("Fechar", e -> dlg.dispose());
        rod.add(btnFechar);
        dlg.add(rod, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void addDetalhe(JPanel p, String label, String value) {
        JLabel lbl = new JLabel(label); lbl.setFont(UIFactory.FONT_BOLD);
        JLabel val = new JLabel(value); val.setFont(UIFactory.FONT_NORMAL);
        lbl.setBorder(new EmptyBorder(2, 6, 2, 4));
        val.setBorder(new EmptyBorder(2, 2, 2, 6));
        p.add(lbl); p.add(val);
    }

    

    private void adicionarItem() {
        Produto prod = (Produto) cbProduto.getSelectedItem();
        if (prod == null) { JOptionPane.showMessageDialog(this, "Selecione um produto."); return; }
        int qtd;
        try {
            qtd = Integer.parseInt(tfQtd.getText().trim());
            if (qtd <= 0) throw new Exception();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Quantidade inválida."); return; }

        BigDecimal preco = null;
        if (!Validator.isBlank(tfPreco.getText())) {
            try { preco = Validator.parseBigDecimal(tfPreco.getText()); }
            catch (Exception e) { JOptionPane.showMessageDialog(this, "Preço inválido."); return; }
        }

        for (int i = 0; i < itensModel.size(); i++) {
            if (itensModel.get(i).produtoId().equals(prod.getId())) {
                EstoqueService.ItemNotaDTO ex = itensModel.get(i);
                itensModel.set(i, new EstoqueService.ItemNotaDTO(
                        prod.getId(), ex.quantidade() + qtd, preco != null ? preco : ex.precoUnitario()));
                tfQtd.setText("1"); tfPreco.setText("");
                return;
            }
        }
        itensModel.addElement(new EstoqueService.ItemNotaDTO(prod.getId(), qtd, preco));
        tfQtd.setText("1"); tfPreco.setText("");
    }

    private void emitirNota() {
        if (itensModel.isEmpty()) { JOptionPane.showMessageDialog(this, "Adicione pelo menos um item."); return; }

        List<EstoqueService.ItemNotaDTO> itens = new ArrayList<>();
        for (int i = 0; i < itensModel.size(); i++) itens.add(itensModel.get(i));

        Fornecedor forn = (Fornecedor) cbFornecedor.getSelectedItem();
        Long fornId = forn != null ? forn.getId() : null;
        String tipo = (String) cbTipo.getSelectedItem();

        try {
            NotaTransferencia nota;
            if ("ENTRADA".equals(tipo)) {
                nota = services.estoque().emitirEntrada(fornId, tfNumeroNota.getText().trim(),
                        taObs.getText().trim(), itens);
            } else {
                nota = services.estoque().emitirSaida(fornId, tfNumeroNota.getText().trim(),
                        taObs.getText().trim(), itens);
            }

            JOptionPane.showMessageDialog(this,
                    "✅ Nota de " + tipo + " emitida!\n"
                            + "Nota Nº: " + (nota.getNumeroNota() != null ? nota.getNumeroNota() : "—") + "\n"
                            + "Itens: " + itens.size() + "\n"
                            + "Total: R$ " + String.format("%.2f", nota.getValorTotal() != null ? nota.getValorTotal() : BigDecimal.ZERO),
                    "Nota Emitida", JOptionPane.INFORMATION_MESSAGE);

            itensModel.clear();
            tfNumeroNota.setText(""); taObs.setText(""); tfQtd.setText("1"); tfPreco.setText("");

        } catch (RegraNegocioException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro de Negócio", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao emitir nota: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}
