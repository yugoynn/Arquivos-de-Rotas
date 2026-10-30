package grafo;

/**
 * Uma ligação entre dois nós com o seu peso (distância em KM).
 * Corresponde a uma linha "02NO;ND=P" do arquivo ou a uma linha da tabela da tela Visível.
 */
public class Aresta {

	private final String origem;
	private final String destino;
	private final int peso;

	public Aresta(final String origem, final String destino, final int peso) {
		this.origem = origem;
		this.destino = destino;
		this.peso = peso;
	}

	public String getOrigem() {
		return origem;
	}

	public String getDestino() {
		return destino;
	}

	public int getPeso() {
		return peso;
	}

	/** Verdadeiro se esta aresta liga os nós a e b, em qualquer sentido. */
	public boolean liga(final String a, final String b) {
		return (origem.equals(a) && destino.equals(b)) || (origem.equals(b) && destino.equals(a));
	}

	@Override
	public String toString() {
		return origem + ";" + destino + "=" + peso;
	}
}
