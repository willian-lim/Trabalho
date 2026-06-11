package br.carmel.service;

import br.carmel.model.Produto;
import br.carmel.repository.ProdutoRepository;
import br.carmel.util.SenhaUtil;
import br.carmel.util.Validator;

import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Regras de negócio relacionadas a Produtos.
 */
public class ProdutoService {

    private final ProdutoRepository repo;

    public ProdutoService(EntityManagerFactory emf) {
        this.repo = new ProdutoRepository(emf);
    }

    public List<Produto> listarTodos() {
        return repo.buscarTodos();
    }

    public Optional<Produto> buscarPorId(Long id) {
        return repo.buscarPorId(id);
    }

    public Optional<Produto> buscarPorCodBarras(String cod) {
        if (cod == null || cod.isBlank())
            throw new RegraNegocioException("Código de barras não informado.");
        return repo.buscarPorCodBarras(cod);
    }

    public List<Produto> buscarPorFiltros(String nome, BigDecimal precoMin, BigDecimal precoMax, String codBarras) {
        return repo.buscarPorFiltros(nome, precoMin, precoMax, codBarras);
    }

    /**
     * Salva ou atualiza um produto, aplicando todas as validações.
     */
    public Produto salvar(Produto produto) {
        validarProduto(produto);
        return repo.salvar(produto);
    }

    /**
     * Adiciona estoque ao produto. Exige senha de administrador.
     */
    public Produto adicionarEstoque(Long produtoId, int quantidade, String senhaAdmin) {
        if (!SenhaUtil.isAdmin(senhaAdmin))
            throw new RegraNegocioException("Senha de administrador incorreta.");
        if (quantidade <= 0)
            throw new RegraNegocioException("Quantidade deve ser maior que zero.");
        if (quantidade > 9999)
            throw new RegraNegocioException("Quantidade máxima por entrada é 9999.");

        Produto produto = repo.buscarPorId(produtoId)
                .orElseThrow(() -> new RegraNegocioException("Produto não encontrado."));

        int estoqueAtual = produto.getEstoque() != null ? produto.getEstoque() : 0;
        produto.setEstoque(estoqueAtual + quantidade);
        return repo.salvar(produto);
    }

    /**
     * Atualiza o preço de custo e recalcula o preço médio ponderado.
     */
    public Produto atualizarPrecoCusto(Produto produto, BigDecimal novoCusto) {
        if (novoCusto == null || novoCusto.compareTo(BigDecimal.ZERO) <= 0) return produto;

        BigDecimal custoAnterior = produto.getPrecoCusto();
        boolean custoMudou = custoAnterior == null || novoCusto.compareTo(custoAnterior) != 0;

        if (custoMudou) {
            BigDecimal medioAnt = produto.getPrecoMedio();
            BigDecimal qtdHist  = produto.getQtdHistoricoCusto();
            BigDecimal peso     = BigDecimal.valueOf(Math.max(
                    produto.getEstoque() != null ? produto.getEstoque() : 0, 1));

            if (medioAnt == null || qtdHist == null || qtdHist.compareTo(BigDecimal.ZERO) == 0) {
                produto.setPrecoMedio(novoCusto);
                produto.setQtdHistoricoCusto(peso);
            } else {
                BigDecimal novaQtd   = qtdHist.add(peso);
                BigDecimal novoMedio = qtdHist.multiply(medioAnt)
                        .add(peso.multiply(novoCusto))
                        .divide(novaQtd, 2, RoundingMode.HALF_UP);
                produto.setPrecoMedio(novoMedio);
                produto.setQtdHistoricoCusto(novaQtd);
            }
            produto.setPrecoCusto(novoCusto);
        }
        return produto;
    }

    public void excluir(Long id) {
        repo.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Produto não encontrado."));
        repo.excluir(id);
    }

    // ── Calculadora Impressão 3D ──────────────────────────────────────────────

    /**
     * Parâmetros de entrada para o cálculo de custo de impressão 3D.
     *
     * @param precoPorKgFilamento  Preço do kg do filamento em R$
     * @param gramasUsadas         Quantidade de material usada na impressão, em gramas
     * @param horasImpressao       Duração da impressão em horas
     * @param tarifaEnergia        Taxa de energia da distribuidora em R$/kWh
     * @param consumoWatts         Consumo médio da impressora em Watts
     * @param valorHoraMaoDeObra   Valor da hora de trabalho em R$ (0 para ignorar)
     * @param margemLucroPercent   Margem de lucro em % (ex: 500 para 500%)
     */
    public record Impressao3DInput(
            double precoPorKgFilamento,
            double gramasUsadas,
            double horasImpressao,
            double tarifaEnergia,
            double consumoWatts,
            double valorHoraMaoDeObra,
            double margemLucroPercent
    ) {}

    /**
     * Resultado do cálculo de impressão 3D.
     */
    public record Impressao3DResultado(
            BigDecimal custoFilamento,
            BigDecimal custoEnergia,
            BigDecimal custoMaoDeObra,
            BigDecimal custoTotal,
            BigDecimal precoVendaSugerido
    ) {}

    /**
     * Calcula os custos de uma impressão 3D e retorna o preço de venda sugerido.
     * Toda a lógica de negócio fica aqui, isolada da UI.
     *
     * Fórmulas:
     *   custoFilamento  = (precoPorKg / 1000) × gramas
     *   custoEnergia    = (watts / 1000) × horas × tarifaKWh
     *   custoMaoDeObra  = valorHora × horas
     *   custoTotal      = filamento + energia + maoDeObra
     *   precoVenda      = custoTotal × (1 + margem/100)
     */
    public Impressao3DResultado calcularImpressao3D(Impressao3DInput in) {
        if (in.precoPorKgFilamento() < 0)
            throw new RegraNegocioException("Preço do filamento não pode ser negativo.");
        if (in.gramasUsadas() < 0)
            throw new RegraNegocioException("Gramas usadas não podem ser negativas.");
        if (in.horasImpressao() < 0)
            throw new RegraNegocioException("Duração da impressão não pode ser negativa.");
        if (in.tarifaEnergia() < 0)
            throw new RegraNegocioException("Tarifa de energia não pode ser negativa.");
        if (in.consumoWatts() < 0)
            throw new RegraNegocioException("Consumo em watts não pode ser negativo.");
        if (in.valorHoraMaoDeObra() < 0)
            throw new RegraNegocioException("Valor hora de mão de obra não pode ser negativo.");
        if (in.margemLucroPercent() < 0)
            throw new RegraNegocioException("Margem de lucro não pode ser negativa.");

        BigDecimal custoFilamento = BigDecimal.valueOf(in.precoPorKgFilamento() / 1000.0 * in.gramasUsadas())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal custoEnergia = BigDecimal.valueOf((in.consumoWatts() / 1000.0) * in.horasImpressao() * in.tarifaEnergia())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal custoMaoDeObra = BigDecimal.valueOf(in.valorHoraMaoDeObra() * in.horasImpressao())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal custoTotal = custoFilamento.add(custoEnergia).add(custoMaoDeObra);

        BigDecimal fatorMargem = BigDecimal.ONE.add(
                BigDecimal.valueOf(in.margemLucroPercent()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal precoVenda = custoTotal.multiply(fatorMargem).setScale(2, RoundingMode.HALF_UP);

        return new Impressao3DResultado(custoFilamento, custoEnergia, custoMaoDeObra, custoTotal, precoVenda);
    }

    // ── Validações ────────────────────────────────────────────────────────────

    private void validarProduto(Produto p) {
        if (Validator.isBlank(p.getNome()))
            throw new RegraNegocioException("Nome é obrigatório.");
        if (p.getNome().length() > 100)
            throw new RegraNegocioException("Nome deve ter no máximo 100 caracteres.");
        if (p.getValor() == null || p.getValor().compareTo(BigDecimal.ZERO) <= 0)
            throw new RegraNegocioException("Preço de venda deve ser maior que zero.");
        if (!Validator.isBlank(p.getCodBarras()) && p.getCodBarras().length() > 20)
            throw new RegraNegocioException("Código de barras deve ter no máximo 20 caracteres.");
        if (!Validator.isBlank(p.getNumeroSerie()) && p.getNumeroSerie().length() > 20)
            throw new RegraNegocioException("Número de série deve ter no máximo 20 caracteres.");
        if (repo.existeNome(p.getNome(), p.getId()))
            throw new RegraNegocioException("Já existe um produto com o nome '" + p.getNome() + "'.");
        if (!Validator.isBlank(p.getCodBarras()) && repo.existeCodBarras(p.getCodBarras(), p.getId()))
            throw new RegraNegocioException("Já existe um produto com o código de barras '" + p.getCodBarras() + "'.");
        if (!Validator.isBlank(p.getNumeroSerie()) && repo.existeNumeroSerie(p.getNumeroSerie(), p.getId()))
            throw new RegraNegocioException("Já existe um produto com o número de série '" + p.getNumeroSerie() + "'.");
    }
}
