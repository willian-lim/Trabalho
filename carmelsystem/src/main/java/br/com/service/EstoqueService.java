package br.carmel.service;

import br.carmel.model.*;
import br.carmel.repository.ProdutoRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras de negócio de transferências de estoque (Notas de Entrada/Saída).
 */
public class EstoqueService {

    private final EntityManagerFactory emf;
    private final ProdutoRepository produtoRepo;

    public EstoqueService(EntityManagerFactory emf) {
        this.emf        = emf;
        this.produtoRepo = new ProdutoRepository(emf);
    }

    /**
     * Emite nota de entrada: adiciona estoque e atualiza preço médio.
     */
    public NotaTransferencia emitirEntrada(Long fornecedorId, String numeroNota,
                                           String observacoes, List<ItemNotaDTO> itens) {
        if (itens == null || itens.isEmpty())
            throw new RegraNegocioException("A nota deve ter pelo menos um item.");

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            NotaTransferencia nota = new NotaTransferencia();
            nota.setTipo(TipoNota.ENTRADA);
            nota.setDataNota(LocalDateTime.now());
            nota.setNumeroNota(numeroNota);
            nota.setObservacoes(observacoes);

            if (fornecedorId != null) {
                Fornecedor f = em.find(Fornecedor.class, fornecedorId);
                nota.setFornecedor(f);
            }
            em.persist(nota);

            for (ItemNotaDTO dto : itens) {
                Produto pm = em.find(Produto.class, dto.produtoId());
                if (pm == null)
                    throw new RegraNegocioException("Produto não encontrado: ID " + dto.produtoId());
                if (dto.quantidade() <= 0)
                    throw new RegraNegocioException("Quantidade deve ser maior que zero.");

                // Debita estoque
                int estoqueAtual = pm.getEstoque() != null ? pm.getEstoque() : 0;
                pm.setEstoque(estoqueAtual + dto.quantidade());

                // Atualiza preço médio ponderado
                if (dto.precoUnitario() != null && dto.precoUnitario().compareTo(BigDecimal.ZERO) > 0) {
                    atualizarPrecoMedio(pm, dto.precoUnitario(), dto.quantidade());
                    pm.setPrecoCusto(dto.precoUnitario());
                }
                em.merge(pm);

                ItemNota item = new ItemNota();
                item.setNota(nota);
                item.setProduto(pm);
                item.setQuantidade(dto.quantidade());
                item.setPrecoUnitario(dto.precoUnitario());
                item.calcularSubtotal();
                nota.getItens().add(item);
                em.persist(item);
            }

            nota.calcularTotal();
            em.merge(nota);
            em.getTransaction().commit();
            return nota;

        } catch (RegraNegocioException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Erro ao emitir nota de entrada: " + e.getMessage(), e);
        } finally { em.close(); }
    }

    /**
     * Emite nota de saída: retira estoque com validação.
     */
    public NotaTransferencia emitirSaida(Long fornecedorId, String numeroNota,
                                         String observacoes, List<ItemNotaDTO> itens) {
        if (itens == null || itens.isEmpty())
            throw new RegraNegocioException("A nota deve ter pelo menos um item.");

        // Valida estoque antes de persistir
        for (ItemNotaDTO dto : itens) {
            produtoRepo.buscarPorId(dto.produtoId()).ifPresent(pm -> {
                int est = pm.getEstoque() != null ? pm.getEstoque() : 0;
                if (est < dto.quantidade())
                    throw new RegraNegocioException("Estoque insuficiente para: " + pm.getNome()
                            + "\nDisponível: " + est + " | Necessário: " + dto.quantidade());
            });
        }

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            NotaTransferencia nota = new NotaTransferencia();
            nota.setTipo(TipoNota.SAIDA);
            nota.setDataNota(LocalDateTime.now());
            nota.setNumeroNota(numeroNota);
            nota.setObservacoes(observacoes);

            if (fornecedorId != null) {
                Fornecedor f = em.find(Fornecedor.class, fornecedorId);
                nota.setFornecedor(f);
            }
            em.persist(nota);

            for (ItemNotaDTO dto : itens) {
                Produto pm = em.find(Produto.class, dto.produtoId());
                if (pm == null)
                    throw new RegraNegocioException("Produto não encontrado.");

                int estAtual = pm.getEstoque() != null ? pm.getEstoque() : 0;
                if (estAtual < dto.quantidade())
                    throw new RegraNegocioException("Estoque insuficiente para: " + pm.getNome());

                pm.setEstoque(estAtual - dto.quantidade());
                em.merge(pm);

                ItemNota item = new ItemNota();
                item.setNota(nota);
                item.setProduto(pm);
                item.setQuantidade(dto.quantidade());
                item.setPrecoUnitario(dto.precoUnitario());
                item.calcularSubtotal();
                nota.getItens().add(item);
                em.persist(item);
            }

            nota.calcularTotal();
            em.merge(nota);
            em.getTransaction().commit();
            return nota;

        } catch (RegraNegocioException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException("Erro ao emitir nota de saída: " + e.getMessage(), e);
        } finally { em.close(); }
    }

    private void atualizarPrecoMedio(Produto pm, BigDecimal novoCusto, int qtdEntrada) {
        BigDecimal medioAnt = pm.getPrecoMedio();
        BigDecimal qtdHist  = pm.getQtdHistoricoCusto();
        BigDecimal peso     = BigDecimal.valueOf(qtdEntrada);

        if (medioAnt == null || qtdHist == null || qtdHist.compareTo(BigDecimal.ZERO) == 0) {
            pm.setPrecoMedio(novoCusto);
            pm.setQtdHistoricoCusto(peso);
        } else {
            BigDecimal novaQtd   = qtdHist.add(peso);
            BigDecimal novoMedio = qtdHist.multiply(medioAnt)
                    .add(peso.multiply(novoCusto))
                    .divide(novaQtd, 2, RoundingMode.HALF_UP);
            pm.setPrecoMedio(novoMedio);
            pm.setQtdHistoricoCusto(novaQtd);
        }
    }



    /**
     * Lista notas com filtros opcionais de tipo e período.
     * Passa null para ignorar o filtro correspondente.
     */
    public List<NotaTransferencia> listarNotas(TipoNota tipo, LocalDateTime de, LocalDateTime ate) {
        EntityManager em = emf.createEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT DISTINCT n FROM NotaTransferencia n LEFT JOIN FETCH n.itens LEFT JOIN FETCH n.fornecedor WHERE 1=1");
            if (tipo != null)  jpql.append(" AND n.tipo = :tipo");
            if (de   != null)  jpql.append(" AND n.dataNota >= :de");
            if (ate  != null)  jpql.append(" AND n.dataNota <= :ate");
            jpql.append(" ORDER BY n.dataNota DESC");

            var q = em.createQuery(jpql.toString(), NotaTransferencia.class);
            if (tipo != null)  q.setParameter("tipo", tipo);
            if (de   != null)  q.setParameter("de",   de);
            if (ate  != null)  q.setParameter("ate",  ate);
            return q.getResultList();
        } finally { em.close(); }
    }

    public record ItemNotaDTO(Long produtoId, int quantidade, BigDecimal precoUnitario) {}
}
