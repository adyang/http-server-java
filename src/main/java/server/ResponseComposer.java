package server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.data.Response;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Map;

public class ResponseComposer {
    private static final Logger logger = LoggerFactory.getLogger(ResponseComposer.class);

    static void compose(PrintStream out, Response response) {
        try {
            writeStatusLine(out, response);
            writeHeaders(out, response);
            out.print("\r\n");
            writeBody(out, response);
            out.flush();
        } catch (Exception e) {
            throw new ComposeException(e);
        }
    }

    private static void writeStatusLine(PrintStream out, Response response) {
        String statusLine = String.format("HTTP/1.1 %d %s", response.status.code, response.status.reason);
        logger.info("[Response] '{}'", statusLine);
        out.print(statusLine + "\r\n");
    }

    private static void writeHeaders(PrintStream out, Response response) {
        for (Map.Entry<String, Object> header : response.headers.entrySet()) {
            out.printf("%s: %s\r\n", header.getKey(), header.getValue());
        }
    }

    private static void writeBody(PrintStream out, Response response) {
        if (response.body instanceof String) {
            writeStringBody(out, response.body);
        } else if (response.body instanceof byte[]) {
            writeByteArrayBody(out, (byte[]) response.body);
        } else if (response.body instanceof ReadableByteChannel) {
            writeReadableByteChannel(out, (ReadableByteChannel) response.body);
        }
    }

    private static void writeStringBody(PrintStream out, Object body) {
        out.printf("%s", body);
    }

    private static void writeByteArrayBody(PrintStream out, byte[] body) {
        try {
            out.write(body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeReadableByteChannel(PrintStream out, ReadableByteChannel body) {
        try (ReadableByteChannel rbc = body) {
            WritableByteChannel wbc = Channels.newChannel(out);
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (rbc.read(buffer) != -1) {
                rbc.read(buffer);
                buffer.flip();
                wbc.write(buffer);
                buffer.clear();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static class ComposeException extends RuntimeException {
        public ComposeException(Exception e) {
            super(e);
        }
    }
}
