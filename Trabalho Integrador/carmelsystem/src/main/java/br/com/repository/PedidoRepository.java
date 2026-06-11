package br.carmel.repository;

import br.carmel.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class PedidoRepository extends BaseRepository {

    public PedidoRepository(EntityManagerFactory emf) { super(emf); }

    public List<Pedido> buscarPorStatus(StatusPedido status) {
        EntityManager em = abrirEm();
        try {
            return em.createQuery(
                            "SELECT DISTINCT p FROM Pedido p " +
                                    "LEFT JOIN FETCH p.cliente " +
                                    "LEFT JOIN FETCH p.itensPedidos i " +
                                    "LEFT JOIN FETCH i.produto " +
                                    "WHERE p.status = :s ORDER BY p.dataPedido ASC", Pedido.class)
                    .setParameter("s", status).getResultList();
        } finally { fechar(em); }
    }

    public Optional<Pedido> buscarComItens(Long id) {
        EntityManager em = abrirEm();
        try {
            return em.createQuery(
                            "SELECT p FROM Pedido p " +
                                    "LEFT JOIN FETCH p.itensPedidos i " +
                                    "LEFT JOIN FETCH i.produto " +
                                    "LEFT JOIN FETCH p.cliente " +
                                    "WHERE p.id = :id", Pedido.class)
                    .setParameter("id", id)
                    .getResultStream().findFirst();
        } finally { fechar(em); }
    }

    public List<Pedido> buscarComFiltros(String cliente, LocalDateTime de,
                                         LocalDateTime ate, Double valorMin, Double valorMax) {
        EntityManager em = abrirEm();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT p FROM Pedido p LEFT JOIN FETCH p.cliente c WHERE 1=1");
            if (cliente != null && !cliente.isBlank())
                jpql.append(" AND LOWER(c.nome) LIKE LOWER(:cli)");
            if (de != null)  jpql.append(" AND p.dataPedido >= :de");
            if (ate != null) jpql.append(" AND p.dataPedido <= :ate");
            if (valorMin != null) jpql.append(" AND p.valorTotal >= :vmin");
            if (valorMax != null) jpql.append(" AND p.valorTotal <= :vmax");
            jpql.append(" ORDER BY p.dataPedido DESC");

            var q = em.createQuery(jpql.toString(), Pedido.class);
            if (cliente != null && !cliente.isBlank()) q.setParameter("cli", "%" + cliente + "%");
            if (de != null)  q.setParameter("de", de);
            if (ate != null) q.setParameter("ate", ate);
            if (valorMin != null) q.setParameter("vmin", valorMin);
            if (valorMax != null) q.setParameter("vmax", valorMax);
            return q.getResultList();
        } finally { fechar(em); }
    }

    public Pedido salvar(Pedido pedido) {
        EntityManager em = abrirEm();
        try {
            em.getTransaction().begin();
            Pedido result = em.merge(pedido);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) { rollback(em); throw new RuntimeException(e.getMessage(), e); }
        finally { fechar(em); }
    }

    public void atualizarStatus(Long pedidoId, StatusPedido novoStatus) {
        EntityManager em = abrirEm();
        try {
            em.getTransaction().begin();
            Pedido p = em.find(Pedido.class, pedidoId);
            if (p != null) { p.setStatus(novoStatus); em.merge(p); }
            em.getTransaction().commit();
        } catch (Exception e) { rollback(em); throw new RuntimeException(e.getMessage(), e); }
        finally { fechar(em); }
    }
}