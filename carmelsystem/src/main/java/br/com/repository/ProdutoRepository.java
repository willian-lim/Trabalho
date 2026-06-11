package br.carmel.repository;

import br.carmel.model.Produto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class ProdutoRepository extends BaseRepository {

    public ProdutoRepository(EntityManagerFactory emf) {
        super(emf);
    }

    public List<Produto> buscarTodos() {
        EntityManager em = abrirEm();
        try {
            return em.createQuery("SELECT p FROM Produto p ORDER BY p.nome", Produto.class)
                    .getResultList();
        } finally { fechar(em); }
    }

    public Optional<Produto> buscarPorId(Long id) {
        EntityManager em = abrirEm();
        try {
            return Optional.ofNullable(em.find(Produto.class, id));
        } finally { fechar(em); }
    }

    public Optional<Produto> buscarPorCodBarras(String codBarras) {
        EntityManager em = abrirEm();
        try {
            return em.createQuery(
                            "SELECT p FROM Produto p WHERE p.codBarras = :cod", Produto.class)
                    .setParameter("cod", codBarras)
                    .getResultStream().findFirst();
        } finally { fechar(em); }
    }

    public List<Produto> buscarPorFiltros(String nome, BigDecimal precoMin, BigDecimal precoMax, String codBarras) {
        EntityManager em = abrirEm();
        try {
            StringBuilder jpql = new StringBuilder("SELECT p FROM Produto p WHERE 1=1");
            if (nome != null && !nome.isBlank())
                jpql.append(" AND LOWER(p.nome) LIKE LOWER(:nome)");
            if (precoMin != null)
                jpql.append(" AND p.valor >= :pmin");
            if (precoMax != null)
                jpql.append(" AND p.valor <= :pmax");
            if (codBarras != null && !codBarras.isBlank())
                jpql.append(" AND p.codBarras LIKE :cod");
            jpql.append(" ORDER BY p.nome");
            var q = em.createQuery(jpql.toString(), Produto.class);
            if (nome != null && !nome.isBlank()) q.setParameter("nome", "%" + nome + "%");
            if (precoMin != null) q.setParameter("pmin", precoMin);
            if (precoMax != null) q.setParameter("pmax", precoMax);
            if (codBarras != null && !codBarras.isBlank()) q.setParameter("cod", "%" + codBarras + "%");
            return q.getResultList();
        } finally { fechar(em); }
    }

    public boolean existeNome(String nome, Long ignorarId) {
        EntityManager em = abrirEm();
        try {
            String jpql = ignorarId == null
                    ? "SELECT COUNT(p) FROM Produto p WHERE p.nome = :nome"
                    : "SELECT COUNT(p) FROM Produto p WHERE p.nome = :nome AND p.id != :id";
            var q = em.createQuery(jpql, Long.class).setParameter("nome", nome);
            if (ignorarId != null) q.setParameter("id", ignorarId);
            return q.getSingleResult() > 0;
        } finally { fechar(em); }
    }

    public boolean existeCodBarras(String cod, Long ignorarId) {
        if (cod == null || cod.isBlank()) return false;
        EntityManager em = abrirEm();
        try {
            String jpql = ignorarId == null
                    ? "SELECT COUNT(p) FROM Produto p WHERE p.codBarras = :cod"
                    : "SELECT COUNT(p) FROM Produto p WHERE p.codBarras = :cod AND p.id != :id";
            var q = em.createQuery(jpql, Long.class).setParameter("cod", cod);
            if (ignorarId != null) q.setParameter("id", ignorarId);
            return q.getSingleResult() > 0;
        } finally { fechar(em); }
    }

    public boolean existeNumeroSerie(String serie, Long ignorarId) {
        if (serie == null || serie.isBlank()) return false;
        EntityManager em = abrirEm();
        try {
            String jpql = ignorarId == null
                    ? "SELECT COUNT(p) FROM Produto p WHERE p.numeroSerie = :serie"
                    : "SELECT COUNT(p) FROM Produto p WHERE p.numeroSerie = :serie AND p.id != :id";
            var q = em.createQuery(jpql, Long.class).setParameter("serie", serie);
            if (ignorarId != null) q.setParameter("id", ignorarId);
            return q.getSingleResult() > 0;
        } finally { fechar(em); }
    }

    public Produto salvar(Produto produto) {
        EntityManager em = abrirEm();
        try {
            em.getTransaction().begin();
            Produto result = produto.getId() == null
                    ? em.merge(produto)
                    : em.merge(produto);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            rollback(em); throw new RuntimeException("Erro ao salvar produto: " + e.getMessage(), e);
        } finally { fechar(em); }
    }

    public void excluir(Long id) {
        EntityManager em = abrirEm();
        try {
            em.getTransaction().begin();
            Produto p = em.find(Produto.class, id);
            if (p != null) em.remove(p);
            em.getTransaction().commit();
        } catch (Exception e) {
            rollback(em); throw new RuntimeException("Erro ao excluir produto: " + e.getMessage(), e);
        } finally { fechar(em); }
    }
}