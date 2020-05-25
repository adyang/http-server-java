package server.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Handler;
import server.data.Header;
import server.data.Request;
import server.data.Response;
import server.data.Status;
import server.util.ByteChannels;
import server.util.Resources;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLConnection;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GetHandler implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(GetHandler.class);

    private final Path directory;

    public GetHandler(Path directory) {
        this.directory = directory;
    }

    @Override
    public Response handle(Request request) {
        Path resource = directory.resolve(request.path.substring(1));
        if (Files.isRegularFile(resource)) {
            return getFile(request, resource);
        } else if (Files.isDirectory(resource)) {
            return getDirectoryListing(request, resource);
        } else {
            return new Response(Status.NOT_FOUND, "");
        }
    }

    private static Response getFile(Request request, Path resource) {
        if (request.headers.containsKey(Header.RANGE)) {
            return partialContentOf(resource, request.headers);
        } else {
            return fullContentOf(resource);
        }
    }

    private static Response fullContentOf(Path resource) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Header.CONTENT_LENGTH, sizeOf(resource));
        String contentType = URLConnection.guessContentTypeFromName(resource.getFileName().toString());
        if (contentType != null) headers.put(Header.CONTENT_TYPE, contentType);
        return new Response(Status.OK, headers, byteChannelOf(resource));
    }

    private static long sizeOf(Path resource) {
        try {
            return Files.size(resource);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static SeekableByteChannel byteChannelOf(Path resource) {
        try {
            return Files.newByteChannel(resource, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Response partialContentOf(Path resource, Map<String, String> requestHeaders) {
        // Close file only on exceptions as downstream is responsible for closing on happy path
        SeekableByteChannel sbc = null;
        try {
            sbc = Files.newByteChannel(resource, StandardOpenOption.READ);
            Range range = parseRange(requestHeaders.get(Header.RANGE), sbc.size());
            sbc.position(range.start);
            long partialSize = range.end - range.start + 1;
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
            throw new RuntimeException(e);
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

    private static Response getDirectoryListing(Request request, Path resource) {
        String listing = directoryListingOf(resource)
                .map(Path::getFileName)
                .map(f -> String.format("<li><a href=\"%s\">%s</a></li>", linkOf(request.path, f), f))
                .collect(Collectors.joining());
        String directoryTemplate = Resources.slurp("/directory.html");
        String directoryListing = String.format(directoryTemplate, request.path, listing);
        Map<String, Object> headers = new HashMap<>();
        headers.put(Header.CONTENT_LENGTH, directoryListing.length());
        headers.put(Header.CONTENT_TYPE, "text/html");
        return new Response(Status.OK, headers, directoryListing);
    }

    private static Stream<Path> directoryListingOf(Path resource) {
        try {
            return Files.list(resource);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String linkOf(String basePath, Path filename) {
        return basePath.endsWith("/") ? basePath + filename : basePath + "/" + filename;
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
