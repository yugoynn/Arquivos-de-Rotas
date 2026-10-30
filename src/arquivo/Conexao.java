package arquivo;

/** Uma linha do RESUMO DE CONEXÕES: "01NO=ND". */
public class Conexao {

	private final String origem;
	private final String destino;

	public Conexao(final String origem, final String destino) {
		this.origem = origem;
		this.destino = destino;
	}

	public String getOrigem() {
		return origem;
	}

	public String getDestino() {
		return destino;
	}

	/** Verdadeiro se esta conexão liga os nós a e b, em qualquer sentido. */
	public boolean liga(final String a, final String b) {
		return (origem.equals(a) && destino.equals(b)) || (origem.equals(b) && destino.equals(a));
	}

	@Override
	public String toString() {
		return origem + "=" + destino;
	}
}
