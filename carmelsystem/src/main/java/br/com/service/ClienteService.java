package br.carmel.service;

import br.carmel.model.Cliente;
import br.carmel.model.ClienteJuridico;
import br.carmel.model.Endereco;
import br.carmel.model.Fornecedor;
import br.carmel.repository.ClienteRepository;
import br.carmel.util.Validator;

import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Optional;

public class ClienteService {

    private final ClienteRepository repo;

    public ClienteService(EntityManagerFactory emf) {
        this.repo = new ClienteRepository(emf);
    }

    

    public List<Cliente> listarPF() { return repo.buscarTodosPF(); }

    
    public List<Cliente> buscarPF(String termo) { return repo.buscarPFPorTermo(termo); }

    public Optional<Cliente> buscarPFPorId(Long id) { return repo.buscarPFPorId(id); }

    public Cliente salvarPF(Cliente cliente) {
        validarPF(cliente);
        cliente.setCpf(Validator.formatarCpf(cliente.getCpf()));
        cliente.setTelefone(Validator.formatarTelefone(cliente.getTelefone()));
        if (cliente.getEnderecos() != null)
            cliente.getEnderecos().forEach(e -> { if (e.getCep() != null) e.setCep(Validator.formatarCep(e.getCep())); });
        return repo.salvarPF(cliente);
    }

    public void excluirPF(Long id) {
        repo.buscarPFPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrado."));
        repo.excluirPF(id);
    }

    private void validarPF(Cliente c) {
        if (Validator.isBlank(c.getNome()))
            throw new RegraNegocioException("Nome é obrigatório.");
        if (Validator.isBlank(c.getCpf()))
            throw new RegraNegocioException("CPF é obrigatório.");
        if (!Validator.isCpfValido(c.getCpf()))
            throw new RegraNegocioException("CPF inválido.");
        if (Validator.isBlank(c.getTelefone()))
            throw new RegraNegocioException("Telefone é obrigatório.");
        if (repo.existeCpf(c.getCpf(), c.getId()))
            throw new RegraNegocioException("Já existe um cliente com o CPF '" + c.getCpf() + "'.");
    }

    

    public List<ClienteJuridico> listarPJ() { return repo.buscarTodosPJ(); }

    
    public List<ClienteJuridico> buscarPJ(String termo) { return repo.buscarPJPorTermo(termo); }

    public Optional<ClienteJuridico> buscarPJPorId(Long id) { return repo.buscarPJPorId(id); }

    public ClienteJuridico salvarPJ(ClienteJuridico c) {
        validarPJ(c);
        return repo.salvarPJ(c);
    }

    public void excluirPJ(Long id) {
        repo.buscarPJPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Cliente PJ não encontrado."));
        repo.excluirPJ(id);
    }

    private void validarPJ(ClienteJuridico c) {
        if (Validator.isBlank(c.getRazaoSocial()))
            throw new RegraNegocioException("Razão Social é obrigatória.");
        if (c.getRazaoSocial().length() > 150)
            throw new RegraNegocioException("Razão Social deve ter no máximo 150 caracteres.");
    }

    

    public List<Fornecedor> listarFornecedores() { return repo.buscarTodosFornecedores(); }

    
    public List<Fornecedor> buscarFornecedores(String termo) { return repo.buscarFornecedorPorTermo(termo); }

    public Optional<Fornecedor> buscarFornecedorPorId(Long id) { return repo.buscarFornecedorPorId(id); }

    public Fornecedor salvarFornecedor(Fornecedor f) {
        validarFornecedor(f);
        return repo.salvarFornecedor(f);
    }

    public void excluirFornecedor(Long id) {
        repo.buscarFornecedorPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Fornecedor não encontrado."));
        repo.excluirFornecedor(id);
    }

    private void validarFornecedor(Fornecedor f) {
        if (Validator.isBlank(f.getRazaoSocial()))
            throw new RegraNegocioException("Razão Social é obrigatória.");
        if (f.getRazaoSocial().length() > 150)
            throw new RegraNegocioException("Razão Social deve ter no máximo 150 caracteres.");
    }
}
