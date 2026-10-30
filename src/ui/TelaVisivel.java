package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import arquivo.ArquivoRota;
import arquivo.ArquivoRotaInvalidoException;
import arquivo.CadastroCidades;
import arquivo.EscritorArquivoRota;
import arquivo.LeitorArquivoRota;
import config.Configuracao;
import config.GerenciadorConfiguracao;
import grafo.Aresta;
import grafo.MapaRotas;
import grafo.MenorCaminho;

/**
 * Tela "Visível" (Dijkstra - Busca por melhor caminho).
 *
 * <ul>
 * <li><b>Buscar</b>: digite o caminho de um rotaNN.txt ou deixe em branco e escolha o arquivo;
 * as rotas do arquivo vão para a tabela;</li>
 * <li><b>Código / Cidade / KM + "+"</b>: adiciona uma rota manualmente na tabela;</li>
 * <li><b>SALVAR</b>: gera um rotaNN.txt com as rotas da tabela;</li>
 * <li><b>PROCESSAR</b>: pergunta origem e destino e roda o Dijkstra do professor.</li>
 * </ul>
 */
public class TelaVisivel extends JFrame {

	private static final long serialVersionUID = 1L;

	private static final int COL_COD_ORIGEM = 0;
	private static final int COL_CID_ORIGEM = 1;
	private static final int COL_COD_DESTINO = 2;
	private static final int COL_CID_DESTINO = 3;
	private static final int COL_DISTANCIA = 4;

	private final GerenciadorConfiguracao gerenciador;
	private final CadastroCidades cidades;

	private final JTextField txtBuscar = new JTextField(30);
	private final JButton btnBuscar = new JButton("Buscar");
	private final JTextField txtCodOrigem = new JTextField(4);
	private final JTextField txtCidOrigem = new JTextField(14);
	private final JTextField txtCodDestino = new JTextField(4);
	private final JTextField txtCidDestino = new JTextField(14);
	private final JTextField txtKm = new JTextField(4);
	private final JButton btnAdicionar = new JButton("+");
	private final JButton btnSalvar = new JButton("SALVAR");
	private final JButton btnProcessar = new JButton("PROCESSAR");

	private final DefaultTableModel modelo = new DefaultTableModel(
			new Object[] { "Código Origem", "Cidade Origem", "Código Destino", "Cidade Destino", "Distância" }, 0) {
		private static final long serialVersionUID = 1L;

		@Override
		public boolean isCellEditable(final int linha, final int coluna) {
			return coluna == COL_CID_ORIGEM || coluna == COL_CID_DESTINO; // só o nome da cidade é editável
		}

		@Override
		public Class<?> getColumnClass(final int coluna) {
			return coluna == COL_DISTANCIA ? Integer.class : String.class;
		}
	};
	private final JTable tabela = new JTable(modelo);
	private boolean atualizandoCidades;

	public TelaVisivel(final GerenciadorConfiguracao gerenciador) {
		super("Dijkstra - Busca por melhor caminho");
		this.gerenciador = gerenciador;
		this.cidades = new CadastroCidades(gerenciador.getPastaConfiguracao().resolve("cidades.txt"));
		montarTela();
		configurarEventos();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setSize(760, 540);
		setLocationRelativeTo(null);
	}

	// =====================================================================
	// Montagem da tela
	// =====================================================================

	private void montarTela() {
		JPanel formulario = new JPanel(new GridBagLayout());
		formulario.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));

		adicionar(formulario, negrito("Buscar"), 0, 0, 1, false);
		adicionar(formulario, txtBuscar, 1, 0, 3, true);
		adicionar(formulario, btnBuscar, 4, 0, 1, false);

		adicionar(formulario, negrito("Código:"), 0, 1, 1, false);
		adicionar(formulario, txtCodOrigem, 1, 1, 1, false);
		adicionar(formulario, negrito("Cidade:"), 2, 1, 1, false);
		adicionar(formulario, txtCidOrigem, 3, 1, 1, true);
		adicionar(formulario, negrito("(ORIGEM)"), 4, 1, 1, false);

		adicionar(formulario, negrito("Código:"), 0, 2, 1, false);
		adicionar(formulario, txtCodDestino, 1, 2, 1, false);
		adicionar(formulario, negrito("Cidade:"), 2, 2, 1, false);
		adicionar(formulario, txtCidDestino, 3, 2, 1, true);
		adicionar(formulario, negrito("(DESTINO)"), 4, 2, 1, false);

		adicionar(formulario, negrito("KM:"), 0, 3, 1, false);
		adicionar(formulario, txtKm, 1, 3, 1, false);
		GridBagConstraints c = restricao(5, 3, 1, false);
		c.anchor = GridBagConstraints.EAST;
		c.weightx = 1;
		formulario.add(btnAdicionar, c);

		txtBuscar.setToolTipText("Caminho de um arquivo rotaNN.txt (ou deixe vazio e clique em Buscar para escolher)");
		txtCodOrigem.setToolTipText("Código do vértice (1 ou 2 letras)");
		txtCodDestino.setToolTipText("Código do vértice (1 ou 2 letras)");
		txtKm.setToolTipText("Distância entre as cidades (peso da aresta), de 1 a 9999");
		btnAdicionar.setToolTipText("Adicionar a rota na tabela");

		tabela.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		tabela.getTableHeader().setReorderingAllowed(false);
		tabela.setRowHeight(22);
		JScrollPane rolagem = new JScrollPane(tabela);
		rolagem.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10),
				rolagem.getBorder()));

		JPanel rodape = new JPanel(new BorderLayout());
		rodape.setBorder(BorderFactory.createEmptyBorder(4, 10, 10, 10));
		JLabel dica = new JLabel("Delete remove a linha selecionada. Clique duplo na cidade para editar o nome.");
		dica.setFont(dica.getFont().deriveFont(Font.ITALIC, 11f));
		rodape.add(dica, BorderLayout.WEST);
		JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		btnSalvar.setFont(btnSalvar.getFont().deriveFont(Font.BOLD));
		btnProcessar.setFont(btnProcessar.getFont().deriveFont(Font.BOLD));
		botoes.add(btnSalvar);
		botoes.add(btnProcessar);
		rodape.add(botoes, BorderLayout.EAST);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(formulario, BorderLayout.NORTH);
		getContentPane().add(rolagem, BorderLayout.CENTER);
		getContentPane().add(rodape, BorderLayout.SOUTH);
	}

	private static JLabel negrito(final String texto) {
		JLabel label = new JLabel(texto);
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		return label;
	}

	private static GridBagConstraints restricao(final int x, final int y, final int largura, final boolean esticar) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.gridwidth = largura;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(3, 3, 3, 6);
		if (esticar) {
			c.fill = GridBagConstraints.HORIZONTAL;
		}
		return c;
	}

	private static void adicionar(final JPanel painel, final JComponent componente, final int x, final int y,
			final int largura, final boolean esticar) {
		painel.add(componente, restricao(x, y, largura, esticar));
	}

	private void configurarEventos() {
		btnBuscar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				buscar();
			}
		});
		txtBuscar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				buscar();
			}
		});
		ActionListener adicionar = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				adicionarRota();
			}
		};
		btnAdicionar.addActionListener(adicionar);
		txtKm.addActionListener(adicionar); // Enter no KM também adiciona
		btnSalvar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				salvar();
			}
		});
		btnProcessar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				processar();
			}
		});

		// Ao sair do campo Código, preenche a cidade se o código já for conhecido.
		txtCodOrigem.addFocusListener(new PreencheCidade(txtCodOrigem, txtCidOrigem));
		txtCodDestino.addFocusListener(new PreencheCidade(txtCodDestino, txtCidDestino));

		// Delete remove linhas; botão direito também.
		AbstractAction remover = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				removerSelecionadas();
			}
		};
		tabela.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "remover");
		tabela.getActionMap().put("remover", remover);
		JPopupMenu popup = new JPopupMenu();
		JMenuItem itemRemover = new JMenuItem("Remover linha(s)");
		itemRemover.addActionListener(remover);
		popup.add(itemRemover);
		tabela.setComponentPopupMenu(popup);

		// Editou o nome de uma cidade na tabela: replica para as outras linhas e guarda no cadastro.
		modelo.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(final TableModelEvent e) {
				if (atualizandoCidades || e.getType() != TableModelEvent.UPDATE || e.getFirstRow() < 0) {
					return;
				}
				int coluna = e.getColumn();
				if (coluna == COL_CID_ORIGEM || coluna == COL_CID_DESTINO) {
					int linha = e.getFirstRow();
					String codigo = (String) modelo.getValueAt(linha, coluna == COL_CID_ORIGEM ? COL_COD_ORIGEM : COL_COD_DESTINO);
					String nome = String.valueOf(modelo.getValueAt(linha, coluna)).trim();
					cidades.definir(codigo, nome);
					atualizarNomes(codigo, nome);
					gravarCidades();
				}
			}
		});
	}

	private class PreencheCidade extends FocusAdapter {
		private final JTextField codigo;
		private final JTextField cidade;

		PreencheCidade(final JTextField codigo, final JTextField cidade) {
			this.codigo = codigo;
			this.cidade = cidade;
		}

		@Override
		public void focusLost(final FocusEvent e) {
			String cod = codigo.getText().trim().toUpperCase();
			codigo.setText(cod);
			if (cidade.getText().trim().isEmpty() && !cod.isEmpty()) {
				cidade.setText(cidades.nome(cod));
			}
		}
	}

	// =====================================================================
	// Buscar: carrega um rotaNN.txt na tabela
	// =====================================================================

	private void buscar() {
		String texto = txtBuscar.getText().trim();
		Path arquivo = null;
		if (!texto.isEmpty()) {
			try {
				arquivo = Paths.get(texto);
			} catch (InvalidPathException e) {
				erro("Caminho inválido: " + texto);
				return;
			}
		}

		if (arquivo == null || !Files.isRegularFile(arquivo)) {
			if (arquivo != null) {
				erro("Arquivo não encontrado: " + arquivo + "\nEscolha o arquivo na janela a seguir.");
			}
			Configuracao cfg = gerenciador.carregarOuPadrao();
			JFileChooser seletor = new JFileChooser(diretorioExistente(cfg.getPasta(), cfg.getSucesso()));
			seletor.setDialogTitle("Selecionar arquivo de rota");
			seletor.setFileFilter(new FileNameExtensionFilter("Arquivos de rota (*.txt)", "txt"));
			if (seletor.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
				return;
			}
			arquivo = seletor.getSelectedFile().toPath();
			txtBuscar.setText(arquivo.toString());
		}

		ArquivoRota rota;
		try {
			rota = new LeitorArquivoRota().ler(arquivo);
		} catch (ArquivoRotaInvalidoException e) {
			erro("Arquivo inválido:\n" + e.getMessage());
			return;
		} catch (IOException e) {
			erro("Não foi possível ler o arquivo:\n" + e.getMessage());
			return;
		}

		if (modelo.getRowCount() > 0) {
			int resposta = JOptionPane.showConfirmDialog(this,
					"A tabela já tem rotas. Substituir pelas rotas do arquivo?", "Buscar", JOptionPane.YES_NO_OPTION);
			if (resposta != JOptionPane.YES_OPTION) {
				return;
			}
		}
		modelo.setRowCount(0);
		for (Aresta a : rota.getArestas()) {
			modelo.addRow(new Object[] { a.getOrigem(), cidades.nome(a.getOrigem()), a.getDestino(),
					cidades.nome(a.getDestino()), a.getPeso() });
		}
		JOptionPane.showMessageDialog(this, "Arquivo carregado: " + rota.getArestas().size() + " rotas e "
				+ rota.getTotalNosHeader() + " cidades (nós).", "Buscar", JOptionPane.INFORMATION_MESSAGE);
	}

	// =====================================================================
	// "+": adiciona uma rota manualmente
	// =====================================================================

	private void adicionarRota() {
		String codOrigem = txtCodOrigem.getText().trim().toUpperCase();
		String codDestino = txtCodDestino.getText().trim().toUpperCase();
		String cidOrigem = txtCidOrigem.getText().trim();
		String cidDestino = txtCidDestino.getText().trim();

		if (!codOrigem.matches("[A-Z]{1,2}")) {
			erroCampo("O código de ORIGEM deve ter 1 ou 2 letras (ex.: A, B, AB).", txtCodOrigem);
			return;
		}
		if (!codDestino.matches("[A-Z]{1,2}")) {
			erroCampo("O código de DESTINO deve ter 1 ou 2 letras (ex.: A, B, AB).", txtCodDestino);
			return;
		}
		if (codOrigem.equals(codDestino)) {
			erroCampo("Origem e destino devem ser diferentes.", txtCodDestino);
			return;
		}
		int km;
		try {
			km = Integer.parseInt(txtKm.getText().trim());
		} catch (NumberFormatException e) {
			km = -1;
		}
		if (km < 1 || km > 9999) {
			erroCampo("Informe o KM como um número inteiro de 1 a 9999.", txtKm);
			return;
		}
		for (Aresta a : arestasDaTabela()) {
			if (a.liga(codOrigem, codDestino)) {
				erroCampo("A rota " + codOrigem + " - " + codDestino + " já está na tabela.", txtCodOrigem);
				return;
			}
		}

		// Nome digitado atualiza o cadastro; nome vazio usa o do cadastro.
		if (cidOrigem.isEmpty()) {
			cidOrigem = cidades.nome(codOrigem);
		} else {
			cidades.definir(codOrigem, cidOrigem);
			atualizarNomes(codOrigem, cidOrigem);
		}
		if (cidDestino.isEmpty()) {
			cidDestino = cidades.nome(codDestino);
		} else {
			cidades.definir(codDestino, cidDestino);
			atualizarNomes(codDestino, cidDestino);
		}
		gravarCidades();

		modelo.addRow(new Object[] { codOrigem, cidOrigem, codDestino, cidDestino, km });

		txtCodOrigem.setText("");
		txtCidOrigem.setText("");
		txtCodDestino.setText("");
		txtCidDestino.setText("");
		txtKm.setText("");
		txtCodOrigem.requestFocusInWindow();
	}

	private void removerSelecionadas() {
		int[] linhas = tabela.getSelectedRows();
		if (tabela.isEditing()) {
			tabela.getCellEditor().cancelCellEditing();
		}
		for (int i = linhas.length - 1; i >= 0; i--) {
			modelo.removeRow(tabela.convertRowIndexToModel(linhas[i]));
		}
	}

	// =====================================================================
	// SALVAR: gera um rotaNN.txt
	// =====================================================================

	private void salvar() {
		pararEdicao();
		List<Aresta> arestas = arestasDaTabela();
		if (arestas.isEmpty()) {
			erro("Adicione pelo menos uma rota antes de salvar.");
			return;
		}

		Configuracao cfg = gerenciador.carregarOuPadrao();
		File pastaInicial = diretorioExistente(cfg.getPasta());
		JFileChooser seletor = new JFileChooser(pastaInicial);
		seletor.setDialogTitle("Salvar arquivo de rota");
		seletor.setFileFilter(new FileNameExtensionFilter("Arquivos de rota (*.txt)", "txt"));
		String sugestao = EscritorArquivoRota.proximoNome(cfg.getPasta(), cfg.getSucesso(), cfg.getErro());
		seletor.setSelectedFile(new File(pastaInicial, sugestao));
		if (seletor.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File escolhido = seletor.getSelectedFile();
		if (!escolhido.getName().toLowerCase().endsWith(".txt")) {
			escolhido = new File(escolhido.getParentFile(), escolhido.getName() + ".txt");
		}
		if (!EscritorArquivoRota.NOME_ARQUIVO_ROTA.matcher(escolhido.getName()).matches()) {
			int r = JOptionPane.showConfirmDialog(this, "O nome \"" + escolhido.getName()
					+ "\" não segue o padrão rotaNN.txt e não será processado pela rota automática.\nSalvar mesmo assim?",
					"Salvar", JOptionPane.YES_NO_OPTION);
			if (r != JOptionPane.YES_OPTION) {
				return;
			}
		}
		if (escolhido.exists()) {
			int r = JOptionPane.showConfirmDialog(this, "O arquivo já existe. Substituir?", "Salvar",
					JOptionPane.YES_NO_OPTION);
			if (r != JOptionPane.YES_OPTION) {
				return;
			}
		}

		try {
			EscritorArquivoRota.salvar(escolhido.toPath(), arestas);
			gravarCidades();
			String msg = "Arquivo salvo em:\n" + escolhido.getAbsolutePath();
			if (escolhido.getParentFile() != null && escolhido.getParentFile().toPath().toAbsolutePath().normalize()
					.equals(cfg.getPasta().toAbsolutePath().normalize())) {
				msg += "\n\nCom a rota automática ativa, ele será processado em até 10 segundos.";
			}
			JOptionPane.showMessageDialog(this, msg, "Salvar", JOptionPane.INFORMATION_MESSAGE);
		} catch (ArquivoRotaInvalidoException e) {
			erro(e.getMessage());
		} catch (IOException e) {
			erro("Não foi possível salvar o arquivo:\n" + e.getMessage());
		}
	}

	// =====================================================================
	// PROCESSAR: Dijkstra do professor
	// =====================================================================

	private void processar() {
		pararEdicao();
		List<Aresta> arestas = arestasDaTabela();
		if (arestas.isEmpty()) {
			erro("Adicione rotas na tabela (manualmente ou pelo Buscar) antes de processar.");
			return;
		}

		MapaRotas mapa = new MapaRotas(arestas);
		List<String> nos = mapa.getNos();
		String[] opcoes = new String[nos.size()];
		for (int i = 0; i < nos.size(); i++) {
			opcoes[i] = descrever(nos.get(i));
		}

		JComboBox<String> cbOrigem = new JComboBox<String>(opcoes);
		JComboBox<String> cbDestino = new JComboBox<String>(opcoes);
		cbOrigem.setSelectedIndex(indiceOuPadrao(nos, txtCodOrigem.getText(), 0));
		cbDestino.setSelectedIndex(indiceOuPadrao(nos, txtCodDestino.getText(), nos.size() - 1));

		JPanel painel = new JPanel(new GridLayout(2, 2, 8, 8));
		painel.add(negrito("Origem:"));
		painel.add(cbOrigem);
		painel.add(negrito("Destino:"));
		painel.add(cbDestino);
		if (JOptionPane.showConfirmDialog(this, painel, "Menor caminho", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) {
			return;
		}

		String origem = nos.get(cbOrigem.getSelectedIndex());
		String destino = nos.get(cbDestino.getSelectedIndex());
		if (origem.equals(destino)) {
			erro("Escolha cidades de origem e destino diferentes.");
			return;
		}

		MenorCaminho caminho = mapa.calcular(origem, destino);
		if (!caminho.isEncontrado()) {
			JOptionPane.showMessageDialog(this,
					"Não existe caminho entre " + descrever(origem) + " e " + descrever(destino) + ".", "Resultado",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		destacarNaTabela(caminho);

		StringBuilder sb = new StringBuilder();
		sb.append("Origem:  ").append(descrever(origem)).append('\n');
		sb.append("Destino: ").append(descrever(destino)).append("\n\n");
		sb.append("Melhor caminho: ").append(caminho.formatar(" → ")).append("\n\n");
		sb.append("Trechos:\n");
		for (Aresta trecho : caminho.getTrechos()) {
			sb.append("  ").append(descrever(trecho.getOrigem())).append("  →  ").append(descrever(trecho.getDestino()))
					.append(": ").append(trecho.getPeso()).append(" km\n");
		}
		sb.append("\nDistância total: ").append(caminho.getDistancia()).append(" km");

		JTextArea resultado = new JTextArea(sb.toString());
		resultado.setEditable(false);
		resultado.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
		resultado.setOpaque(false);
		JOptionPane.showMessageDialog(this, resultado, "Resultado - Dijkstra", JOptionPane.INFORMATION_MESSAGE);
	}

	/** Seleciona na tabela as linhas que fazem parte do caminho encontrado. */
	private void destacarNaTabela(final MenorCaminho caminho) {
		tabela.clearSelection();
		for (Aresta trecho : caminho.getTrechos()) {
			for (int linha = 0; linha < modelo.getRowCount(); linha++) {
				String o = (String) modelo.getValueAt(linha, COL_COD_ORIGEM);
				String d = (String) modelo.getValueAt(linha, COL_COD_DESTINO);
				if (trecho.liga(o, d)) {
					int vista = tabela.convertRowIndexToView(linha);
					tabela.addRowSelectionInterval(vista, vista);
				}
			}
		}
	}

	// =====================================================================
	// Auxiliares
	// =====================================================================

	private List<Aresta> arestasDaTabela() {
		List<Aresta> arestas = new ArrayList<Aresta>();
		for (int linha = 0; linha < modelo.getRowCount(); linha++) {
			arestas.add(new Aresta((String) modelo.getValueAt(linha, COL_COD_ORIGEM),
					(String) modelo.getValueAt(linha, COL_COD_DESTINO),
					((Number) modelo.getValueAt(linha, COL_DISTANCIA)).intValue()));
		}
		return arestas;
	}

	/** Coloca o mesmo nome em todas as linhas que usam o código. */
	private void atualizarNomes(final String codigo, final String nome) {
		atualizandoCidades = true;
		try {
			for (int linha = 0; linha < modelo.getRowCount(); linha++) {
				if (codigo.equals(modelo.getValueAt(linha, COL_COD_ORIGEM))) {
					modelo.setValueAt(nome, linha, COL_CID_ORIGEM);
				}
				if (codigo.equals(modelo.getValueAt(linha, COL_COD_DESTINO))) {
					modelo.setValueAt(nome, linha, COL_CID_DESTINO);
				}
			}
		} finally {
			atualizandoCidades = false;
		}
	}

	private void gravarCidades() {
		try {
			cidades.salvar();
		} catch (IOException ignorado) {
			// os nomes continuam na tela; só não ficam guardados para a próxima vez
		}
	}

	private String descrever(final String codigo) {
		String nome = cidades.nome(codigo);
		return nome.isEmpty() ? codigo : codigo + " - " + nome;
	}

	private static int indiceOuPadrao(final List<String> nos, final String codigo, final int padrao) {
		int i = nos.indexOf(codigo.trim().toUpperCase());
		return i >= 0 ? i : padrao;
	}

	private static File diretorioExistente(final Path... opcoes) {
		for (Path p : opcoes) {
			if (p != null && Files.isDirectory(p)) {
				return p.toFile();
			}
		}
		return null;
	}

	private void pararEdicao() {
		if (tabela.isEditing()) {
			tabela.getCellEditor().stopCellEditing();
		}
	}

	private void erro(final String mensagem) {
		JOptionPane.showMessageDialog(this, mensagem, getTitle(), JOptionPane.ERROR_MESSAGE);
	}

	private void erroCampo(final String mensagem, final JTextField campo) {
		erro(mensagem);
		campo.requestFocusInWindow();
		campo.selectAll();
	}
}
