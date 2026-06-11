package br.carmel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa uma licença já utilizada/ativada no banco de dados.
 *
 * Objetivo: impedir que a mesma chave seja reinserida após expirar,
 * ou que o arquivo local seja apagado para "resetar" a licença.
 *
 * A chave não é armazenada em texto puro — apenas seu hash SHA-256
 * (campo {@code hashChave}) é persistido, protegendo a chave original.
 */
@Getter
@Setter
@Entity
@Table(
    name = "licenca",
    uniqueConstraints = @UniqueConstraint(name = "uq_licenca_hash", columnNames = "hash_chave")
)
public class Licenca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SHA-256 da chave bruta recebida (Base64 original).
     * Permite verificar duplicidade sem expor a chave.
     */
    @Column(name = "hash_chave", nullable = false, length = 64, unique = true)
    private String hashChave;

    /** Data de início da validade informada pela chave. */
    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    /** Data de expiração calculada no momento da ativação. */
    @Column(name = "data_expiracao", nullable = false)
    private LocalDate dataExpiracao;

    /** Momento exato em que esta chave foi ativada neste sistema. */
    @Column(name = "ativada_em", nullable = false)
    private LocalDateTime ativadaEm;

    /**
     * Fingerprint da máquina no momento da ativação.
     * Combina: nome do host + nome do usuário do SO.
     * Serve para auditoria; a licença não é vinculada exclusivamente
     * a uma máquina (isso depende de decisão de negócio).
     */
    @Column(name = "fingerprint_maquina", nullable = false, length = 256)
    private String fingerprintMaquina;

    /** Número serial sequencial embutido na chave (para rastreabilidade). */
    @Column(name = "serial", nullable = false)
    private int serial;
}