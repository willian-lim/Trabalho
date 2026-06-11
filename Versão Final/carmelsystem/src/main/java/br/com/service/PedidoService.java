package br.carmel.service;

import br.carmel.model.*;
import br.carmel.repository.PedidoRepository;
import br.carmel.repository.ProdutoRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PedidoService {

    private final EntityManagerFactory emf;
    private final PedidoRepository pedidoRepo;
    private final ProdutoRepository produtoRepo;

    public PedidoService(EntityManagerFactory emf) {
        this.emf         = emf;
        this.pedidoRepo  = new PedidoRepository(emf);
        this.produtoRepo = new ProdutoRepository(emf);
    }

    

    public List<Pedido> listarPendentes() {
        return pedidoRepo.buscarPorStatus(StatusPedido.PENDENTE);
    }

    public List<Pedido> buscarComFiltros(String cliente, LocalDateTime de,
                                         LocalDateTime ate, Double valorMin, Double valorMax) {
        return pedidoRepo.buscarComFiltros(cliente, de, ate, valorMin, valorMax);
    }

    public Pedido buscarComItens(Long id) {
        return pedidoRepo.buscarComItens(id)
                .orElseThrow(() -> new RegraNegocioException("Pedido #" + id + " não encontrado."));
    }

    

    public Pedido criarPedidoPendente(Long clienteId, List<ItemPedidoDTO> itens, String observacoes) {
        if (itens == null || itens.isEmpty())
            throw new RegraNegocioException("O pedido deve ter pelo menos um item.");
        if (observacoes != null && observacoes.length() > 500)
            throw new RegraNegocioException("Observações devem ter no máximo 500 caracteres.");

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Cliente cliente = em.find(Cliente.class, clienteId);
            if (cliente == null)
                throw new RegraNegocioException("Cliente não encontrado.");

            Pedido pedido = new Pedido();
            pedido.setCliente(cliente);
            pedido.setObservacoes(observacoes);
            pedido.setDataPedido(LocalDateTime.now());
            pedido.setStatus(StatusPedido.PENDENTE);
            em.persist(pedido);

            for (ItemPedidoDTO dto : itens) {
                Produto produto = em.find(Produto.class, dto.produtoId());
                if (produto == null)
                    throw new RegraNegocioException("Produto ID " + dto.produtoId() + " não encontrado.");

                ItensPedido item = new ItensPedido();
                item.setPedido(pedido);
                item.setProduto(produto);
                item.setQuantidade(dto.quantidade());

                
                BigDecimal preco = (dto.precoUnitario() != null
                        && dto.precoUnitario().compareTo(BigDecimal.ZERO) > 0)
                        ? dto.precoUnitario()
                        : produto.getValor();
                item.setPrecoUnitario(preco);
                item.calcularSubtotal();

                pedido.adicionarItem(item);
                em.persist(item);
            }

            pedido.calcularValorTotal();
            em.merge(pedido);
            em.getTransaction().commit();
            return pedido;

        } catch (RegraNegocioException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Erro ao criar pedido: " + e.getMessage(), e);
        } finally { em.close(); }
    }

    

    public static class ResultadoEmissao {
        public Pagamento pagamento;
        public BigDecimal troco;
        public BigDecimal totalFinal;
    }

    public ResultadoEmissao emitirNota(Long pedidoId, BigDecimal desconto,
                                       FormaPagamento forma, BigDecimal valorPago,
                                       boolean caixaAberto) {
        if (!caixaAberto)
            throw new RegraNegocioException("O caixa está fechado. Abra o caixa antes de emitir a nota.");
        if (desconto == null) desconto = BigDecimal.ZERO;
        if (desconto.compareTo(BigDecimal.ZERO) < 0)
            throw new RegraNegocioException("Desconto não pode ser negativo.");

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Pedido pedido = em.createQuery(
                    "SELECT p FROM Pedido p LEFT JOIN FETCH p.itensPedidos i " +
                            "LEFT JOIN FETCH i.produto LEFT JOIN FETCH p.cliente WHERE p.id = :id",
                    Pedido.class).setParameter("id", pedidoId).getSingleResult();

            if (pedido.getStatus() != StatusPedido.PENDENTE)
                throw new RegraNegocioException("Apenas pedidos PENDENTES podem ser atendidos.");

            BigDecimal subtotal = pedido.getValorTotal();
            if (desconto.compareTo(subtotal) > 0)
                throw new RegraNegocioException("Desconto não pode ser maior que o total do pedido.");

            BigDecimal totalFinal = subtotal.subtract(desconto).max(BigDecimal.ZERO);

            if (valorPago.compareTo(BigDecimal.ZERO) <= 0)
                throw new RegraNegocioException("Valor recebido deve ser maior que zero.");
            if (valorPago.compareTo(totalFinal) < 0)
                throw new RegraNegocioException("Valor recebido (R$ " + String.format("%.2f", valorPago)
                        + ") é menor que o total (R$ " + String.format("%.2f", totalFinal) + ").");

            for (ItensPedido item : pedido.getItensPedidos()) {
                Produto pm = em.find(Produto.class, item.getProduto().getId());
                int estAtual = pm.getEstoque() != null ? pm.getEstoque() : 0;

                if (estAtual < item.getQuantidade())
                    throw new RegraNegocioException("Estoque insuficiente para: " + pm.getNome()
                            + "\nDisponível: " + estAtual + " | Necessário: " + item.getQuantidade());

                pm.setEstoque(estAtual - item.getQuantidade());
                em.merge(pm);
            }

            BigDecimal troco = valorPago.subtract(totalFinal);
            Pagamento pagamento = new Pagamento();
            pagamento.setPedido(pedido);
            pagamento.setFormaPagamento(forma);
            pagamento.setValorPago(valorPago);
            pagamento.setDesconto(desconto);
            pagamento.setValorFinal(totalFinal);
            pagamento.setTroco(troco);
            pagamento.setDataPagamento(LocalDateTime.now());
            em.persist(pagamento);

            pedido.setStatus(StatusPedido.CONFIRMADO);
            em.merge(pedido);
            em.getTransaction().commit();

            ResultadoEmissao resultado = new ResultadoEmissao();
            resultado.pagamento  = pagamento;
            resultado.troco      = troco;
            resultado.totalFinal = totalFinal;
            return resultado;

        } catch (RegraNegocioException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Erro ao emitir nota: " + e.getMessage(), e);
        } finally { em.close(); }
    }

    

    public void cancelarPedido(Long pedidoId) {
        Pedido pedido = pedidoRepo.buscarComItens(pedidoId)
                .orElseThrow(() -> new RegraNegocioException("Pedido não encontrado."));
        if (pedido.getStatus() == StatusPedido.CONFIRMADO)
            throw new RegraNegocioException("Pedidos já confirmados não podem ser cancelados diretamente. Cancele a nota primeiro.");
        pedidoRepo.atualizarStatus(pedidoId, StatusPedido.CANCELADO);
    }

    public void cancelarNota(Long pedidoId) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Pedido pedido = em.createQuery(
                    "SELECT p FROM Pedido p LEFT JOIN FETCH p.itensPedidos i " +
                            "LEFT JOIN FETCH i.produto LEFT JOIN FETCH p.cliente WHERE p.id = :id",
                    Pedido.class).setParameter("id", pedidoId).getSingleResult();

            if (pedido.getStatus() != StatusPedido.CONFIRMADO)
                throw new RegraNegocioException("Apenas notas com status CONFIRMADO podem ser canceladas.");

            for (ItensPedido item : pedido.getItensPedidos()) {
                Produto pm = em.find(Produto.class, item.getProduto().getId());
                int atual = pm.getEstoque() != null ? pm.getEstoque() : 0;
                pm.setEstoque(atual + item.getQuantidade());
                em.merge(pm);
            }

            List<Pagamento> pags = em.createQuery(
                            "SELECT p FROM Pagamento p WHERE p.pedido.id = :pid", Pagamento.class)
                    .setParameter("pid", pedidoId).getResultList();
            for (Pagamento pag : pags)
                em.remove(em.contains(pag) ? pag : em.merge(pag));

            pedido.setStatus(StatusPedido.PENDENTE);
            em.merge(pedido);
            em.getTransaction().commit();

        } catch (RegraNegocioException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Erro ao cancelar nota: " + e.getMessage(), e);
        } finally { em.close(); }
    }

    

    
    public record ItemPedidoDTO(Long produtoId, int quantidade, BigDecimal precoUnitario) {
        
        public ItemPedidoDTO(Long produtoId, int quantidade) {
            this(produtoId, quantidade, null);
        }
    }
}