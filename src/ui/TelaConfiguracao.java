package ui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import config.Configuracao;
import config.ConfiguracaoException;
import config.GerenciadorConfiguracao;

/**
 * Tela "Configuração" do enunciado: Pasta, Sucesso, Erro, Rota automática e SALVAR.
 * Ao salvar, grava o config.txt e cria os diretórios.
 */
public class TelaConfiguracao extends JDialog {

	private static final long serialVersionUID = 1L;

	private final GerenciadorConfiguracao gerenciador;

	private final JTextField txtPasta = new JTextField(26);
	private final JTextField txtSucesso = new JTextField(26);
	private final JTextField txtErro = new JTextField(26);
	private final JCheckBox chkRotaAutomatica = new JCheckBox("Rota automática");
	private final JButton btnSalvar = new JButton("SALVAR");

	private Configuracao configuracaoSalva;

	public TelaConfiguracao(final JFrame dono, final GerenciadorConfiguracao gerenciador) {
		super(dono, "Configuração", true);
		this.gerenciador = gerenciador;
		montarTela();
		preencher(gerenciador.carregarOuPadrao());
		pack();
		setResizable(false);
		setLocationRelativeTo(dono);
	}

	private void montarTela() {
		JPanel painel = new JPanel(new GridBagLayout());
		painel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		adicionarLinha(painel, 0, "Pasta:", txtPasta, "Diretório raiz da aplicação, onde os arquivos rotaNN.txt são copiados");
		adicionarLinha(painel, 1, "Sucesso:", txtSucesso, "Diretório dos arquivos processados com sucesso");
		adicionarLinha(painel, 2, "Erro:", txtErro, "Diretório dos arquivos processados com erro");

		chkRotaAutomatica.setToolTipText("Quando marcado, o sistema processa os arquivos da Pasta em segundo plano");
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 3;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(6, 4, 4, 4);
		painel.add(chkRotaAutomatica, c);

		btnSalvar.setFont(btnSalvar.getFont().deriveFont(Font.BOLD));
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 4;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(6, 4, 4, 4);
		painel.add(btnSalvar, c);

		btnSalvar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				salvar();
			}
		});
		getRootPane().setDefaultButton(btnSalvar);
		setContentPane(painel);
	}

	private void adicionarLinha(final JPanel painel, final int linha, final String rotulo, final JTextField campo,
			final String dica) {
		JLabel label = new JLabel(rotulo);
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = linha;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(4, 4, 4, 8);
		painel.add(label, c);

		campo.setToolTipText(dica);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = linha;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.insets = new Insets(4, 4, 4, 4);
		painel.add(campo, c);

		JButton escolher = new JButton("...");
		escolher.setToolTipText("Escolher diretório");
		escolher.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				escolherDiretorio(campo);
			}
		});
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = linha;
		c.insets = new Insets(4, 0, 4, 4);
		painel.add(escolher, c);
	}

	private void preencher(final Configuracao configuracao) {
		txtPasta.setText(configuracao.getPasta().toString());
		txtSucesso.setText(configuracao.getSucesso().toString());
		txtErro.setText(configuracao.getErro().toString());
		chkRotaAutomatica.setSelected(configuracao.isRotaAutomatica());
	}

	private void escolherDiretorio(final JTextField campo) {
		JFileChooser seletor = new JFileChooser(campo.getText().trim().isEmpty() ? null : new File(campo.getText().trim()));
		seletor.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (seletor.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			campo.setText(seletor.getSelectedFile().getAbsolutePath());
		}
	}

	private void salvar() {
		try {
			Path pasta = caminho(txtPasta, "Pasta");
			Path sucesso = caminho(txtSucesso, "Sucesso");
			Path erro = caminho(txtErro, "Erro");
			if (sucesso.equals(erro)) {
				throw new ConfiguracaoException("Os diretórios de Sucesso e Erro devem ser diferentes.");
			}
			if (pasta.equals(sucesso) || pasta.equals(erro)) {
				throw new ConfiguracaoException("Os diretórios de Sucesso e Erro devem ser diferentes da Pasta.");
			}

			Configuracao nova = new Configuracao(pasta, sucesso, erro, chkRotaAutomatica.isSelected(), false);
			gerenciador.salvar(nova);
			configuracaoSalva = nova;
			dispose();
		} catch (ConfiguracaoException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Configuração", JOptionPane.ERROR_MESSAGE);
		}
	}

	private static Path caminho(final JTextField campo, final String nome) throws ConfiguracaoException {
		String texto = campo.getText().trim();
		if (texto.isEmpty()) {
			throw new ConfiguracaoException("Preencha o campo " + nome + ".");
		}
		try {
			return Paths.get(texto).toAbsolutePath().normalize();
		} catch (InvalidPathException e) {
			throw new ConfiguracaoException("Caminho inválido no campo " + nome + ": " + texto);
		}
	}

	/** A configuração gravada, ou null se a tela foi fechada sem salvar. */
	public Configuracao getConfiguracaoSalva() {
		return configuracaoSalva;
	}
}
