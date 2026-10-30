package util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lê arquivos de texto aceitando tanto UTF-8 quanto o padrão antigo do Windows
 * (ANSI / windows-1252). Isso é importante porque o config.txt é criado à mão no
 * Bloco de Notas e tem acento ("Não Processado").
 */
public final class LeitorTexto {

	private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

	private LeitorTexto() {
	}

	/** Lê o arquivo e devolve as linhas (sem as quebras de linha). */
	public static List<String> lerLinhas(final Path arquivo) throws IOException {
		byte[] bytes = Files.readAllBytes(arquivo);

		// Remove o BOM (marca que alguns editores colocam no início de arquivos UTF-8).
		if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
			bytes = Arrays.copyOfRange(bytes, 3, bytes.length);
		}

		String texto;
		try {
			texto = StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(bytes))
					.toString();
		} catch (CharacterCodingException naoEhUtf8) {
			texto = new String(bytes, WINDOWS_1252);
		}

		List<String> linhas = new ArrayList<String>(Arrays.asList(texto.split("\r\n|\r|\n", -1)));
		// Remove a "linha" vazia que sobra quando o arquivo termina com quebra de linha.
		if (!linhas.isEmpty() && linhas.get(linhas.size() - 1).isEmpty()) {
			linhas.remove(linhas.size() - 1);
		}
		return linhas;
	}
}
