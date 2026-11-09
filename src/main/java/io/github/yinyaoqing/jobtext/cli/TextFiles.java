package io.github.yinyaoqing.jobtext.cli;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Bounded, strictly decoded reads and atomic writes. */
final class TextFiles {

    /** An input problem the user can fix: missing file, too large, wrong encoding. */
    static final class InputException extends IOException {

        private static final long serialVersionUID = 1L;

        InputException(String message) {
            super(message);
        }
    }

    private TextFiles() {
    }

    /**
     * Reads a whole file, refusing anything over {@code maxBytes} and any byte sequence that is
     * invalid in {@code charset}, instead of silently replacing it with U+FFFD.
     */
    static String read(Path file, Charset charset, long maxBytes) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new InputException(file + " is not a readable file");
        }
        long size = Files.size(file);
        if (size > maxBytes) {
            throw new InputException(file + " is " + size + " bytes, over the limit of " + maxBytes
                    + " (raise it with --max-bytes)");
        }
        byte[] bytes = Files.readAllBytes(file);
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer in = ByteBuffer.wrap(bytes);
        CharBuffer out = CharBuffer.allocate((int) Math.ceil(bytes.length * (double) decoder.maxCharsPerByte()) + 1);
        CoderResult result = decoder.decode(in, out, true);
        if (!result.isError()) {
            result = decoder.flush(out);
        }
        if (result.isError()) {
            String hint = charset.equals(StandardCharsets.UTF_8)
                    ? "; if the file was saved by Excel in Big5, pass --encoding Big5"
                    : "";
            throw new InputException(file + " is not valid " + charset.name() + " at byte " + in.position() + hint);
        }
        out.flip();
        return out.toString();
    }

    /** Writes UTF-8 via a temporary file in the same folder, so a crash never leaves half a report. */
    static void writeAtomically(Path target, String content) throws IOException {
        Path absolute = target.toAbsolutePath();
        Path dir = absolute.getParent();
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, "." + absolute.getFileName(), ".tmp");
        try {
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
