package grafo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ponte entre o "mundo das letras" (A, B, C... do arquivo e da tela) e o
 * {@link Grafo} do professor, que só trabalha com índices numéricos (0, 1, 2...).
 *
 * Ela:
 * <ol>
 * <li>dá um índice para cada nó, na ordem em que aparece (A=0, B=1, ...);</li>
 * <li>monta o Grafo chamando criaArestar para cada aresta;</li>
 * <li>chama caminhoMinimo (Dijkstra do professor) e converte os índices de volta para letras.</li>
 * </ol>
 *
 * Observação: antes de chamar o Dijkstra verificamos se o destino é alcançável.
 * O código do professor soma "infinito" (Integer.MAX_VALUE) com o peso quando o
 * destino não tem ligação com a origem, o que estoura o int; com essa verificação
 * isso nunca acontece e o código do professor fica intacto.
 */
public class MapaRotas {

	private final List<String> nos = new ArrayList<String>();
	private final Map<String, Integer> indices = new HashMap<String, Integer>();
	private final Grafo grafo;

	public MapaRotas(final List<Aresta> arestas) {
		if (arestas == null || arestas.isEmpty()) {
			throw new IllegalArgumentException("Nenhuma aresta informada para montar o grafo.");
		}
		for (Aresta aresta : arestas) {
			registrar(aresta.getOrigem());
			registrar(aresta.getDestino());
		}

		grafo = new Grafo(nos.size());
		for (Aresta aresta : arestas) {
			grafo.criaArestar(indices.get(aresta.getOrigem()), indices.get(aresta.getDestino()), aresta.getPeso());
		}
	}

	private void registrar(final String no) {
		if (!indices.containsKey(no)) {
			indices.put(no, nos.size());
			nos.add(no);
		}
	}

	/** Todos os nós, na ordem em que apareceram. */
	public List<String> getNos() {
		return Collections.unmodifiableList(nos);
	}

	public boolean contem(final String no) {
		return indices.containsKey(no);
	}

	/** Calcula o menor caminho entre dois nós usando o Dijkstra do professor. */
	public MenorCaminho calcular(final String origem, final String destino) {
		Integer indiceOrigem = indices.get(origem);
		Integer indiceDestino = indices.get(destino);
		if (indiceOrigem == null) {
			throw new IllegalArgumentException("O nó de origem " + origem + " não existe no grafo.");
		}
		if (indiceDestino == null) {
			throw new IllegalArgumentException("O nó de destino " + destino + " não existe no grafo.");
		}

		if (!alcancavel(indiceOrigem, indiceDestino)) {
			return new MenorCaminho(origem, destino, Collections.<String>emptyList(), Collections.<Aresta>emptyList(), 0);
		}

		ResultadoCaminho resultado = grafo.caminhoMinimo(indiceOrigem, indiceDestino);

		List<String> caminho = new ArrayList<String>();
		List<Aresta> trechos = new ArrayList<Aresta>();
		List<Integer> indicesCaminho = resultado.getCaminho();
		for (int i = 0; i < indicesCaminho.size(); i++) {
			caminho.add(nos.get(indicesCaminho.get(i)));
			if (i > 0) {
				int de = indicesCaminho.get(i - 1);
				int para = indicesCaminho.get(i);
				trechos.add(new Aresta(nos.get(de), nos.get(para), grafo.getCusto(de, para)));
			}
		}
		return new MenorCaminho(origem, destino, caminho, trechos, resultado.getCusto());
	}

	/** Busca em largura usando os vizinhos do próprio Grafo do professor. */
	private boolean alcancavel(final int origem, final int destino) {
		boolean[] visitado = new boolean[nos.size()];
		Deque<Integer> fila = new ArrayDeque<Integer>();
		fila.add(origem);
		visitado[origem] = true;
		while (!fila.isEmpty()) {
			int atual = fila.poll();
			if (atual == destino) {
				return true;
			}
			for (Integer vizinho : grafo.getVizinho(atual)) {
				if (!visitado[vizinho]) {
					visitado[vizinho] = true;
					fila.add(vizinho);
				}
			}
		}
		return false;
	}
}
