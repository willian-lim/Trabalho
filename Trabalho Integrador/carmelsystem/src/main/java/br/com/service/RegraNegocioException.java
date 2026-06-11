package br.carmel.service;

/**
 * Exceção lançada quando uma regra de negócio é violada.
 * A UI captura e exibe a mensagem ao usuário.
 */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}