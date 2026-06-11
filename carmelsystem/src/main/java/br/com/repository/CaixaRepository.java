package br.carmel.repository;

import br.carmel.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CaixaRepository extends BaseRepository {

    public CaixaRepository(EntityManagerFactory emf) { super(emf); }

    

    
    public List<CaixaMovimento> buscarMovimentosPorNumero(Long numeroCaixa) {
        EntityManager em = abrirEm();
        try {
            return em.createQuery(
                    "SELECT m FROM CaixaMovimento m WHERE m.numeroCaixa = :n ORDER BY m.dataMovimento ASC",
                    CaixaMovimento.class).setParameter("n", numeroCaixa).getResultList();
        } finally { fechar(em); }
    }

    
    public Long proximoNumeroCaixa() {
        EntityManager em = abrirEm();
        try {
            Long max = em.createQuery(
                    "SELECT MAX(m.numeroCaixa) FROM CaixaMovimento m", Long.class).getSingleResult();
            return (max == null ? 0L : max) + 1L;
        } finally { fechar(em); }
    }

    
    public Long buscarNumeroCaixaAberto() {
        EntityManager em = abrirEm();
        try {
            
            List<Long> abertos = em.createQuery(
                    "SELECT DISTINCT m.numeroCaixa FROM CaixaMovimento m " +
                    "WHERE m.tipo = :ab " +
                    "AND m.numeroCaixa NOT IN (" +
                    "  SELECT m2.numeroCaixa FROM CaixaMovimento m2 WHERE m2.tipo = :fech" +
                    ") ORDER BY m.numeroCaixa DESC",
                    Long.class)
                    .setParameter("ab",   TipoCaixaMovimento.ABERTURA)
                    .setParameter("fech", TipoCaixaMovimento.FECHAMENTO)
                    .getResultList();
            return abertos.isEmpty() ? null : abertos.get(0);
        } finally { fechar(em); }
    }

    
    public List<Long> buscarNumerosCaixaFechados() {
        EntityManager em = abrirEm();
        try {
            return em.createQuery(
                    "SELECT DISTINCT m.numeroCaixa FROM CaixaMovimento m " +
                    "WHERE m.tipo = :tipo ORDER BY m.numeroCaixa DESC",
                    Long.class).setParameter("tipo", TipoCaixaMovimento.FECHAMENTO).getResultList();
        } finally { fechar(em); }
    }

    

    public BigDecimal buscarTotalVendasPorPeriodo(java.time.LocalDateTime inicio, java.time.LocalDateTime fim) {
        EntityManager em = abrirEm();
        try {
            BigDecimal result = em.createQuery(
                            "SELECT COALESCE(SUM(p.valorFinal), 0) FROM Pagamento p " +
                                    "WHERE p.dataPagamento >= :ini AND p.dataPagamento <= :fim",
                            BigDecimal.class)
                    .setParameter("ini", inicio).setParameter("fim", fim)
                    .getSingleResult();
            return result != null ? result : BigDecimal.ZERO;
        } finally { fechar(em); }
    }

    

    public List<CaixaMovimento> buscarMovimentosDia(LocalDate data) {
        EntityManager em = abrirEm();
        try {
            return em.createQuery(
                    "SELECT m FROM CaixaMovimento m WHERE m.dataCaixa = :d ORDER BY m.dataMovimento ASC",
                    CaixaMovimento.class).setParameter("d", data).getResultList();
        } finally { fechar(em); }
    }

    public List<LocalDate> buscarDatasComFechamento() {
        EntityManager em = abrirEm();
        try {
            return em.createQuery(
                    "SELECT DISTINCT m.dataCaixa FROM CaixaMovimento m WHERE m.tipo = :tipo ORDER BY m.dataCaixa DESC",
                    LocalDate.class).setParameter("tipo", TipoCaixaMovimento.FECHAMENTO).getResultList();
        } finally { fechar(em); }
    }

    

    public CaixaMovimento registrar(CaixaMovimento movimento) {
        EntityManager em = abrirEm();
        try {
            em.getTransaction().begin();
            CaixaMovimento result = em.merge(movimento);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) { rollback(em); throw new RuntimeException(e.getMessage(), e); }
        finally { fechar(em); }
    }
}
