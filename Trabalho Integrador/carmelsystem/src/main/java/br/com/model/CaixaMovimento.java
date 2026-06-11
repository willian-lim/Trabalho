package br.carmel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Movimento de caixa.
 * Cada abertura gera um número sequencial de caixa (numeroCaixa).
 * Todos os movimentos do mesmo turno compartilham o mesmo numeroCaixa.
 */
@Getter
@Setter
@Entity
@Table(name = "caixa_movimento")
public class CaixaMovimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Número sequencial do caixa (Caixa 1, Caixa 2, ...).
     * Gerado na abertura e herdado por todos os movimentos do turno.
     */
    @Column(name = "numero_caixa")
    private Long numeroCaixa;

    @Column(name = "data_movimento", nullable = false)
    private LocalDateTime dataMovimento;

    @Column(name = "data_caixa", nullable = false)
    private LocalDate dataCaixa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoCaixaMovimento tipo;

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "descricao", length = 300)
    private String descricao;

    // Referência opcional ao pagamento que gerou o movimento (para vendas)
    @ManyToOne
    @JoinColumn(name = "pagamento_id")
    private Pagamento pagamento;

    @PrePersist
    protected void onCreate() {
        if (dataMovimento == null) dataMovimento = LocalDateTime.now();
        if (dataCaixa == null) dataCaixa = LocalDate.now();
    }
}
