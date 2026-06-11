package br.carmel.ui;

import br.carmel.util.LicencaManager;
import br.carmel.util.LicencaManager.ResultadoLicenca;
import br.carmel.util.LicencaManager.ChaveJaUtilizadaException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Diálogo de verificação/ativação de licença exibido na inicialização.
 *
 * SEM_LICENCA / EXPIRADA / CORROMPIDA / CHAVE_JA_UTILIZADA → bloqueia.
 * VALIDA com ≤7 dias → aviso, mas deixa entrar.
 * VALIDA normal       → passa direto, sem diálogo.
 */
public class LicencaDialog extends JDialog {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Cores
    private static final Color COR_FUNDO        = new Color(28, 28, 38);
    private static final Color COR_CAMPO_FUNDO  = new Color(18, 18, 28);
    private static final Color COR_VERDE         = new Color(34, 160, 70);
    private static final Color COR_AZUL          = new Color(30, 110, 200);
    private static final Color COR_VERMELHO      = new Color(160, 35, 35);
    private static final Color COR_TEXTO         = new Color(210, 210, 220);
    private static final Color COR_LABEL         = new Color(160, 200, 255);
    private static final Color COR_CHAVE_TEXTO   = new Color(100, 230, 120);
    private static final Color COR_BORDA_CAMPO   = new Color(60, 120, 200);

    private boolean autorizado = false;

    // ── Fábrica estática ──────────────────────────────────────────────────────

    public static boolean verificarEExibir(Frame parent) {
        ResultadoLicenca resultado = LicencaManager.verificar();

        return switch (resultado.status()) {
            case VALIDA -> {
                if (resultado.diasRestantes() <= 7) {
                    LicencaDialog dlg = new LicencaDialog(parent, resultado, false);
                    dlg.setVisible(true);
                    yield dlg.autorizado;
                }
                yield true; // válida e longe de vencer — passa direto
            }
            case SEM_LICENCA, EXPIRADA, CORROMPIDA, CHAVE_JA_UTILIZADA -> {
                LicencaDialog dlg = new LicencaDialog(parent, resultado, true);
                dlg.setVisible(true);
                yield dlg.autorizado;
            }
        };
    }

    // ── Construtor ────────────────────────────────────────────────────────────

    private LicencaDialog(Frame parent, ResultadoLicenca resultado, boolean bloqueado) {
        super(parent, "Licença do Sistema — Carmel", true);
        setSize(520, bloqueado ? 380 : 240);
        setResizable(false);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(bloqueado ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        add(criarHeader(resultado), BorderLayout.NORTH);
        add(criarCorpo(resultado, bloqueado), BorderLayout.CENTER);
    }

    // ── Corpo da UI ───────────────────────────────────────────────────────────

    private JPanel criarCorpo(ResultadoLicenca resultado, boolean bloqueado) {
        JPanel corpo = new JPanel();
        corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
        corpo.setBackground(COR_FUNDO);
        corpo.setBorder(new EmptyBorder(18, 24, 12, 24));

        // Texto informativo
        JLabel lblInfo = new JLabel("<html>" + montarTextoInfo(resultado) + "</html>");
        lblInfo.setForeground(COR_TEXTO);
        lblInfo.setFont(new Font("Tahoma", Font.PLAIN, 12));
        lblInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        corpo.add(lblInfo);

        if (bloqueado) {
            corpo.add(Box.createVerticalStrut(18));

            // Label do campo
            JLabel lblChave = new JLabel("Chave de ativação:");
            lblChave.setForeground(COR_LABEL);
            lblChave.setFont(new Font("Tahoma", Font.BOLD, 11));
            lblChave.setAlignmentX(Component.LEFT_ALIGNMENT);
            corpo.add(lblChave);
            corpo.add(Box.createVerticalStrut(6));

            // Campo de texto para a chave
            JTextArea tfChave = new JTextArea(3, 38);
            tfChave.setFont(new Font("Consolas", Font.PLAIN, 12));
            tfChave.setBackground(COR_CAMPO_FUNDO);
            tfChave.setForeground(COR_CHAVE_TEXTO);
            tfChave.setCaretColor(Color.WHITE);
            tfChave.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COR_BORDA_CAMPO, 1),
                    new EmptyBorder(6, 8, 6, 8)
            ));
            tfChave.setLineWrap(true);
            tfChave.setWrapStyleWord(false);
            JScrollPane scroll = new JScrollPane(tfChave);
            scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
            scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
            scroll.setBorder(null);
            corpo.add(scroll);

            // Hint de formato
            JLabel lblHint = new JLabel("  Formato: XXXXX-XXXXX-XXXXX-XXXXX-XXXXX-XXXXX");
            lblHint.setForeground(new Color(120, 130, 150));
            lblHint.setFont(new Font("Tahoma", Font.ITALIC, 10));
            lblHint.setAlignmentX(Component.LEFT_ALIGNMENT);
            corpo.add(lblHint);

            corpo.add(Box.createVerticalStrut(16));

            // Botões
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            btns.setOpaque(false);
            btns.setAlignmentX(Component.LEFT_ALIGNMENT);

            JButton btnAtivar = criarBotao("  Ativar Licença  ", COR_VERDE, true);
            btnAtivar.addActionListener(e -> tentarAtivar(tfChave.getText()));

            JButton btnFechar = criarBotao("Fechar", COR_VERMELHO, false);
            btnFechar.addActionListener(e -> { autorizado = false; dispose(); });

            btns.add(btnAtivar);
            btns.add(btnFechar);
            corpo.add(btns);

        } else {
            // Aviso de vencimento próximo
            corpo.add(Box.createVerticalStrut(14));
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            btns.setOpaque(false);
            btns.setAlignmentX(Component.LEFT_ALIGNMENT);

            JButton btnContinuar = criarBotao("  Continuar  ", COR_AZUL, true);
            btnContinuar.addActionListener(e -> { autorizado = true; dispose(); });
            btns.add(btnContinuar);
            corpo.add(btns);
            autorizado = true;
        }

        return corpo;
    }

    // ── Ativação ──────────────────────────────────────────────────────────────

    private void tentarAtivar(String chaveDigitada) {
        String chave = chaveDigitada.trim();
        if (chave.isEmpty()) {
            avisar("Cole ou digite a chave de ativação no campo acima.", "Atenção");
            return;
        }

        try {
            boolean ok = LicencaManager.renovar(chave);
            if (ok) {
                ResultadoLicenca nova = LicencaManager.verificar();
                String dataExp = nova.dataInicio() != null
                        ? nova.dataInicio().plusDays(LicencaManager.DIAS_VALIDADE).format(FMT)
                        : "-";
                JOptionPane.showMessageDialog(this,
                        "✔  Licença ativada com sucesso!\n\nVálida até: " + dataExp,
                        "Licença Ativada", JOptionPane.INFORMATION_MESSAGE);
                autorizado = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Chave inválida, incorreta ou já expirada.\n\n" +
                        "Verifique se a chave foi digitada corretamente\n" +
                        "e contate o suporte para obter uma nova chave.",
                        "Chave Inválida", JOptionPane.ERROR_MESSAGE);
            }

        } catch (LicencaManager.ChaveJaUtilizadaException ex) {
            JOptionPane.showMessageDialog(this,
                    "⚠  Esta chave já foi utilizada anteriormente.\n\n" +
                    "Uma chave de licença só pode ser ativada uma única vez.\n" +
                    "Solicite uma nova chave ao suporte.",
                    "Chave Já Utilizada", JOptionPane.WARNING_MESSAGE);

        } catch (LicencaManager.SemInternetException ex) {
            JOptionPane.showMessageDialog(this,
                    "🌐  Sem conexão com a internet.\n\n" +
                    "A ativação da licença requer acesso à internet\n" +
                    "para verificar a data atual com segurança.\n\n" +
                    "Conecte-se e tente novamente.",
                    "Sem Internet", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ── Componentes de UI ─────────────────────────────────────────────────────

    private JPanel criarHeader(ResultadoLicenca r) {
        Color cor = switch (r.status()) {
            case VALIDA                                   -> new Color(160, 110, 0);
            case SEM_LICENCA, EXPIRADA, CHAVE_JA_UTILIZADA -> new Color(150, 30, 30);
            case CORROMPIDA                               -> new Color(110, 55, 0);
        };
        String icone = switch (r.status()) {
            case VALIDA              -> "⚠";
            case SEM_LICENCA        -> "🔒";
            case EXPIRADA           -> "🔒";
            case CORROMPIDA         -> "⚠";
            case CHAVE_JA_UTILIZADA -> "⛔";
        };
        String titulo = switch (r.status()) {
            case VALIDA              -> "Licença vencendo em breve";
            case SEM_LICENCA        -> "Sistema não ativado";
            case EXPIRADA           -> "Licença Expirada";
            case CORROMPIDA         -> "Licença Inválida ou Corrompida";
            case CHAVE_JA_UTILIZADA -> "Chave já utilizada";
        };

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 12)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(cor);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        JLabel lbl = new JLabel(icone + "  " + titulo);
        lbl.setFont(new Font("Tahoma", Font.BOLD, 15));
        lbl.setForeground(Color.WHITE);
        header.add(lbl);
        return header;
    }

    private JButton criarBotao(String texto, Color cor, boolean negrito) {
        JButton btn = new JButton(texto);
        btn.setBackground(cor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Tahoma", negrito ? Font.BOLD : Font.PLAIN, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void avisar(String mensagem, String titulo) {
        JOptionPane.showMessageDialog(this, mensagem, titulo, JOptionPane.WARNING_MESSAGE);
    }

    private String montarTextoInfo(ResultadoLicenca r) {
        return switch (r.status()) {
            case SEM_LICENCA ->
                "Este sistema ainda <b>não foi ativado</b>.<br><br>" +
                "Insira a chave de ativação fornecida pelo suporte para liberar o acesso.";

            case EXPIRADA ->
                "Sua licença expirou em <b>" +
                (r.dataInicio() != null
                        ? r.dataInicio().plusDays(LicencaManager.DIAS_VALIDADE).format(FMT)
                        : "-") +
                "</b>.<br><br>" +
                "Insira uma nova chave de ativação para continuar usando o sistema.";

            case CORROMPIDA ->
                "O arquivo de licença está <b>corrompido</b> ou foi adulterado.<br><br>" +
                "Contate o suporte para obter uma nova chave de ativação.";

            case CHAVE_JA_UTILIZADA ->
                "A chave informada <b>já foi utilizada</b> anteriormente.<br><br>" +
                "Cada chave de licença pode ser usada uma única vez.<br>" +
                "Solicite uma nova chave ao suporte.";

            case VALIDA ->
                "Sua licença vence em <b>" + r.diasRestantes() + " dia(s)</b>.<br>" +
                "Entre em contato com o suporte para renovar antes do vencimento.";
        };
    }
}