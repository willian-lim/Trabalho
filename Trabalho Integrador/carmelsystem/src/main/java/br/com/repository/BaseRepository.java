package br.carmel.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Base para todos os repositórios.
 * Gerencia o ciclo de vida do EntityManager.
 */
public abstract class BaseRepository {

    protected final EntityManagerFactory emf;

    protected BaseRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    protected EntityManager abrirEm() {
        return emf.createEntityManager();
    }

    protected void fechar(EntityManager em) {
        if (em != null && em.isOpen()) em.close();
    }

    protected void rollback(EntityManager em) {
        if (em != null && em.getTransaction().isActive())
            em.getTransaction().rollback();
    }
}