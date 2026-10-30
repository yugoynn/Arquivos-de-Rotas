package arquivo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import grafo.Aresta;
import util.LeitorTexto;

/**
 * Lê um arquivo rotaNN.txt e valida TUDO antes de devolver o {@link ArquivoRota}.
 * Se algo estiver errado lança {@link ArquivoRotaInvalidoException} com a mensagem
 * que o roteiro de testes pede.
 *
 * Ordem das validações:
 * <ol>
 * <li>Formato de cada linha (HEADER, 01, 02, TRAILER) — testes 1.8 e 1.9;</li>
 * <li>Linhas do TRAILER: RC x linhas 01 e RP x linhas 02 — testes 2.0 e 2.1;</li>
 * <li>Total de nós do HEADER (NN) — teste 1.6;</li>
 * <li>Soma dos pesos do HEADER (SP) e do TRAILER (P) — teste 1.7;</li>
 * <li>Cada peso (02) precisa ter a sua conexão (01) e vice-versa.</li>
 * </ol>
 */
public class LeitorArquivoRota {

	// Tamanhos conforme a "Descrição dos campos" do enunciado.
	private static final Pattern HEADER = Pattern.compile("00(\\d{2})(\\d{1,5})");
	private static final Pattern CONEXAO = Pattern.compile("01([A-Za-z]{1,2})=([A-Za-z]{1,2})");
	private static final Pattern PESO = Pattern.compile("02([A-Za-z]{1,2});([A-Za-z]{1,2})=(\\d{1,4})");
	private static final Pattern TRAILER = Pattern.compile("09RC=(\\d{1,2});RP=(\\d{1,2});(\\d{1,4})");

	/** Lê e valida o arquivo informado. */
	public ArquivoRota ler(final Path arquivo) throws IOException, ArquivoRotaInvalidoException {
		return ler(LeitorTexto.lerLinhas(arquivo));
	}

	/** Valida as linhas de um arquivo de rota (também usado para conferir o arquivo gerado pela tela). */
	public ArquivoRota ler(final List<String> linhasDoArquivo) throws ArquivoRotaInvalidoException {

		// Guarda o texto de cada linha preenchida junto com o número da linha no arquivo.
		List<String> textos = new ArrayList<String>();
		List<Integer> numeros = new ArrayList<Integer>();
		for (int i = 0; i < linhasDoArquivo.size(); i++) {
			String texto = linhasDoArquivo.get(i).trim();
			if (!texto.isEmpty()) {
				textos.add(texto);
				numeros.add(i + 1);
			}
		}
		if (textos.isEmpty()) {
			throw new ArquivoRotaInvalidoException("Arquivo vazio.");
		}

		// ---------------- HEADER (primeira linha) ----------------
		String header = textos.get(0);
		Matcher mHeader = HEADER.matcher(header);
		if (!header.startsWith("00") || !mHeader.matches()) {
			throw new ArquivoRotaInvalidoException("Header inválido (linha " + numeros.get(0) + ": \"" + header
					+ "\"). Formato esperado: 00NNSP, com NN de 2 dígitos e SP de até 5 dígitos.");
		}
		int totalNosHeader = Integer.parseInt(mHeader.group(1));
		int somaHeader = Integer.parseInt(mHeader.group(2));

		// ---------------- TRAILER (última linha) ----------------
		int ultima = textos.size() - 1;
		String trailer = textos.get(ultima);
		if (ultima == 0 || !trailer.startsWith("09")) {
			throw new ArquivoRotaInvalidoException(
					"Trailer inválido: a última linha do arquivo deve ser o TRAILER (09RC=NN;RP=NN;P).");
		}
		Matcher mTrailer = TRAILER.matcher(trailer);
		if (!mTrailer.matches()) {
			throw new ArquivoRotaInvalidoException("Trailer inválido (linha " + numeros.get(ultima) + ": \"" + trailer
					+ "\"). Formato esperado: 09RC=NN;RP=NN;P.");
		}
		int rcTrailer = Integer.parseInt(mTrailer.group(1));
		int rpTrailer = Integer.parseInt(mTrailer.group(2));
		int somaTrailer = Integer.parseInt(mTrailer.group(3));

		// ---------------- Linhas do meio (01 e 02) ----------------
		List<Conexao> conexoes = new ArrayList<Conexao>();
		List<Integer> linhasConexao = new ArrayList<Integer>();
		List<Aresta> pesos = new ArrayList<Aresta>();
		List<Integer> linhasPeso = new ArrayList<Integer>();
		boolean jaLeuPeso = false;

		for (int i = 1; i < ultima; i++) {
			String linha = textos.get(i);
			int numero = numeros.get(i);

			if (linha.startsWith("01")) {
				if (jaLeuPeso) {
					throw new ArquivoRotaInvalidoException("Resumo de conexões inválido (linha " + numero
							+ "): as linhas 01 devem vir antes das linhas 02.");
				}
				Matcher m = CONEXAO.matcher(linha);
				if (!m.matches()) {
					throw new ArquivoRotaInvalidoException("Resumo de conexões inválido (linha " + numero + ": \"" + linha
							+ "\"). Formato esperado: 01NO=ND, com NO e ND formados por 1 ou 2 letras.");
				}
				String origem = m.group(1).toUpperCase();
				String destino = m.group(2).toUpperCase();
				if (origem.equals(destino)) {
					throw new ArquivoRotaInvalidoException("Resumo de conexões inválido (linha " + numero + "): o nó "
							+ origem + " está ligado a ele mesmo.");
				}
				for (Conexao existente : conexoes) {
					if (existente.liga(origem, destino)) {
						throw new ArquivoRotaInvalidoException("Resumo de conexões inválido (linha " + numero
								+ "): a conexão " + origem + "=" + destino + " está repetida.");
					}
				}
				conexoes.add(new Conexao(origem, destino));
				linhasConexao.add(numero);

			} else if (linha.startsWith("02")) {
				jaLeuPeso = true;
				Matcher m = PESO.matcher(linha);
				if (!m.matches()) {
					throw new ArquivoRotaInvalidoException("Resumo dos pesos inválido (linha " + numero + ": \"" + linha
							+ "\"). Formato esperado: 02NO;ND=P, com NO e ND de 1 ou 2 letras e P de até 4 dígitos.");
				}
				String origem = m.group(1).toUpperCase();
				String destino = m.group(2).toUpperCase();
				int peso = Integer.parseInt(m.group(3));
				if (peso < 1) {
					throw new ArquivoRotaInvalidoException(
							"Resumo dos pesos inválido (linha " + numero + "): o peso deve ser maior que zero.");
				}
				if (origem.equals(destino)) {
					throw new ArquivoRotaInvalidoException("Resumo dos pesos inválido (linha " + numero + "): o nó "
							+ origem + " está ligado a ele mesmo.");
				}
				for (Aresta existente : pesos) {
					if (existente.liga(origem, destino)) {
						throw new ArquivoRotaInvalidoException("Resumo dos pesos inválido (linha " + numero
								+ "): o peso de " + origem + ";" + destino + " está repetido.");
					}
				}
				pesos.add(new Aresta(origem, destino, peso));
				linhasPeso.add(numero);

			} else if (linha.startsWith("00")) {
				throw new ArquivoRotaInvalidoException(
						"Header inválido: existe mais de um HEADER (linha " + numero + ").");
			} else if (linha.startsWith("09")) {
				throw new ArquivoRotaInvalidoException(
						"Trailer inválido: o TRAILER deve ser a última linha (encontrado na linha " + numero + ").");
			} else {
				throw new ArquivoRotaInvalidoException("Linha " + numero + " com identificador de registro inválido: \""
						+ linha + "\" (esperado 00, 01, 02 ou 09).");
			}
		}

		if (conexoes.isEmpty()) {
			throw new ArquivoRotaInvalidoException(
					"Resumo de conexões inválido: o arquivo precisa ter pelo menos uma linha 01.");
		}
		if (pesos.isEmpty()) {
			throw new ArquivoRotaInvalidoException(
					"Resumo dos pesos inválido: o arquivo precisa ter pelo menos uma linha 02.");
		}

		// ---------------- Totais do TRAILER (testes 2.0 e 2.1) ----------------
		if (rcTrailer != conexoes.size()) {
			throw new ArquivoRotaInvalidoException("Número de linhas do Resumo de Conexões difere (TRAILER RC = "
					+ rcTrailer + " e linhas de conexão no arquivo = " + conexoes.size() + ").");
		}
		if (rpTrailer != pesos.size()) {
			throw new ArquivoRotaInvalidoException("Número de linhas do Resumo de Pesos difere (TRAILER RP = "
					+ rpTrailer + " e linhas de peso no arquivo = " + pesos.size() + ").");
		}

		ArquivoRota arquivo = new ArquivoRota(totalNosHeader, somaHeader, conexoes, pesos, rcTrailer, rpTrailer,
				somaTrailer);

		// ---------------- Total de nós (teste 1.6) ----------------
		Set<String> nos = arquivo.getNos();
		if (totalNosHeader != nos.size()) {
			throw new ArquivoRotaInvalidoException("Número totais de nós inválido (HEADER NN = " + totalNosHeader
					+ " e nós encontrados no arquivo = " + nos.size() + ").");
		}

		// ---------------- Soma dos pesos (teste 1.7) ----------------
		int soma = 0;
		for (Aresta a : pesos) {
			soma += a.getPeso();
		}
		if (somaHeader != soma) {
			throw new ArquivoRotaInvalidoException("Soma dos pesos difere (Valor do Registro HEADER = " + somaHeader
					+ " e Soma dos Pesos = " + soma + ").");
		}
		if (somaTrailer != soma) {
			throw new ArquivoRotaInvalidoException("Soma dos pesos difere (Valor do Registro TRAILER = " + somaTrailer
					+ " e Soma dos Pesos = " + soma + ").");
		}

		// ---------------- Conexões x Pesos ----------------
		for (int i = 0; i < pesos.size(); i++) {
			Aresta a = pesos.get(i);
			if (!existeConexao(conexoes, a.getOrigem(), a.getDestino())) {
				throw new ArquivoRotaInvalidoException("Resumo dos pesos inválido (linha " + linhasPeso.get(i)
						+ "): não existe conexão 01 para " + a.getOrigem() + ";" + a.getDestino() + ".");
			}
		}
		for (int i = 0; i < conexoes.size(); i++) {
			Conexao c = conexoes.get(i);
			if (!existePeso(pesos, c.getOrigem(), c.getDestino())) {
				throw new ArquivoRotaInvalidoException("Resumo de conexões inválido (linha " + linhasConexao.get(i)
						+ "): a conexão " + c + " não tem peso (linha 02).");
			}
		}

		return arquivo;
	}

	private static boolean existeConexao(final List<Conexao> conexoes, final String a, final String b) {
		for (Conexao c : conexoes) {
			if (c.liga(a, b)) {
				return true;
			}
		}
		return false;
	}

	private static boolean existePeso(final List<Aresta> pesos, final String a, final String b) {
		for (Aresta p : pesos) {
			if (p.liga(a, b)) {
				return true;
			}
		}
		return false;
	}
}
