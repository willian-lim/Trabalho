package br.carmel.repository;

import br.carmel.model.Licenca;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;

import java.util.Optional;

public class LicencaRepository extends BaseRepository {

    public LicencaRepository(EntityManagerFactory emf) {
        super(emf);
    }

    
    public boolean chaveJaUtilizada(String hashChave) {
        EntityManager em = abrirEm();
        try {
            Long count = em.createQuery(
                    "SELECT COUNT(l) FROM Licenca l WHERE l.hashChave = :hash",
                    Long.class
            ).setParameter("hash", hashChave).getSingleResult();
            return count > 0;
        } catch (Exception e) {
            return false;
        } finally {
            fechar(em);
        }
    }

    
    public Optional<Licenca> buscarMaisRecente() {
        EntityManager em = abrirEm();
        try {
            Licenca licenca = em.createQuery(
                    "SELECT l FROM Licenca l ORDER BY l.id DESC",
                    Licenca.class
            ).setMaxResults(1).getSingleResult();
            return Optional.of(licenca);
        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            fechar(em);
        }
    }

    
    public void salvar(Licenca licenca) {
        EntityManager em = abrirEm();
        try {
            em.getTransaction().begin();
            em.persist(licenca);
            em.getTransaction().commit();
        } catch (Exception e) {
            rollback(em);
            throw new RuntimeException("Erro ao salvar licença no banco de dados.", e);
        } finally {
            fechar(em);
        }
    }
}