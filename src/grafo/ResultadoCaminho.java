package grafo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resultado da busca de menor caminho feita por {@link Grafo#caminhoMinimo(int, int)}.
 * Guarda a sequência de vértices do caminho (da origem ao destino) e o custo total.
 */
public class ResultadoCaminho {

	private final List<Integer> caminho;
	private final int custo;

	public ResultadoCaminho(final List<Integer> caminho, final int custo) {
		this.caminho = Collections.unmodifiableList(new ArrayList<Integer>(caminho));
		this.custo = custo;
	}

	/** Vértices do caminho, da origem até o destino. Vazia se não houver caminho. */
	public List<Integer> getCaminho() {
		return caminho;
	}

	/** Soma dos pesos das arestas do caminho. */
	public int getCusto() {
		return custo;
	}

	/** Indica se foi encontrado algum caminho. */
	public boolean isEncontrado() {
		return !caminho.isEmpty();
	}

	@Override
	public String toString() {
		return "Caminho: " + caminho + " | Custo: " + custo;
	}
}
