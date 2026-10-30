package config;

/**
 * Erro de configuração: pastas inexistentes, config.txt ausente, em branco,
 * sem a linha 2 ou com o separador "=" trocado (testes 1.1 a 1.5 do roteiro).
 */
public class ConfiguracaoException extends Exception {

	private static final long serialVersionUID = 1L;

	public ConfiguracaoException(final String mensagem) {
		super(mensagem);
	}

	public ConfiguracaoException(final String mensagem, final Throwable causa) {
		super(mensagem, causa);
	}
}
