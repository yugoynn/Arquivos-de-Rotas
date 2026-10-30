package grafo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resultado do Dijkstra já "traduzido" para os códigos dos nós (letras),
 * com os trechos percorridos e a distância total.
 */
public class MenorCaminho {

	private final String origem;
	private final String destino;
	private final List<String> nos;
	private final List<Aresta> trechos;
	private final int distancia;

	public MenorCaminho(final String origem, final String destino, final List<String> nos, final List<Aresta> trechos,
			final int distancia) {
		this.origem = origem;
		this.destino = destino;
		this.nos = Collections.unmodifiableList(new ArrayList<String>(nos));
		this.trechos = Collections.unmodifiableList(new ArrayList<Aresta>(trechos));
		this.distancia = distancia;
	}

	public String getOrigem() {
		return origem;
	}

	public String getDestino() {
		return destino;
	}

	/** Nós do caminho, da origem ao destino. Vazio se não existe caminho. */
	public List<String> getNos() {
		return nos;
	}

	/** Cada trecho (aresta) do caminho, com o seu peso. */
	public List<Aresta> getTrechos() {
		return trechos;
	}

	public int getDistancia() {
		return distancia;
	}

	public boolean isEncontrado() {
		return !nos.isEmpty();
	}

	/** Ex.: formatar(" -> ") devolve "A -> E -> F -> G -> H". */
	public String formatar(final String separador) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < nos.size(); i++) {
			if (i > 0) {
				sb.append(separador);
			}
			sb.append(nos.get(i));
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		if (!isEncontrado()) {
			return "Não existe caminho entre " + origem + " e " + destino;
		}
		return formatar(" -> ") + " (distância " + distancia + ")";
	}
}
