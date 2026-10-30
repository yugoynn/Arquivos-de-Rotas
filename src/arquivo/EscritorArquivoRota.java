package arquivo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import grafo.Aresta;

/**
 * Gera um arquivo rotaNN.txt a partir de uma lista de arestas (botão SALVAR da
 * tela Visível), calculando HEADER e TRAILER automaticamente.
 */
public final class EscritorArquivoRota {

	/** Nome válido de arquivo de rota: rota + número + .txt (rota01.txt, rota2.txt, ROTA15.TXT...). */
	public static final Pattern NOME_ARQUIVO_ROTA = Pattern.compile("(?i)rota(\\d+)\\.txt");

	private static final Pattern CODIGO = Pattern.compile("[A-Z]{1,2}");

	private EscritorArquivoRota() {
	}

	/** Monta as linhas do arquivo. Lança exceção se os dados não cabem no formato. */
	public static List<String> gerarLinhas(final List<Aresta> arestas) throws ArquivoRotaInvalidoException {
		if (arestas.isEmpty()) {
			throw new ArquivoRotaInvalidoException("Adicione pelo menos uma rota antes de salvar.");
		}

		Set<String> nos = new LinkedHashSet<String>();
		int soma = 0;
		for (Aresta a : arestas) {
			if (!CODIGO.matcher(a.getOrigem()).matches() || !CODIGO.matcher(a.getDestino()).matches()) {
				throw new ArquivoRotaInvalidoException("Os códigos devem ter 1 ou 2 letras (" + a + ").");
			}
			nos.add(a.getOrigem());
			nos.add(a.getDestino());
			soma += a.getPeso();
		}
		if (nos.size() > 99) {
			throw new ArquivoRotaInvalidoException("O formato permite no máximo 99 nós (há " + nos.size() + ").");
		}
		if (arestas.size() > 99) {
			throw new ArquivoRotaInvalidoException("O formato permite no máximo 99 linhas de conexão (há " + arestas.size() + ").");
		}
		if (soma > 9999) {
			throw new ArquivoRotaInvalidoException("A soma dos pesos (" + soma + ") passa do limite de 4 dígitos do TRAILER.");
		}

		List<String> linhas = new ArrayList<String>();
		linhas.add("00" + String.format("%02d", nos.size()) + soma);
		for (Aresta a : arestas) {
			linhas.add("01" + a.getOrigem() + "=" + a.getDestino());
		}
		for (Aresta a : arestas) {
			linhas.add("02" + a.getOrigem() + ";" + a.getDestino() + "=" + a.getPeso());
		}
		linhas.add("09RC=" + String.format("%02d", arestas.size()) + ";RP=" + String.format("%02d", arestas.size()) + ";" + soma);
		return linhas;
	}

	/** Gera, confere com o próprio leitor e grava o arquivo. */
	public static void salvar(final Path destino, final List<Aresta> arestas) throws ArquivoRotaInvalidoException, IOException {
		List<String> linhas = gerarLinhas(arestas);
		new LeitorArquivoRota().ler(linhas); // garante que o arquivo gerado passa em todas as validações
		StringBuilder conteudo = new StringBuilder();
		for (String linha : linhas) {
			conteudo.append(linha).append("\r\n");
		}
		Files.write(destino, conteudo.toString().getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Próximo nome sequencial (rota01.txt, rota02.txt...) olhando as pastas
	 * informadas, para não repetir número de arquivo já usado.
	 */
	public static String proximoNome(final Path... pastas) {
		int maior = 0;
		for (Path pasta : pastas) {
			if (pasta == null || !Files.isDirectory(pasta)) {
				continue;
			}
			try (DirectoryStream<Path> arquivos = Files.newDirectoryStream(pasta)) {
				for (Path arquivo : arquivos) {
					Matcher m = Pattern.compile("(?i)rota(\\d+).*\\.txt").matcher(arquivo.getFileName().toString());
					if (m.matches() && m.group(1).length() < 9) {
						maior = Math.max(maior, Integer.parseInt(m.group(1)));
					}
				}
			} catch (IOException ignorado) {
				// pasta inacessível: apenas não considera
			}
		}
		return String.format("rota%02d.txt", maior + 1);
	}
}
