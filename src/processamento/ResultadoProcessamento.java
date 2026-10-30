package processamento;

import java.nio.file.Path;

/** O que aconteceu com um arquivo rotaNN.txt: sucesso ou erro, a mensagem e para onde ele foi movido. */
public class ResultadoProcessamento {

	private final String arquivo;
	private final boolean sucesso;
	private final String mensagem;
	private final Path destino;

	public ResultadoProcessamento(final String arquivo, final boolean sucesso, final String mensagem, final Path destino) {
		this.arquivo = arquivo;
		this.sucesso = sucesso;
		this.mensagem = mensagem;
		this.destino = destino;
	}

	public String getArquivo() {
		return arquivo;
	}

	public boolean isSucesso() {
		return sucesso;
	}

	public String getMensagem() {
		return mensagem;
	}

	/** Caminho final do arquivo (null se não foi possível movê-lo). */
	public Path getDestino() {
		return destino;
	}

	@Override
	public String toString() {
		return arquivo + " -> " + (sucesso ? "PROCESSADO" : "NÃO PROCESSADO") + " | " + mensagem;
	}
}
