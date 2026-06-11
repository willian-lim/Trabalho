package br.carmel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(length = 500)
    private String descricao;

    
    @Column(nullable = false)
    private BigDecimal valor;

    
    @Column(name = "preco_custo", precision = 10, scale = 2)
    private BigDecimal precoCusto;

    
    @Column(name = "preco_medio", precision = 10, scale = 2)
    private BigDecimal precoMedio;

    
    @Column(name = "qtd_historico_custo", precision = 10, scale = 2)
    private BigDecimal qtdHistoricoCusto = BigDecimal.ZERO;

    @Column(unique = true, length = 20)
    private String codBarras;

    @Column(unique = true, length = 20)
    private String numeroSerie;

    
    @Column(name = "estoque", nullable = false)
    private Integer estoque = 0;

    @Override
    public String toString() {
        return nome != null ? nome : "";
    }
}