package br.carmel.service;

import br.carmel.model.Pagamento;
import br.carmel.model.Produto;
import br.carmel.model.StatusPedido;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras de negócio para geração de relatórios.
 */
public class RelatorioService {

    private final EntityManagerFactory emf;

    public RelatorioService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    // ── Relatório de Vendas ───────────────────────────────────────────────────

    /**
     * Busca pagamentos (notas emitidas) no período.
     * Usa valorFinal (com desconto) — nunca o valor bruto.
     */
    public List<Pagamento> buscarVendasPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null)
            throw new RegraNegocioException("Período obrigatório para o relatório de vendas.");
        if (inicio.isAfter(fim))
            throw new RegraNegocioException("A data inicial não pode ser posterior à data final.");

        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT p FROM Pagamento p " +
                                    "LEFT JOIN FETCH p.pedido ped " +
                                    "LEFT JOIN FETCH ped.cliente " +
                                    "WHERE p.dataPagamento >= :ini AND p.dataPagamento <= :fim " +
                                    "ORDER BY p.dataPagamento DESC",
                            Pagamento.class)
                    .setParameter("ini", inicio)
                    .setParameter("fim", fim)
                    .getResultList();
        } finally { em.close(); }
    }

    /**
     * Calcula o total real das vendas (usando valorFinal com desconto).
     */
    public BigDecimal calcularTotalVendas(List<Pagamento> pagamentos) {
        return pagamentos.stream()
                .map(p -> p.getValorFinal() != null ? p.getValorFinal() : p.getValorPago())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula o total de descontos concedidos no período.
     */
    public BigDecimal calcularTotalDescontos(List<Pagamento> pagamentos) {
        return pagamentos.stream()
                .map(p -> p.getDesconto() != null ? p.getDesconto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ── Relatório de Estoque ──────────────────────────────────────────────────

    public List<Produto> buscarTodosProdutosParaEstoque() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT p FROM Produto p ORDER BY p.nome", Produto.class)
                    .getResultList();
        } finally { em.close(); }
    }

    public List<Produto> buscarProdutosEstoqueBaixo(int limite) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT p FROM Produto p WHERE p.estoque <= :lim ORDER BY p.estoque ASC",
                            Produto.class)
                    .setParameter("lim", limite)
                    .getResultList();
        } finally { em.close(); }
    }

    // ── Dashboard / Home ──────────────────────────────────────────────────────

    public record ProdutoRanking(String nome, Long quantidade, BigDecimal total) {}
    public record ClienteRanking(String nome, Long pedidos, BigDecimal total) {}

    /**
     * Top produtos mais vendidos no mês — apenas pedidos CONFIRMADOS.
     */
    public List<ProdutoRanking> topProdutosMes(LocalDateTime inicio, LocalDateTime fim) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT new br.carmel.service.RelatorioService$ProdutoRanking(" +
                                    "  i.produto.nome, SUM(i.quantidade), SUM(i.subtotal)) " +
                                    "FROM ItensPedido i " +
                                    "WHERE i.pedido.status = :status " +
                                    "AND i.pedido.dataPedido >= :ini AND i.pedido.dataPedido < :fim " +
                                    "GROUP BY i.produto.nome " +
                                    "ORDER BY SUM(i.quantidade) DESC",
                            ProdutoRanking.class)
                    .setParameter("status", StatusPedido.CONFIRMADO)
                    .setParameter("ini", inicio)
                    .setParameter("fim", fim)
                    .setMaxResults(10)
                    .getResultList();
        } finally { em.close(); }
    }

    /**
     * Top clientes que mais compraram no mês — usando valorFinal (com desconto).
     */
    public List<ClienteRanking> topClientesMes(LocalDateTime inicio, LocalDateTime fim) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT new br.carmel.service.RelatorioService$ClienteRanking(" +
                                    "  p.cliente.nome, COUNT(p), SUM(COALESCE(pag.valorFinal, p.valorTotal))) " +
                                    "FROM Pedido p LEFT JOIN Pagamento pag ON pag.pedido = p " +
                                    "WHERE p.status = :status " +
                                    "AND p.dataPedido >= :ini AND p.dataPedido < :fim " +
                                    "GROUP BY p.cliente.nome " +
                                    "ORDER BY SUM(COALESCE(pag.valorFinal, p.valorTotal)) DESC",
                            ClienteRanking.class)
                    .setParameter("status", StatusPedido.CONFIRMADO)
                    .setParameter("ini", inicio)
                    .setParameter("fim", fim)
                    .setMaxResults(10)
                    .getResultList();
        } finally { em.close(); }
    }
}