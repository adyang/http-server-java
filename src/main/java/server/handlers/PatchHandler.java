package server.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Handler;
import server.data.Header;
import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.Maps;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class PatchHandler implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(PatchHandler.class);

    private final Path directory;

    public PatchHandler(Path directory) {
        this.directory = directory;
    }

    @Override
    public Response handle(Request request) {
        Path resource = directory.resolve(request.path.substring(1));
        if (!request.headers.containsKey(Header.IF_MATCH))
            return new Response(Status.CONFLICT, "Missing ETag, unable to patch file.");
        try {
            long contentLength = parseContentLength(request.headers);
            return patch(resource, request.body, request.headers.get(Header.IF_MATCH), contentLength);
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

    private Response patch(Path resource, ReadableByteChannel body, String eTag, long contentLength) {
        if (Files.isRegularFile(resource)) {
            if (!eTag.equals(sha1Of(resource)))
                return new Response(Status.PRECONDITION_FAILED, "ETag does not match file checksum.");
            String newETag = write(resource, body, contentLength);
            return new Response(Status.NO_CONTENT, Maps.of(Header.E_TAG, newETag), "");
        } else if (Files.isDirectory(resource)) {
            return new Response(Status.CONFLICT, "Unable to create/update: " + resource.getFileName() + " is a directory.");
        } else {
            return new Response(Status.NOT_FOUND, "");
        }
    }

    private String write(Path resource, ReadableByteChannel rbc, long contentLength) {
        Path temp = createTempFile(directory);
        try (FileChannel sbc = FileChannel.open(temp, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            long bytesTransferred = sbc.transferFrom(rbc, 0, contentLength);
            if (bytesTransferred != contentLength)
                throw new InvalidRequest("Incomplete message: body is not " + contentLength + " byte(s)");
            Files.move(temp, resource, StandardCopyOption.REPLACE_EXISTING);
            return sha1Of(resource);
        } catch (NonReadableChannelException e) {
            throw new InvalidRequest("Incomplete message: unable to read body");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            deleteIfExists(temp);
        }
    }

    private static String sha1Of(Path resource) {
        try (InputStream is = Files.newInputStream(resource);
             DigestInputStream dis = new DigestInputStream(is, digest("SHA-1"))) {
            while (dis.read() != -1) ;
            byte[] digest = dis.getMessageDigest().digest();
            return String.format("%040x", new BigInteger(1, digest));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static MessageDigest digest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
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
