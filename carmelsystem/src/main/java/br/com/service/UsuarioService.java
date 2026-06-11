package br.carmel.service;

import br.carmel.model.Usuario;
import br.carmel.util.SenhaUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Optional;

public class UsuarioService {

    private final EntityManagerFactory emf;

    public UsuarioService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    
    public Usuario autenticar(String login, String senha) {
        if (login == null || login.isBlank())
            throw new RegraNegocioException("Login é obrigatório.");
        if (senha == null || senha.isBlank())
            throw new RegraNegocioException("Senha é obrigatória.");

        EntityManager em = emf.createEntityManager();
        try {
            String hashSenha = SenhaUtil.hash(senha);
            return em.createQuery(
                            "SELECT u FROM Usuario u WHERE u.login = :login AND u.senha = :senha",
                            Usuario.class)
                    .setParameter("login", login)
                    .setParameter("senha", hashSenha)
                    .getResultStream().findFirst()
                    .orElseThrow(() -> new RegraNegocioException("Login ou senha incorretos."));
        } finally { em.close(); }
    }

    
    public void cadastrar(String senhaAdmin, String nomeCompleto,
                          String login, String senha, String confirmacao) {
        if (!SenhaUtil.isAdmin(senhaAdmin))
            throw new RegraNegocioException("Senha de administrador incorreta.");
        if (nomeCompleto == null || nomeCompleto.isBlank())
            throw new RegraNegocioException("Nome completo é obrigatório.");
        if (login == null || login.isBlank())
            throw new RegraNegocioException("Login é obrigatório.");
        if (senha == null || senha.length() < 4)
            throw new RegraNegocioException("Senha deve ter no mínimo 4 caracteres.");
        if (!senha.equals(confirmacao))
            throw new RegraNegocioException("As senhas não conferem.");

        EntityManager em = emf.createEntityManager();
        try {
            Long existe = em.createQuery(
                            "SELECT COUNT(u) FROM Usuario u WHERE u.login = :login", Long.class)
                    .setParameter("login", login).getSingleResult();
            if (existe > 0)
                throw new RegraNegocioException("Já existe um usuário com o login '" + login + "'.");

            em.getTransaction().begin();
            Usuario u = new Usuario();
            u.setLogin(login);
            u.setSenha(SenhaUtil.hash(senha));
            u.setNomeCompleto(nomeCompleto);
            em.persist(u);
            em.getTransaction().commit();
        } catch (RegraNegocioException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Erro ao cadastrar usuário: " + e.getMessage(), e);
        } finally { em.close(); }
    }

    
    public void alterarSenha(String login, String senhaAtual, String novaSenha, String confirmacao) {
        if (novaSenha == null || novaSenha.length() < 4)
            throw new RegraNegocioException("Nova senha deve ter no mínimo 4 caracteres.");
        if (!novaSenha.equals(confirmacao))
            throw new RegraNegocioException("As novas senhas não conferem.");

        
        autenticar(login, senhaAtual);

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Usuario u = em.createQuery(
                            "SELECT u FROM Usuario u WHERE u.login = :login", Usuario.class)
                    .setParameter("login", login).getSingleResult();
            u.setSenha(SenhaUtil.hash(novaSenha));
            em.merge(u);
            em.getTransaction().commit();
        } catch (RegraNegocioException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Erro ao alterar senha: " + e.getMessage(), e);
        } finally { em.close(); }
    }

    
    public void excluir(Long usuarioId, String senhaAdmin) {
        if (!SenhaUtil.isAdmin(senhaAdmin))
            throw new RegraNegocioException("Senha de administrador incorreta.");

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Usuario u = em.find(Usuario.class, usuarioId);
            if (u == null) throw new RegraNegocioException("Usuário não encontrado.");
            em.remove(u);
            em.getTransaction().commit();
        } catch (RegraNegocioException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Erro ao excluir usuário: " + e.getMessage(), e);
        } finally { em.close(); }
    }

    public List<Usuario> listarTodos() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT u FROM Usuario u ORDER BY u.nomeCompleto", Usuario.class)
                    .getResultList();
        } finally { em.close(); }
    }

    public boolean bancoBancoVazio() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT COUNT(u) FROM Usuario u", Long.class)
                    .getSingleResult() == 0;
        } finally { em.close(); }
    }

    public void criarUsuarioPadrao() {
        if (!bancoBancoVazio()) return;
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Usuario u = new Usuario();
            u.setLogin("admin");
            
            u.setSenha("8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918");
            u.setNomeCompleto("Administrador");
            em.persist(u);
            em.getTransaction().commit();
            System.out.println("[Sistema] Usuário padrão criado: admin / admin");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } finally { em.close(); }
    }
}