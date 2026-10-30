package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import config.Configuracao;
import config.ConfiguracaoException;
import config.GerenciadorConfiguracao;
import processamento.MonitorPasta;

/**
 * Janela principal com o menu de opções: Configurações, Visível e Sair.
 *
 * Segue o fluxograma do enunciado ao abrir:
 * <ul>
 * <li>1ª vez (ou config.txt com problema) → abre a tela de Configuração;</li>
 * <li>Rota automática marcada → começa a monitorar a pasta em segundo plano
 * (o log aparece nesta janela, e o botão "Parar" é o PARAR do fluxograma);</li>
 * <li>Rota automática desmarcada → abre a tela manual (Visível).</li>
 * </ul>
 */
public class TelaPrincipal extends JFrame {

	private static final long serialVersionUID = 1L;

	private final GerenciadorConfiguracao gerenciador;
	private Configuracao configuracao;
	private MonitorPasta monitor;
	private TelaVisivel telaVisivel;

	private final JLabel lblStatus = new JLabel();
	private final JButton btnIniciar = new JButton("Iniciar rota automática");
	private final JButton btnParar = new JButton("Parar");
	private final JTextArea txtLog = new JTextArea();

	public TelaPrincipal(final GerenciadorConfiguracao gerenciador) {
		super("Arquivos de Rota - Dijkstra");
		this.gerenciador = gerenciador;
		montarMenu();
		montarTela();
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				sair();
			}
		});
		setSize(820, 460);
		setLocationRelativeTo(null);
	}

	private void montarMenu() {
		JMenuBar barra = new JMenuBar();
		JMenu menu = new JMenu("Opções");
		menu.setMnemonic(KeyEvent.VK_O);

		JMenuItem itemConfig = new JMenuItem("Configurações");
		itemConfig.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				abrirConfiguracao();
			}
		});
		JMenuItem itemVisivel = new JMenuItem("Visível");
		itemVisivel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				abrirVisivel();
			}
		});
		JMenuItem itemSair = new JMenuItem("Sair");
		itemSair.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				sair();
			}
		});

		menu.add(itemConfig);
		menu.add(itemVisivel);
		menu.addSeparator();
		menu.add(itemSair);
		barra.add(menu);
		setJMenuBar(barra);
	}

	private void montarTela() {
		JPanel topo = new JPanel(new BorderLayout());
		topo.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
		lblStatus.setFont(lblStatus.getFont().deriveFont(Font.BOLD));
		topo.add(lblStatus, BorderLayout.CENTER);

		JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		botoes.add(btnIniciar);
		botoes.add(btnParar);
		topo.add(botoes, BorderLayout.EAST);

		btnIniciar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				iniciarMonitor();
			}
		});
		btnParar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				pararMonitor();
			}
		});

		txtLog.setEditable(false);
		txtLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		txtLog.setLineWrap(true);
		txtLog.setWrapStyleWord(true);
		JScrollPane rolagem = new JScrollPane(txtLog);
		rolagem.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8),
				BorderFactory.createTitledBorder("Log do processamento")));

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(topo, BorderLayout.NORTH);
		getContentPane().add(rolagem, BorderLayout.CENTER);
		atualizarStatus();
	}

	/** Decide o que fazer ao abrir a aplicação (fluxograma). */
	public void iniciar() {
		registrar("Aplicação iniciada. Arquivo de configuração: " + gerenciador.getArquivoConfig());
		try {
			configuracao = gerenciador.carregar();
			gerenciador.criarDiretorios(configuracao);
			registrar("Configuração carregada: " + configuracao);
		} catch (ConfiguracaoException e) {
			registrar("EXCEÇÃO na configuração: " + e.getMessage());
			JOptionPane.showMessageDialog(this, e.getMessage() + "\n\nA tela de Configuração será aberta para corrigir.",
					"Problema na configuração", JOptionPane.ERROR_MESSAGE);
			abrirConfiguracao();
			return;
		}

		if (configuracao.isPrimeiraVez()) {
			registrar("Primeira utilização: abrindo a tela de Configuração.");
			abrirConfiguracao();
		} else if (configuracao.isRotaAutomatica()) {
			iniciarMonitor();
		} else {
			abrirVisivel();
		}
	}

	private void abrirConfiguracao() {
		TelaConfiguracao tela = new TelaConfiguracao(this, gerenciador);
		tela.setVisible(true); // modal: espera fechar
		Configuracao nova = tela.getConfiguracaoSalva();
		if (nova == null) {
			if (configuracao == null) {
				registrar("Configuração não foi salva. Use Opções > Configurações para configurar o sistema.");
			}
			return;
		}

		pararMonitor(); // as pastas podem ter mudado: reinicia com a configuração nova
		configuracao = nova;
		registrar("Configuração salva em " + gerenciador.getArquivoConfig() + ": " + configuracao);
		if (configuracao.isRotaAutomatica()) {
			iniciarMonitor();
		}
		atualizarStatus();
	}

	private void abrirVisivel() {
		if (telaVisivel == null || !telaVisivel.isDisplayable()) {
			telaVisivel = new TelaVisivel(gerenciador);
		}
		telaVisivel.setVisible(true);
		telaVisivel.toFront();
	}

	private void iniciarMonitor() {
		if (configuracao == null) {
			JOptionPane.showMessageDialog(this, "Configure o sistema antes (Opções > Configurações).", "Rota automática",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (monitor == null || !monitor.isAtivo()) {
			monitor = new MonitorPasta(configuracao, new MonitorPasta.Ouvinte() {
				@Override
				public void registrar(final String mensagem) {
					TelaPrincipal.this.registrar(mensagem);
				}
			});
			monitor.iniciar();
		}
		atualizarStatus();
	}

	private void pararMonitor() {
		if (monitor != null && monitor.isAtivo()) {
			monitor.parar();
		}
		atualizarStatus();
	}

	private void atualizarStatus() {
		boolean ativo = monitor != null && monitor.isAtivo();
		if (configuracao == null) {
			lblStatus.setText("Sistema não configurado");
		} else if (ativo) {
			lblStatus.setText("Rota automática ATIVA - monitorando " + configuracao.getPasta());
		} else {
			lblStatus.setText("Rota automática parada");
		}
		btnIniciar.setEnabled(!ativo);
		btnParar.setEnabled(ativo);
	}

	/** Escreve no log da tela (pode ser chamado de qualquer thread). */
	public void registrar(final String mensagem) {
		System.out.println(mensagem);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				txtLog.append(mensagem + "\n");
				txtLog.setCaretPosition(txtLog.getDocument().getLength());
			}
		});
	}

	private void sair() {
		pararMonitor();
		dispose();
		System.exit(0);
	}
}
