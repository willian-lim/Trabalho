package br.carmel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    
    @Column(name = "hash_chave", nullable = false, length = 64, unique = true)
    private String hashChave;

    
    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    
    @Column(name = "data_expiracao", nullable = false)
    private LocalDate dataExpiracao;

    
    @Column(name = "ativada_em", nullable = false)
    private LocalDateTime ativadaEm;

    
    @Column(name = "fingerprint_maquina", nullable = false, length = 256)
    private String fingerprintMaquina;

    
    @Column(name = "serial", nullable = false)
    private int serial;
}