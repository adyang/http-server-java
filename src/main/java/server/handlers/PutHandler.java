package server.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Handler;
import server.data.Header;
import server.data.Request;
import server.data.Response;
import server.data.Status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class PutHandler implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(PutHandler.class);

    private final Path directory;

    public PutHandler(Path directory) {
        this.directory = directory;
    }

    @Override
    public Response handle(Request request) {
        Path resource = directory.resolve(request.path.substring(1));
        try {
            long contentLength = parseContentLength(request.headers);
            return put(resource, request.body, contentLength);
        } catch (InvalidRequest e) {
            return new Response(Status.BAD_REQUEST, e.getMessage());
        }
    }

    private static long parseContentLength(Map<String, String> headers) {
        String contentLength = headers.getOrDefault(Header.CONTENT_LENGTH, "0");
        try {
            return Long.parseLong(contentLength);
        } catch (NumberFormatException e) {
            throw new InvalidRequest("Invalid Content-Length: " + contentLength);
        }
    }

    private Response put(Path resource, ReadableByteChannel body, long contentLength) {
        if (Files.isRegularFile(resource)) {
            write(resource, body, contentLength);
            return new Response(Status.OK, "");
        } else if (Files.notExists(resource)) {
            write(resource, body, contentLength);
            return new Response(Status.CREATED, "");
        } else {
            return new Response(Status.CONFLICT, "Unable to create/update: " + resource.getFileName() + " is a directory.");
        }
    }

    private void write(Path resource, ReadableByteChannel rbc, long contentLength) {
        Path temp = createTempFile(directory);
        try (FileChannel sbc = FileChannel.open(temp, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            long bytesTransferred = sbc.transferFrom(rbc, 0, contentLength);
            if (bytesTransferred == contentLength)
                Files.move(temp, resource, StandardCopyOption.REPLACE_EXISTING);
            else
                throw new InvalidRequest("Incomplete message: body is not " + contentLength + " byte(s)");
        } catch (NonReadableChannelException e) {
            throw new InvalidRequest("Incomplete message: unable to read body");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            deleteIfExists(temp);
        }
    }

    private static Path createTempFile(Path tempDirectory) {
        try {
            return Files.createTempFile(tempDirectory, null, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteIfExists(Path temp) {
        try {
            Files.deleteIfExists(temp);
        } catch (IOException e) {
            logger.warn("Failed to delete temp file: {}", temp);
        }
    }

    private static class InvalidRequest extends RuntimeException {
        public InvalidRequest(String message) {
            super(message);
        }
    }
}
