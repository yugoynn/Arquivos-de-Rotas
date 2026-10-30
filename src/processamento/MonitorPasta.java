package processamento;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import arquivo.EscritorArquivoRota;
import config.Configuracao;

/**
 * "Rota automática": vigia a pasta raiz (C:\Teste) em segundo plano.
 *
 * A cada {@value #INTERVALO_SEGUNDOS} segundos procura arquivos rotaNN.txt e
 * entrega cada um para uma thread diferente (pool de threads), então vários
 * arquivos são tratados AO MESMO TEMPO. É o laço "Aguarda o arquivo → Processa →
 * Sucesso? → Move" do fluxograma; o "PARAR" é o método {@link #parar()}.
 */
public class MonitorPasta {

	/** Recebe as mensagens de log (a tela principal ou o console). */
	public interface Ouvinte {
		void registrar(String mensagem);
	}

	public static final int INTERVALO_SEGUNDOS = 10;

	/** Arquivos alterados há menos que isso ainda podem estar sendo copiados. */
	private static final long ESPERA_COPIA_MS = 2000;

	private static final int THREADS = 4;

	private final Configuracao configuracao;
	private final Ouvinte ouvinte;
	private final ProcessadorArquivo processador = new ProcessadorArquivo();
	private final Set<Path> emAndamento = Collections.newSetFromMap(new ConcurrentHashMap<Path, Boolean>());

	private ScheduledExecutorService agendador;
	private ExecutorService trabalhadores;

	public MonitorPasta(final Configuracao configuracao, final Ouvinte ouvinte) {
		this.configuracao = configuracao;
		this.ouvinte = ouvinte;
	}

	/** Começa a monitorar em segundo plano. */
	public synchronized void iniciar() {
		if (isAtivo()) {
			return;
		}
		trabalhadores = Executors.newFixedThreadPool(THREADS);
		agendador = Executors.newSingleThreadScheduledExecutor();
		agendador.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					varrer(trabalhadores, false);
				} catch (Throwable t) {
					log("Erro ao verificar a pasta: " + t);
				}
			}
		}, 0, INTERVALO_SEGUNDOS, TimeUnit.SECONDS);
		log("Rota automática INICIADA. Monitorando " + configuracao.getPasta() + " a cada " + INTERVALO_SEGUNDOS + " segundos.");
	}

	/** Para o monitoramento (espera os arquivos que já estavam sendo processados terminarem). */
	public synchronized void parar() {
		if (!isAtivo()) {
			return;
		}
		agendador.shutdownNow();
		trabalhadores.shutdown();
		try {
			trabalhadores.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		agendador = null;
		trabalhadores = null;
		log("Rota automática PARADA.");
	}

	public synchronized boolean isAtivo() {
		return agendador != null && !agendador.isShutdown();
	}

	/**
	 * Processa todos os arquivos que estão na pasta agora (em paralelo) e espera
	 * terminar. Usado pelo modo console "--uma-vez".
	 */
	public List<ResultadoProcessamento> processarUmaVez() {
		ExecutorService pool = Executors.newFixedThreadPool(THREADS);
		List<ResultadoProcessamento> resultados = new ArrayList<ResultadoProcessamento>();
		try {
			for (Future<ResultadoProcessamento> futuro : varrer(pool, true)) {
				try {
					resultados.add(futuro.get());
				} catch (ExecutionException e) {
					log("Erro: " + e.getCause());
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			pool.shutdown();
		}
		return resultados;
	}

	/** Procura os rotaNN.txt na pasta e manda cada um para uma thread. */
	private List<Future<ResultadoProcessamento>> varrer(final ExecutorService pool, final boolean ignorarEsperaCopia) {
		List<Future<ResultadoProcessamento>> futuros = new ArrayList<Future<ResultadoProcessamento>>();
		Path pasta = configuracao.getPasta();
		if (!Files.isDirectory(pasta)) {
			log("Pasta " + pasta + " não encontrada.");
			return futuros;
		}

		List<Path> encontrados = new ArrayList<Path>();
		try (DirectoryStream<Path> arquivos = Files.newDirectoryStream(pasta)) {
			for (Path arquivo : arquivos) {
				if (Files.isRegularFile(arquivo)
						&& EscritorArquivoRota.NOME_ARQUIVO_ROTA.matcher(arquivo.getFileName().toString()).matches()) {
					encontrados.add(arquivo);
				}
			}
		} catch (IOException e) {
			log("Não foi possível listar a pasta " + pasta + ": " + e.getMessage());
			return futuros;
		}
		Collections.sort(encontrados);

		for (final Path arquivo : encontrados) {
			if (!ignorarEsperaCopia && aindaSendoCopiado(arquivo)) {
				continue; // pega na próxima verificação
			}
			if (!emAndamento.add(arquivo)) {
				continue; // outra thread já está cuidando dele
			}
			futuros.add(pool.submit(new Callable<ResultadoProcessamento>() {
				@Override
				public ResultadoProcessamento call() {
					try {
						log("Processando " + arquivo.getFileName() + " (" + Thread.currentThread().getName() + ")...");
						ResultadoProcessamento resultado = processador.processar(arquivo, configuracao);
						log(resultado.toString());
						return resultado;
					} finally {
						emAndamento.remove(arquivo);
					}
				}
			}));
		}
		return futuros;
	}

	private static boolean aindaSendoCopiado(final Path arquivo) {
		try {
			long modificado = Files.getLastModifiedTime(arquivo).toMillis();
			return System.currentTimeMillis() - modificado < ESPERA_COPIA_MS;
		} catch (IOException e) {
			return true;
		}
	}

	private void log(final String mensagem) {
		String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());
		ouvinte.registrar("[" + hora + "] " + mensagem);
	}
}
