package server.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Handler;
import server.data.Header;
import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.ByteChannels;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLConnection;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DefaultHandler implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(DefaultHandler.class);
    private static final int EOF = -1;
    private final Path directory;

    public DefaultHandler(Path directory) {
        this.directory = directory;
    }

    @Override
    public Response handle(Request request) throws IOException {
        Path resource = directory.resolve(request.uri.substring(1));
        switch (request.method) {
            case HEAD:
                return head(resource);
            case GET:
                return get(request, directory);
            case PUT:
                return put(request, directory);
            case DELETE:
                return delete(request, directory);
            default:
                return new Response(Status.INTERNAL_SERVER_ERROR, "");
        }
    }

    private static Response head(Path resource) throws IOException {
        if (Files.exists(resource)) {
            Map<String, Object> headers = Collections.singletonMap(Header.CONTENT_LENGTH, Files.size(resource));
            return new Response(Status.OK, headers, "");
        } else {
            return new Response(Status.NOT_FOUND, "");
        }
    }

    private static Response get(Request request, Path directory) throws IOException {
        Path resource = directory.resolve(request.uri.substring(1));
        if (Files.isRegularFile(resource)) {
            return getFile(request, resource);
        } else if (Files.isDirectory(resource)) {
            return getDirectoryListing(request, resource);
        } else {
            return new Response(Status.NOT_FOUND, "");
        }
    }

    private static Response getFile(Request request, Path resource) throws IOException {
        if (request.headers.containsKey(Header.RANGE)) {
            return partialContentOf(resource, request.headers);
        } else {
            return fullContentOf(resource);
        }
    }

    private static Response fullContentOf(Path resource) throws IOException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Header.CONTENT_LENGTH, Files.size(resource));
        String contentType = URLConnection.guessContentTypeFromName(resource.getFileName().toString());
        if (contentType != null) headers.put(Header.CONTENT_TYPE, contentType);
        return new Response(Status.OK, headers, Files.newByteChannel(resource, StandardOpenOption.READ));
    }

    private static Response partialContentOf(Path resource, Map<String, String> requestHeaders) throws IOException {
        // Close file only on exceptions as downstream is responsible for closing on happy path
        SeekableByteChannel sbc = null;
        try {
            sbc = Files.newByteChannel(resource, StandardOpenOption.READ);
            Range range = parseRange(requestHeaders.get(Header.RANGE), sbc.size());
            long partialSize = range.end - range.start + 1;
            sbc.position(range.start);
            ReadableByteChannel partialContent = ByteChannels.limit(sbc, partialSize);
            Map<String, Object> headers = new HashMap<>();
            headers.put(Header.CONTENT_RANGE, String.format("bytes %d-%d/%d", range.start, range.end, sbc.size()));
            headers.put(Header.CONTENT_LENGTH, partialSize);
            return new Response(Status.PARTIAL_CONTENT, headers, partialContent);
        } catch (UnknownRangeUnit e) {
            close(sbc);
            return fullContentOf(resource);
        } catch (InvalidByteRange e) {
            close(sbc);
            Map<String, Object> headers = Collections.singletonMap(Header.CONTENT_RANGE, String.format("bytes */%d", e.resourceSize));
            return new Response(Status.REQUESTED_RANGE_NOT_SATISFIABLE, headers, "");
        } catch (Exception e) {
            close(sbc);
            throw e;
        }
    }

    private static void close(SeekableByteChannel sbc) {
        if (sbc == null) return;
        try {
            sbc.close();
        } catch (IOException e) {
            logger.warn("Unable to close ByteChannel.", e);
        }
    }

    private static Range parseRange(String range, long resourceSize) {
        if (!range.trim().startsWith("bytes=")) throw new UnknownRangeUnit();
        Pattern pattern = Pattern.compile("bytes=(?:(?<start>\\d+)-(?<end>\\d*)|-(?<suffix>\\d+))");
        Matcher matcher = pattern.matcher(range);
        if (!matcher.find()) throw new InvalidByteRange(resourceSize);
        if (matcher.group("suffix") == null) {
            return parseStartEndRange(matcher.group("start"), matcher.group("end"), resourceSize);
        } else {
            return parseSuffixRange(matcher.group("suffix"), resourceSize);
        }
    }

    private static Range parseStartEndRange(String rawStart, String rawEnd, long resourceSize) {
        long start = Long.parseLong(rawStart);
        if (start >= resourceSize) throw new InvalidByteRange(resourceSize);
        if (rawEnd.isEmpty()) {
            return new Range(start, (resourceSize - 1));
        } else {
            long end = Long.parseLong(rawEnd);
            if (start > end) throw new InvalidByteRange(resourceSize);
            return new Range(start, Math.min(end, resourceSize - 1));
        }
    }

    private static Range parseSuffixRange(String rawSuffix, long resourceSize) {
        long suffixLength = Long.parseLong(rawSuffix);
        if (suffixLength == 0) throw new InvalidByteRange(resourceSize);
        long start = Math.max(resourceSize - suffixLength, 0);
        long end = resourceSize - 1;
        return new Range(start, end);
    }

    private static Response getDirectoryListing(Request request, Path resource) throws IOException {
        String listing = Files.list(resource)
                .map(Path::getFileName)
                .map(f -> String.format("<li><a href=\"%s\">%s</a></li>", linkOf(request.uri, f), f))
                .collect(Collectors.joining());
        String directoryTemplate = slurp("/directory.html");
        String directoryListing = String.format(directoryTemplate, request.uri, listing);
        Map<String, Object> headers = new HashMap<>();
        headers.put(Header.CONTENT_LENGTH, directoryListing.length());
        headers.put(Header.CONTENT_TYPE, "text/html");
        return new Response(Status.OK, headers, directoryListing);
    }

    private static String linkOf(String basePath, Path filename) {
        return basePath.endsWith("/") ? basePath + filename : basePath + "/" + filename;
    }

    private static String slurp(String resourcePath) {
        try (InputStream in = DefaultHandler.class.getResourceAsStream(resourcePath);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != EOF) out.write(buffer, 0, length);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Response put(Request request, Path directory) {
        Path resource = directory.resolve(request.uri.substring(1));
        if (Files.isRegularFile(resource)) {
            write(resource, request.body);
            return new Response(Status.OK, request.body);
        } else if (Files.notExists(resource)) {
            write(resource, request.body);
            return new Response(Status.CREATED, request.body);
        } else {
            return new Response(Status.CONFLICT, "Unable to create/update: " + resource.getFileName() + " is a directory.");
        }
    }

    private static Response delete(Request request, Path directory) throws IOException {
        Path resource = directory.resolve(request.uri.substring(1));
        if (Files.isDirectory(resource)) {
            return new Response(Status.CONFLICT, "Unable to delete: " + resource.getFileName() + " is a directory.");
        } else {
            boolean deleted = Files.deleteIfExists(resource);
            return new Response(deleted ? Status.OK : Status.NOT_FOUND, "");
        }
    }

    private static Path write(Path resource, String content) {
        try {
            return Files.write(resource, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class UnknownRangeUnit extends RuntimeException {
    }

    private static class InvalidByteRange extends RuntimeException {
        final long resourceSize;

        public InvalidByteRange(long resourceSize) {
            this.resourceSize = resourceSize;
        }
    }

    private static class Range {
        final long start;
        final long end;

        public Range(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }
}
