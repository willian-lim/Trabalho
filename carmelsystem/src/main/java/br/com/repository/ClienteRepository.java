package br.carmel.repository;

import br.carmel.model.Cliente;
import br.carmel.model.ClienteJuridico;
import br.carmel.model.Fornecedor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Optional;

public class ClienteRepository extends BaseRepository {

    public ClienteRepository(EntityManagerFactory emf) { super(emf); }

    

    public List<Cliente> buscarTodosPF() {
        EntityManager em = abrirEm();
        try {
            return em.createQuery(
                    "SELECT DISTINCT c FROM Cliente c LEFT JOIN FETCH c.enderecos ORDER BY c.nome",
                    Cliente.class).getResultList();
        } finally { fechar(em); }
    }

    
    public List<Cliente> buscarPFPorTermo(String termo) {
        EntityManager em = abrirEm();
        try {
            if (termo == null || termo.isBlank()) return buscarTodosPF();
            String t    = "%" + termo.trim().toLowerCase() + "%";
            String tcpf = "%" + termo.trim().replaceAll("[^0-9]", "") + "%";
            return em.createQuery(
                    "SELECT DISTINCT c FROM Cliente c LEFT JOIN FETCH c.enderecos " +
                    "WHERE LOWER(c.nome) LIKE :t " +
                    "OR REPLACE(REPLACE(REPLACE(c.cpf,'.',''),'-',''),' ','') LIKE :tcpf " +
                    "ORDER BY c.nome",
                    Cliente.class)
                    .setParameter("t",    t)
                    .setParameter("tcpf", tcpf)
                    .getResultList();
        } finally { fechar(em); }
    }

    public Optional<Cliente> buscarPFPorId(Long id) {
        EntityManager em = abrirEm();
        try {
            List<Cliente> result = em.createQuery(
                    "SELECT c FROM Cliente c LEFT JOIN FETCH c.enderecos WHERE c.id = :id",
                    Cliente.class).setParameter("id", id).getResultList();
            return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
        } finally { fechar(em); }
    }

    public boolean existeCpf(String cpf, Long ignorarId) {
        EntityManager em = abrirEm();
        try {
            String jpql = ignorarId == null
                    ? "SELECT COUNT(c) FROM Cliente c WHERE c.cpf = :cpf"
                    : "SELECT COUNT(c) FROM Cliente c WHERE c.cpf = :cpf AND c.id != :id";
            var q = em.createQuery(jpql, Long.class).setParameter("cpf", cpf);
            if (ignorarId != null) q.setParameter("id", ignorarId);
            return q.getSingleResult() > 0;
        } finally { fechar(em); }
    }

    public Cliente salvarPF(Cliente cliente) {
        EntityManager em = abrirEm();
        try {
            em.getTransaction().begin();
            
            if (cliente.getEnderecos() != null)
                cliente.getEnderecos().forEach(e -> { if (e.getCliente() == null) e.setCliente(cliente); });
            Cliente result = em.merge(cliente);
            em.flush(); 
            em.getTransaction().commit();
            return result;
        } catch (Exception e) { rollback(em); throw new RuntimeException(e.getMessage(), e); }
        finally { fechar(em); }
    }

    public void excluirPF(Long id) {
        EntityManager em = abrirEm();
        try {
            em.getTransaction().begin();
            Cliente c = em.find(Cliente.class, id);
            if (c != null) em.remove(c);
            em.getTransaction().commit();
        } catch (Exception e) { rollback(em); throw new RuntimeException(e.getMessage(), e); }
        finally { fechar(em); }
    }

    

    public List<ClienteJuridico> buscarTodosPJ() {
        EntityManager em = abrirEm();
        try {
            return em.createQuery("SELECT c FROM ClienteJuridico c ORDER BY c.razaoSocial", ClienteJuridico.class).getResultList();
        } finally { fechar(em); }
    }

    
    public List<ClienteJuridico> buscarPJPorTermo(String termo) {
        EntityManager em = abrirEm();
        try {
            if (termo == null || termo.isBlank()) return buscarTodosPJ();
            String t    = "%" + termo.trim().toLowerCase() + "%";
            String tcnpj = "%" + termo.trim().replaceAll("[^0-9]", "") + "%";
            return em.createQuery(
                    "SELECT c FROM ClienteJuridico c WHERE LOWER(c.razaoSocial) LIKE :t " +
                    "OR LOWER(c.nomeFantasia) LIKE :t " +
                    "OR REPLACE(REPLACE(REPLACE(REPLACE(c.cnpj,'.','-'),'/',''),'-',''),' ','') LIKE :tcnpj " +
                    "ORDER BY c.razaoSocial",
                    ClienteJuridico.class)
                    .setParameter("t",     t)
                    .setParameter("tcnpj", tcnpj)
                    .getResultList();
        } finally { fechar(em); }
    }

    public Optional<ClienteJuridico> buscarPJPorId(Long id) {
        EntityManager em = abrirEm();
        try { return Optional.ofNullable(em.find(ClienteJuridico.class, id)); }
        finally { fechar(em); }
    }

    public ClienteJuridico salvarPJ(ClienteJuridico c) {
        EntityManager em = abrirEm();
        try {
            em.getTransaction().begin();
            ClienteJuridico result = em.merge(c);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) { rollback(em); throw new RuntimeException(e.getMessage(), e); }
        finally { fechar(em); }
    }

    public void excluirPJ(Long id) {
        EntityManager em = abrirEm();
        try {
            em.getTransaction().begin();
            ClienteJuridico c = em.find(ClienteJuridico.class, id);
            if (c != null) em.remove(c);
            em.getTransaction().commit();
        } catch (Exception e) { rollback(em); throw new RuntimeException(e.getMessage(), e); }
        finally { fechar(em); }
    }

    

    public List<Fornecedor> buscarTodosFornecedores() {
        EntityManager em = abrirEm();
        try {
            return em.createQuery("SELECT f FROM Fornecedor f ORDER BY f.razaoSocial", Fornecedor.class).getResultList();
        } finally { fechar(em); }
    }

    
    public List<Fornecedor> buscarFornecedorPorTermo(String termo) {
        EntityManager em = abrirEm();
        try {
            if (termo == null || termo.isBlank()) return buscarTodosFornecedores();
            String t     = "%" + termo.trim().toLowerCase() + "%";
            String tcnpj = "%" + termo.trim().replaceAll("[^0-9]", "") + "%";
            return em.createQuery(
                    "SELECT f FROM Fornecedor f WHERE LOWER(f.razaoSocial) LIKE :t " +
                    "OR LOWER(f.nomeFantasia) LIKE :t " +
                    "OR REPLACE(REPLACE(REPLACE(REPLACE(f.cnpj,'.','-'),'/',''),'-',''),' ','') LIKE :tcnpj " +
                    "ORDER BY f.razaoSocial",
                    Fornecedor.class)
                    .setParameter("t",     t)
                    .setParameter("tcnpj", tcnpj)
                    .getResultList();
        } finally { fechar(em); }
    }

    public Optional<Fornecedor> buscarFornecedorPorId(Long id) {
        EntityManager em = abrirEm();
        try { return Optional.ofNullable(em.find(Fornecedor.class, id)); }
        finally { fechar(em); }
    }

    public Fornecedor salvarFornecedor(Fornecedor f) {
        EntityManager em = abrirEm();
        try {
            em.getTransaction().begin();
            Fornecedor result = em.merge(f);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) { rollback(em); throw new RuntimeException(e.getMessage(), e); }
        finally { fechar(em); }
    }

    public void excluirFornecedor(Long id) {
        EntityManager em = abrirEm();
        try {
            em.getTransaction().begin();
            Fornecedor f = em.find(Fornecedor.class, id);
            if (f != null) em.remove(f);
            em.getTransaction().commit();
        } catch (Exception e) { rollback(em); throw new RuntimeException(e.getMessage(), e); }
        finally { fechar(em); }
    }
}
