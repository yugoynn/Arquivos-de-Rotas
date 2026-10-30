package arquivo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import grafo.Aresta;

/**
 * Conteúdo de um arquivo rotaNN.txt já lido e validado.
 *
 * <pre>
 * 00NNSP              HEADER   (NN = total de nós, SP = soma dos pesos)
 * 01NO=ND             RESUMO DE CONEXÕES (1 ou N linhas)
 * 02NO;ND=P           RESUMO DOS PESOS   (1 ou N linhas)
 * 09RC=NN;RP=NN;P     TRAILER  (RC = nº de linhas 01, RP = nº de linhas 02, P = soma dos pesos)
 * </pre>
 */
public class ArquivoRota {

	private final int totalNosHeader;
	private final int somaPesosHeader;
	private final List<Conexao> conexoes;
	private final List<Aresta> pesos;
	private final int linhasConexaoTrailer;
	private final int linhasPesoTrailer;
	private final int somaPesosTrailer;

	public ArquivoRota(final int totalNosHeader, final int somaPesosHeader, final List<Conexao> conexoes,
			final List<Aresta> pesos, final int linhasConexaoTrailer, final int linhasPesoTrailer,
			final int somaPesosTrailer) {
		this.totalNosHeader = totalNosHeader;
		this.somaPesosHeader = somaPesosHeader;
		this.conexoes = Collections.unmodifiableList(new ArrayList<Conexao>(conexoes));
		this.pesos = Collections.unmodifiableList(new ArrayList<Aresta>(pesos));
		this.linhasConexaoTrailer = linhasConexaoTrailer;
		this.linhasPesoTrailer = linhasPesoTrailer;
		this.somaPesosTrailer = somaPesosTrailer;
	}

	public int getTotalNosHeader() {
		return totalNosHeader;
	}

	public int getSomaPesosHeader() {
		return somaPesosHeader;
	}

	public List<Conexao> getConexoes() {
		return conexoes;
	}

	/** As linhas 02, já como arestas com peso (prontas para o grafo). */
	public List<Aresta> getArestas() {
		return pesos;
	}

	public int getLinhasConexaoTrailer() {
		return linhasConexaoTrailer;
	}

	public int getLinhasPesoTrailer() {
		return linhasPesoTrailer;
	}

	public int getSomaPesosTrailer() {
		return somaPesosTrailer;
	}

	/** Nós distintos, na ordem em que aparecem no arquivo. */
	public Set<String> getNos() {
		Set<String> nos = new LinkedHashSet<String>();
		for (Conexao c : conexoes) {
			nos.add(c.getOrigem());
			nos.add(c.getDestino());
		}
		for (Aresta a : pesos) {
			nos.add(a.getOrigem());
			nos.add(a.getDestino());
		}
		return nos;
	}
}
