package server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LineReader {
    private static final int BUFFER_SIZE = 8192;
    private static final int EOS = -1;
    private static final int CR = '\r';
    private static final int LF = '\n';
    private static final Set<Integer> LINE_TERMINATORS = Stream.of(CR, LF).collect(Collectors.toSet());

    public static String readLine(InputStream in) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        int curr;
        while ((curr = in.read()) != EOS && !LINE_TERMINATORS.contains(curr)) {
            buffer.put((byte) curr);
        }
        if (curr == CR)
            return validHttpLine(in, buffer);
        else if (curr == LF)
            throw new InvalidLineException("Invalid line terminator, expecting <CR><LF> but got: <LF>");
        else if (buffer.position() > 0)
            throw new InvalidLineException("Missing line terminator before end of stream: " + stringOf(buffer));
        else
            return null;
    }

    private static String validHttpLine(InputStream in, ByteBuffer buffer) throws IOException {
        char next = (char) in.read();
        if (next == LF)
            return stringOf(buffer);
        else
            throw new InvalidLineException("Invalid line terminator, expecting <CR><LF> but got: <CR><" + next + ">");
    }

    private static String stringOf(ByteBuffer buffer) {
        buffer.flip();
        return StandardCharsets.UTF_8.decode(buffer).toString();
    }

    public static class InvalidLineException extends RuntimeException {
        public InvalidLineException(String message) {
            super(message);
        }
    }
}
