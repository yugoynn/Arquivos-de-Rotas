package arquivo;

/**
 * Lançada quando um arquivo rotaNN.txt não respeita a sintaxe ou os totais
 * (HEADER, RESUMO DE CONEXÕES, RESUMO DOS PESOS ou TRAILER).
 * Um arquivo que gera esta exceção é movido para a pasta NaoProcessado.
 */
public class ArquivoRotaInvalidoException extends Exception {

	private static final long serialVersionUID = 1L;

	public ArquivoRotaInvalidoException(final String mensagem) {
		super(mensagem);
	}
}
