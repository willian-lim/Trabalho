package br.carmel.service;

import jakarta.persistence.EntityManagerFactory;

public class ServiceLocator {

    private final ProdutoService   produtoService;
    private final PedidoService    pedidoService;
    private final CaixaService     caixaService;
    private final EstoqueService   estoqueService;
    private final RelatorioService relatorioService;
    private final UsuarioService   usuarioService;
    private final ClienteService   clienteService;

    public ServiceLocator(EntityManagerFactory emf) {
        this.produtoService   = new ProdutoService(emf);
        this.pedidoService    = new PedidoService(emf);
        this.caixaService     = new CaixaService(emf);
        this.estoqueService   = new EstoqueService(emf);
        this.relatorioService = new RelatorioService(emf);
        this.usuarioService   = new UsuarioService(emf);
        this.clienteService   = new ClienteService(emf);
    }

    public ProdutoService   produtos()   { return produtoService; }
    public PedidoService    pedidos()    { return pedidoService; }
    public CaixaService     caixa()      { return caixaService; }
    public EstoqueService   estoque()    { return estoqueService; }
    public RelatorioService relatorios() { return relatorioService; }
    public UsuarioService   usuarios()   { return usuarioService; }
    public ClienteService   clientes()   { return clienteService; }
}