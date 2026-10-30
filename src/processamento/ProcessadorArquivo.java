package processamento;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import arquivo.ArquivoRota;
import arquivo.ArquivoRotaInvalidoException;
import arquivo.Conexao;
import arquivo.LeitorArquivoRota;
import config.Configuracao;
import grafo.MapaRotas;
import grafo.MenorCaminho;

/**
 * Processa UM arquivo rotaNN.txt do começo ao fim:
 *
 * <ol>
 * <li>lê e valida (LeitorArquivoRota);</li>
 * <li>monta o grafo e roda o Dijkstra do professor, do primeiro nó (origem da
 * primeira conexão) até o último (destino da última conexão);</li>
 * <li>se deu tudo certo, move para a pasta Processado;</li>
 * <li>se deu QUALQUER exceção no meio do caminho, move para NaoProcessado e
 * grava ao lado um rotaNN.erro.txt com o motivo.</li>
 * </ol>
 */
public class ProcessadorArquivo {

	private final LeitorArquivoRota leitor = new LeitorArquivoRota();

	public ResultadoProcessamento processar(final Path arquivo, final Configuracao configuracao) {
		String nome = arquivo.getFileName().toString();
		try {
			ArquivoRota rota = leitor.ler(arquivo);

			List<Conexao> conexoes = rota.getConexoes();
			String origem = conexoes.get(0).getOrigem();
			String destino = conexoes.get(conexoes.size() - 1).getDestino();
			MenorCaminho caminho = new MapaRotas(rota.getArestas()).calcular(origem, destino);

			Path movido = mover(arquivo, configuracao.getSucesso());
			String mensagem = "OK: " + rota.getTotalNosHeader() + " nós, " + rota.getArestas().size()
					+ " arestas, soma dos pesos " + rota.getSomaPesosHeader() + ". Menor caminho " + origem + " -> "
					+ destino + ": " + caminho;
			return new ResultadoProcessamento(nome, true, mensagem, movido);

		} catch (ArquivoRotaInvalidoException e) {
			return falha(arquivo, configuracao, e.getMessage());
		} catch (IOException e) {
			return falha(arquivo, configuracao, "Erro de leitura/gravação: " + e.getMessage());
		} catch (RuntimeException e) {
			return falha(arquivo, configuracao, "Erro inesperado: " + e);
		}
	}

	private ResultadoProcessamento falha(final Path arquivo, final Configuracao configuracao, final String motivo) {
		String nome = arquivo.getFileName().toString();
		try {
			Path movido = mover(arquivo, configuracao.getErro());
			gravarMotivo(movido, motivo);
			return new ResultadoProcessamento(nome, false, motivo, movido);
		} catch (IOException e) {
			return new ResultadoProcessamento(nome, false,
					motivo + " (e não foi possível mover o arquivo: " + e.getMessage() + ")", null);
		}
	}

	/** Move o arquivo para a pasta; se já existir um com o mesmo nome, acrescenta data/hora ao nome. */
	static Path mover(final Path arquivo, final Path pasta) throws IOException {
		Files.createDirectories(pasta);
		String nome = arquivo.getFileName().toString();
		Path alvo = pasta.resolve(nome);
		if (Files.exists(alvo)) {
			String carimbo = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
			int ponto = nome.lastIndexOf('.');
			String base = ponto > 0 ? nome.substring(0, ponto) : nome;
			String extensao = ponto > 0 ? nome.substring(ponto) : "";
			alvo = pasta.resolve(base + "_" + carimbo + extensao);
		}
		return Files.move(arquivo, alvo);
	}

	/** Cria "rotaNN.erro.txt" ao lado do arquivo com erro, explicando o motivo. */
	private static void gravarMotivo(final Path arquivoMovido, final String motivo) {
		String nome = arquivoMovido.getFileName().toString();
		int ponto = nome.lastIndexOf('.');
		String base = ponto > 0 ? nome.substring(0, ponto) : nome;
		Path log = arquivoMovido.resolveSibling(base + ".erro.txt");
		String texto = "Arquivo: " + nome + "\r\n"
				+ "Data: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()) + "\r\n"
				+ "Motivo: " + motivo + "\r\n";
		try {
			Files.write(log, texto.getBytes(StandardCharsets.UTF_8));
		} catch (IOException ignorado) {
			// o motivo também aparece no log da tela/console
		}
	}
}
