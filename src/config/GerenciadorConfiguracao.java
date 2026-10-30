package config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.List;

import util.LeitorTexto;

/**
 * Lê e grava o arquivo C:\Teste\Configuracao\config.txt e cria os diretórios.
 *
 * Formato do config.txt (as 2 primeiras linhas são as exigidas no enunciado;
 * as 2 últimas são acrescentadas pela tela de Configuração):
 *
 * <pre>
 * Processado=C:\Teste\Processado
 * Não Processado=C:\Teste\NaoProcessado
 * Pasta=C:\Teste
 * RotaAutomatica=NAO
 * </pre>
 *
 * Pasta raiz: C:\Teste no Windows (como no enunciado) e ~/Teste no Linux/Mac.
 * Para usar outra pasta, rode com -Drota.base=/outra/pasta.
 */
public class GerenciadorConfiguracao {

	/** Pasta raiz padrão: C:\Teste no Windows, ~/Teste (pasta do usuário) nos outros sistemas. */
	public static final String PASTA_BASE_PADRAO = System.getProperty("os.name", "").toLowerCase().startsWith("windows")
			? "C:\\Teste"
			: Paths.get(System.getProperty("user.home"), "Teste").toString();

	private static final String CHAVE_PROCESSADO = "processado";
	private static final String CHAVE_NAO_PROCESSADO = "naoprocessado";
	private static final String CHAVE_PASTA = "pasta";
	private static final String CHAVE_ROTA_AUTOMATICA = "rotaautomatica";

	private final Path base;
	private final Path pastaConfiguracao;
	private final Path arquivoConfig;

	public GerenciadorConfiguracao() {
		this(Paths.get(System.getProperty("rota.base", PASTA_BASE_PADRAO)));
	}

	public GerenciadorConfiguracao(final Path base) {
		this.base = base;
		this.pastaConfiguracao = base.resolve("Configuracao");
		this.arquivoConfig = pastaConfiguracao.resolve("config.txt");
	}

	public Path getBase() {
		return base;
	}

	public Path getPastaConfiguracao() {
		return pastaConfiguracao;
	}

	public Path getArquivoConfig() {
		return arquivoConfig;
	}

	/**
	 * Lê o config.txt. Lança {@link ConfiguracaoException} com uma mensagem clara
	 * em cada situação do roteiro de testes (1.1 a 1.5).
	 */
	public Configuracao carregar() throws ConfiguracaoException {
		// Teste 1.1 - diretórios não criados.
		if (!Files.isDirectory(base)) {
			throw new ConfiguracaoException("Diretório " + base + " não encontrado. Crie-o manualmente.");
		}
		if (!Files.isDirectory(pastaConfiguracao)) {
			throw new ConfiguracaoException("Diretório " + pastaConfiguracao + " não encontrado. Crie-o manualmente.");
		}

		// Teste 1.2 - config.txt não existe.
		if (!Files.isRegularFile(arquivoConfig)) {
			throw new ConfiguracaoException("Arquivo " + arquivoConfig + " não encontrado.");
		}

		List<String> linhas;
		try {
			linhas = LeitorTexto.lerLinhas(arquivoConfig);
		} catch (IOException e) {
			throw new ConfiguracaoException("Não foi possível ler o arquivo " + arquivoConfig + ": " + e.getMessage(), e);
		}

		String processado = null;
		String naoProcessado = null;
		String pasta = null;
		String rotaAutomatica = null;
		int linhasPreenchidas = 0;

		for (int i = 0; i < linhas.size(); i++) {
			String linha = linhas.get(i).trim();
			if (linha.isEmpty()) {
				continue;
			}
			linhasPreenchidas++;
			int numero = i + 1;

			// Teste 1.5 - "=" trocado por "@" (ou qualquer outro caractere).
			int separador = linha.indexOf('=');
			if (separador < 0) {
				throw new ConfiguracaoException("Linha " + numero + " do config.txt inválida: separador '=' não encontrado (\""
						+ linha + "\").");
			}

			String chave = normalizarChave(linha.substring(0, separador));
			String valor = linha.substring(separador + 1).trim();
			if (valor.isEmpty()) {
				throw new ConfiguracaoException("Linha " + numero + " do config.txt sem valor depois do '=' (\"" + linha + "\").");
			}

			if (CHAVE_PROCESSADO.equals(chave)) {
				processado = valor;
			} else if (CHAVE_NAO_PROCESSADO.equals(chave)) {
				naoProcessado = valor;
			} else if (CHAVE_PASTA.equals(chave)) {
				pasta = valor;
			} else if (CHAVE_ROTA_AUTOMATICA.equals(chave)) {
				rotaAutomatica = valor;
			} else {
				throw new ConfiguracaoException("Linha " + numero + " do config.txt com chave desconhecida: \""
						+ linha.substring(0, separador).trim() + "\".");
			}
		}

		// Teste 1.3 - arquivo em branco.
		if (linhasPreenchidas == 0) {
			throw new ConfiguracaoException("O arquivo config.txt está em branco.");
		}
		if (processado == null) {
			throw new ConfiguracaoException("A linha 1 do config.txt (Processado=...) não foi encontrada.");
		}
		// Teste 1.4 - sem a linha 2.
		if (naoProcessado == null) {
			throw new ConfiguracaoException("A linha 2 do config.txt (Não Processado=...) não foi encontrada.");
		}

		return new Configuracao(
				pasta == null ? base : caminho(pasta, "Pasta"),
				caminho(processado, "Processado"),
				caminho(naoProcessado, "Não Processado"),
				lerBooleano(rotaAutomatica),
				rotaAutomatica == null);
	}

	/**
	 * Tenta carregar a configuração; se houver qualquer problema, devolve os
	 * valores padrão (usado para preencher a tela de Configuração).
	 */
	public Configuracao carregarOuPadrao() {
		try {
			return carregar();
		} catch (ConfiguracaoException e) {
			return new Configuracao(base, base.resolve("Processado"), base.resolve("NaoProcessado"), false, true);
		}
	}

	/** Cria os diretórios da configuração (Pasta, Processado e NaoProcessado). */
	public void criarDiretorios(final Configuracao configuracao) throws ConfiguracaoException {
		try {
			Files.createDirectories(configuracao.getPasta());
			Files.createDirectories(configuracao.getSucesso());
			Files.createDirectories(configuracao.getErro());
		} catch (IOException e) {
			throw new ConfiguracaoException("Não foi possível criar os diretórios: " + e.getMessage(), e);
		}
	}

	/** Grava o config.txt (usado pela tela de Configuração) e cria os diretórios. */
	public void salvar(final Configuracao configuracao) throws ConfiguracaoException {
		try {
			Files.createDirectories(pastaConfiguracao);
			String conteudo = "Processado=" + configuracao.getSucesso() + "\r\n"
					+ "Não Processado=" + configuracao.getErro() + "\r\n"
					+ "Pasta=" + configuracao.getPasta() + "\r\n"
					+ "RotaAutomatica=" + (configuracao.isRotaAutomatica() ? "SIM" : "NAO") + "\r\n";
			Files.write(arquivoConfig, conteudo.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new ConfiguracaoException("Não foi possível gravar o arquivo " + arquivoConfig + ": " + e.getMessage(), e);
		}
		criarDiretorios(configuracao);
	}

	/** "Não Processado" -> "naoprocessado" (sem acento, sem espaço, minúsculo). */
	private static String normalizarChave(final String chave) {
		String semAcento = Normalizer.normalize(chave, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return semAcento.replaceAll("[^A-Za-z]", "").toLowerCase();
	}

	private static boolean lerBooleano(final String valor) throws ConfiguracaoException {
		if (valor == null) {
			return false;
		}
		String v = valor.trim().toUpperCase();
		if (v.equals("SIM") || v.equals("S") || v.equals("TRUE")) {
			return true;
		}
		if (v.equals("NAO") || v.equals("NÃO") || v.equals("N") || v.equals("FALSE")) {
			return false;
		}
		throw new ConfiguracaoException("Valor inválido para RotaAutomatica no config.txt: \"" + valor + "\" (use SIM ou NAO).");
	}

	private static Path caminho(final String valor, final String campo) throws ConfiguracaoException {
		try {
			return Paths.get(valor);
		} catch (InvalidPathException e) {
			throw new ConfiguracaoException("Caminho inválido em " + campo + ": \"" + valor + "\".", e);
		}
	}
}
