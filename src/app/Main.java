package app;

import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import config.Configuracao;
import config.ConfiguracaoException;
import config.GerenciadorConfiguracao;
import processamento.MonitorPasta;
import processamento.ResultadoProcessamento;
import ui.TelaPrincipal;

/**
 * Ponto de entrada.
 *
 * <ul>
 * <li>Sem argumentos: abre a aplicação com telas (Parte 2 do trabalho).</li>
 * <li>--console: roda só o processamento de arquivos, sem telas (Parte 1),
 * monitorando a pasta até o Ctrl+C.</li>
 * <li>--console --uma-vez: processa os arquivos que estão na pasta e encerra.</li>
 * </ul>
 */
public class Main {

	public static void main(final String[] args) {
		boolean console = contem(args, "--console");
		boolean umaVez = contem(args, "--uma-vez");

		if (console || umaVez || GraphicsEnvironment.isHeadless()) {
			int codigo = executarConsole(umaVez);
			if (codigo != 0 || umaVez) {
				System.exit(codigo);
			}
			return;
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception ignorado) {
					// fica com o visual padrão do Java
				}
				TelaPrincipal tela = new TelaPrincipal(new GerenciadorConfiguracao());
				tela.setVisible(true);
				tela.iniciar();
			}
		});
	}

	/** Parte 1: lê o config.txt, cria os diretórios e processa os rotaNN.txt. */
	private static int executarConsole(final boolean umaVez) {
		GerenciadorConfiguracao gerenciador = new GerenciadorConfiguracao();
		Configuracao configuracao;
		try {
			configuracao = gerenciador.carregar();
			gerenciador.criarDiretorios(configuracao);
		} catch (ConfiguracaoException e) {
			System.out.println("EXCEÇÃO: " + e.getMessage());
			return 1;
		}
		System.out.println("Configuração: " + configuracao);

		final MonitorPasta monitor = new MonitorPasta(configuracao, new MonitorPasta.Ouvinte() {
			@Override
			public void registrar(final String mensagem) {
				System.out.println(mensagem);
			}
		});

		if (umaVez) {
			List<ResultadoProcessamento> resultados = monitor.processarUmaVez();
			int ok = 0;
			for (ResultadoProcessamento r : resultados) {
				if (r.isSucesso()) {
					ok++;
				}
			}
			System.out.println("Fim: " + resultados.size() + " arquivo(s), " + ok + " processado(s) e "
					+ (resultados.size() - ok) + " não processado(s).");
			return 0;
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				monitor.parar();
			}
		});
		monitor.iniciar();
		System.out.println("Pressione Ctrl+C para PARAR.");
		try {
			new CountDownLatch(1).await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return 0;
	}

	private static boolean contem(final String[] args, final String opcao) {
		for (String a : args) {
			if (a.equalsIgnoreCase(opcao)) {
				return true;
			}
		}
		return false;
	}
}
