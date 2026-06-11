package br.carmel.service;

import br.carmel.model.*;
import br.carmel.repository.CaixaRepository;

import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras de negócio do Caixa.
 * O caixa é identificado por um número sequencial (numeroCaixa),
 * independente de data ou turno.
 */
public class CaixaService {

    private final CaixaRepository repo;

    public CaixaService(EntityManagerFactory emf) {
        this.repo = new CaixaRepository(emf);
    }

    // ── Estado do caixa ───────────────────────────────────────────────────────

    public static class EstadoCaixa {
        public boolean aberto;
        public boolean algumFechamento;
        public Long    numeroCaixa;          // número do caixa atual (aberto ou último fechado)
        public BigDecimal saldoAtual      = BigDecimal.ZERO;
        public BigDecimal aberturaAtual   = BigDecimal.ZERO;
        public BigDecimal sangriasAtual   = BigDecimal.ZERO;
        public BigDecimal suprimentoAtual = BigDecimal.ZERO;
        public LocalDateTime inicioTurno;
    }

    public EstadoCaixa calcularEstado() {
        EstadoCaixa estado = new EstadoCaixa();

        // Verifica se há um caixa aberto (sem fechamento)
        Long numAberto = repo.buscarNumeroCaixaAberto();

        if (numAberto != null) {
            // Caixa aberto — carrega seus movimentos
            estado.aberto      = true;
            estado.numeroCaixa = numAberto;
            List<CaixaMovimento> movimentos = repo.buscarMovimentosPorNumero(numAberto);

            for (CaixaMovimento m : movimentos) {
                switch (m.getTipo()) {
                    case ABERTURA -> {
                        estado.aberturaAtual   = m.getValor();
                        estado.sangriasAtual   = BigDecimal.ZERO;
                        estado.suprimentoAtual = BigDecimal.ZERO;
                        estado.inicioTurno     = m.getDataMovimento();
                    }
                    case SANGRIA    -> estado.sangriasAtual   = estado.sangriasAtual.add(m.getValor());
                    case SUPRIMENTO -> estado.suprimentoAtual = estado.suprimentoAtual.add(m.getValor());
                    default -> {}
                }
            }

            if (estado.inicioTurno != null) {
                BigDecimal vendas = repo.buscarTotalVendasPorPeriodo(estado.inicioTurno, LocalDateTime.now());
                estado.saldoAtual = estado.aberturaAtual
                        .add(vendas)
                        .add(estado.suprimentoAtual)
                        .add(estado.sangriasAtual); // sangrias já são negativas
            }
        } else {
            // Sem caixa aberto — verifica se já houve algum fechamento
            List<Long> fechados = repo.buscarNumerosCaixaFechados();
            if (!fechados.isEmpty()) {
                estado.algumFechamento = true;
                estado.numeroCaixa     = fechados.get(0); // último fechado

                // Calcula saldo do último caixa fechado (para exibição no painel)
                List<CaixaMovimento> movsFechado = repo.buscarMovimentosPorNumero(estado.numeroCaixa);
                LocalDateTime horaFech = null;
                for (CaixaMovimento m : movsFechado) {
                    switch (m.getTipo()) {
                        case ABERTURA -> {
                            estado.aberturaAtual   = m.getValor();
                            estado.sangriasAtual   = BigDecimal.ZERO;
                            estado.suprimentoAtual = BigDecimal.ZERO;
                            estado.inicioTurno     = m.getDataMovimento();
                        }
                        case SANGRIA    -> estado.sangriasAtual   = estado.sangriasAtual.add(m.getValor());
                        case SUPRIMENTO -> estado.suprimentoAtual = estado.suprimentoAtual.add(m.getValor());
                        case FECHAMENTO -> horaFech = m.getDataMovimento();
                        default -> {}
                    }
                }
                if (estado.inicioTurno != null) {
                    LocalDateTime fim = horaFech != null ? horaFech : LocalDateTime.now();
                    BigDecimal vendas = repo.buscarTotalVendasPorPeriodo(estado.inicioTurno, fim);
                    estado.saldoAtual = estado.aberturaAtual
                            .add(vendas)
                            .add(estado.suprimentoAtual)
                            .add(estado.sangriasAtual);
                }
            }
        }

        return estado;
    }

    // ── Operações ─────────────────────────────────────────────────────────────

    public void abrirCaixa(BigDecimal valorInicial) {
        if (valorInicial == null || valorInicial.compareTo(BigDecimal.ZERO) < 0)
            throw new RegraNegocioException("Valor inicial não pode ser negativo.");

        EstadoCaixa estado = calcularEstado();
        if (estado.aberto)
            throw new RegraNegocioException("O caixa já está aberto (Caixa #" + estado.numeroCaixa + ").");

        Long novoCaixa = repo.proximoNumeroCaixa();

        CaixaMovimento mov = new CaixaMovimento();
        mov.setNumeroCaixa(novoCaixa);
        mov.setTipo(TipoCaixaMovimento.ABERTURA);
        mov.setValor(valorInicial);
        mov.setDescricao("Abertura do Caixa #" + novoCaixa + " com R$ " + String.format("%.2f", valorInicial));
        repo.registrar(mov);
    }

    public BigDecimal fecharCaixa(BigDecimal valorInformado) {
        if (valorInformado == null || valorInformado.compareTo(BigDecimal.ZERO) < 0)
            throw new RegraNegocioException("Valor informado não pode ser negativo.");

        EstadoCaixa estado = calcularEstado();
        if (!estado.aberto)
            throw new RegraNegocioException("O caixa não está aberto.");

        BigDecimal diferenca = valorInformado.subtract(estado.saldoAtual);
        String statusStr = diferenca.abs().compareTo(new BigDecimal("0.01")) <= 0 ? "CORRETO" : "DIFERENCA";

        CaixaMovimento mov = new CaixaMovimento();
        mov.setNumeroCaixa(estado.numeroCaixa);
        mov.setTipo(TipoCaixaMovimento.FECHAMENTO);
        mov.setValor(estado.saldoAtual);
        mov.setDescricao(String.format("FECHAMENTO|caixa=%d|sistema=%.2f|informado=%.2f|diferenca=%.2f|status=%s",
                estado.numeroCaixa, estado.saldoAtual, valorInformado, diferenca, statusStr));
        repo.registrar(mov);

        return diferenca; // positivo = sobrou, negativo = faltou
    }

    public void registrarSangria(BigDecimal valor, String descricao) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0)
            throw new RegraNegocioException("Valor da sangria deve ser maior que zero.");

        EstadoCaixa estado = calcularEstado();
        if (!estado.aberto)
            throw new RegraNegocioException("O caixa não está aberto.");
        if (valor.compareTo(estado.saldoAtual) > 0)
            throw new RegraNegocioException("Valor da sangria maior que o saldo atual (R$ "
                    + String.format("%.2f", estado.saldoAtual) + ").");

        CaixaMovimento mov = new CaixaMovimento();
        mov.setNumeroCaixa(estado.numeroCaixa);
        mov.setTipo(TipoCaixaMovimento.SANGRIA);
        mov.setValor(valor.negate());
        mov.setDescricao(descricao == null || descricao.isBlank() ? "Sangria" : "Sangria: " + descricao);
        repo.registrar(mov);
    }

    public void registrarSuprimento(BigDecimal valor, String descricao) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0)
            throw new RegraNegocioException("Valor do suprimento deve ser maior que zero.");

        EstadoCaixa estado = calcularEstado();
        if (!estado.aberto)
            throw new RegraNegocioException("O caixa não está aberto.");

        CaixaMovimento mov = new CaixaMovimento();
        mov.setNumeroCaixa(estado.numeroCaixa);
        mov.setTipo(TipoCaixaMovimento.SUPRIMENTO);
        mov.setValor(valor);
        mov.setDescricao(descricao == null || descricao.isBlank() ? "Suprimento" : "Suprimento: " + descricao);
        repo.registrar(mov);
    }

    public void registrarVenda(Pagamento pagamento) {
        EstadoCaixa estado = calcularEstado();
        if (!estado.aberto) return; // venda já foi validada antes

        CaixaMovimento mov = new CaixaMovimento();
        mov.setNumeroCaixa(estado.numeroCaixa);
        mov.setTipo(TipoCaixaMovimento.VENDA);
        mov.setValor(pagamento.getValorFinal() != null ? pagamento.getValorFinal() : pagamento.getValorPago());
        mov.setPagamento(pagamento);
        mov.setDescricao("Venda - Pedido #" + pagamento.getPedido().getId()
                + " | " + pagamento.getFormaPagamento());
        repo.registrar(mov);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public List<CaixaMovimento> buscarMovimentosPorNumero(Long numeroCaixa) {
        return repo.buscarMovimentosPorNumero(numeroCaixa);
    }

    public List<Long> buscarNumerosCaixaFechados() {
        return repo.buscarNumerosCaixaFechados();
    }

    public BigDecimal buscarTotalVendasPorTurno(LocalDateTime inicio, LocalDateTime fim) {
        return repo.buscarTotalVendasPorPeriodo(inicio, fim);
    }

    // ── Compat. legado (RelatorioService) ────────────────────────────────────

    public List<CaixaMovimento> buscarMovimentosDia(LocalDate data) {
        return repo.buscarMovimentosDia(data);
    }

    public List<LocalDate> buscarDatasComFechamento() {
        return repo.buscarDatasComFechamento();
    }
}
