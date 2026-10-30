package config;

import java.nio.file.Path;

/**
 * Valores da configuração do sistema (os 4 campos da tela de Configuração).
 *
 * <ul>
 * <li><b>pasta</b>: diretório raiz monitorado, onde os arquivos rotaNN.txt são copiados (ex.: C:\Teste);</li>
 * <li><b>sucesso</b>: para onde vão os arquivos processados com sucesso (linha "Processado=");</li>
 * <li><b>erro</b>: para onde vão os arquivos com erro (linha "Não Processado=");</li>
 * <li><b>rotaAutomatica</b>: se verdadeiro, o sistema monitora a pasta em segundo plano.</li>
 * </ul>
 */
public class Configuracao {

	private final Path pasta;
	private final Path sucesso;
	private final Path erro;
	private final boolean rotaAutomatica;
	private final boolean primeiraVez;

	public Configuracao(final Path pasta, final Path sucesso, final Path erro, final boolean rotaAutomatica,
			final boolean primeiraVez) {
		this.pasta = pasta;
		this.sucesso = sucesso;
		this.erro = erro;
		this.rotaAutomatica = rotaAutomatica;
		this.primeiraVez = primeiraVez;
	}

	public Path getPasta() {
		return pasta;
	}

	public Path getSucesso() {
		return sucesso;
	}

	public Path getErro() {
		return erro;
	}

	public boolean isRotaAutomatica() {
		return rotaAutomatica;
	}

	/**
	 * Verdadeiro quando o config.txt ainda não foi salvo pela tela de Configuração
	 * (por exemplo, quando só tem as 2 linhas criadas manualmente). Nesse caso a
	 * aplicação abre a tela de Configuração automaticamente.
	 */
	public boolean isPrimeiraVez() {
		return primeiraVez;
	}

	@Override
	public String toString() {
		return "Pasta=" + pasta + " | Sucesso=" + sucesso + " | Erro=" + erro + " | Rota automática="
				+ (rotaAutomatica ? "SIM" : "NÃO");
	}
}
