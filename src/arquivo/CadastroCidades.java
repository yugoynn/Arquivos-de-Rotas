package arquivo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import util.LeitorTexto;

/**
 * Guarda o nome da cidade de cada código (A=Criciúma, B=Içara...).
 *
 * O formato rotaNN.txt só tem os códigos dos nós, então os nomes digitados na
 * tela Visível ficam no arquivo Configuracao\cidades.txt. Assim, ao abrir um
 * rotaNN.txt pelo botão Buscar, a tabela já aparece com os nomes das cidades.
 */
public class CadastroCidades {

	private final Path arquivo;
	private final Map<String, String> cidades = new TreeMap<String, String>();

	public CadastroCidades(final Path arquivo) {
		this.arquivo = arquivo;
		carregar();
	}

	private void carregar() {
		if (arquivo == null || !Files.isRegularFile(arquivo)) {
			return;
		}
		try {
			List<String> linhas = LeitorTexto.lerLinhas(arquivo);
			for (String linha : linhas) {
				int separador = linha.indexOf('=');
				if (separador > 0) {
					String codigo = linha.substring(0, separador).trim().toUpperCase();
					String nome = linha.substring(separador + 1).trim();
					if (!codigo.isEmpty() && !nome.isEmpty()) {
						cidades.put(codigo, nome);
					}
				}
			}
		} catch (IOException ignorado) {
			// Sem o cadastro a tela continua funcionando, só não mostra os nomes.
		}
	}

	/** Nome da cidade do código, ou "" se não for conhecido. */
	public String nome(final String codigo) {
		String nome = cidades.get(codigo);
		return nome == null ? "" : nome;
	}

	public void definir(final String codigo, final String nome) {
		if (nome == null || nome.trim().isEmpty()) {
			return;
		}
		cidades.put(codigo, nome.trim());
	}

	/** Grava o cadastro em disco (Configuracao\cidades.txt). */
	public void salvar() throws IOException {
		if (arquivo == null) {
			return;
		}
		Files.createDirectories(arquivo.getParent());
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> e : cidades.entrySet()) {
			sb.append(e.getKey()).append('=').append(e.getValue()).append("\r\n");
		}
		Files.write(arquivo, sb.toString().getBytes(StandardCharsets.UTF_8));
	}
}
