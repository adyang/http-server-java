package server.handlers;

import server.Handler;
import server.data.Header;
import server.data.Request;
import server.data.Response;
import server.util.ByteChannels;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class ParametersWrapper implements Handler {
    static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private final Handler handler;

    public ParametersWrapper(Handler handler) {
        this.handler = handler;
    }

    @Override
    public Response handle(Request request) {
        if (APPLICATION_FORM_URLENCODED.equals(request.headers.get(Header.CONTENT_TYPE))) {
            long contentLength = parseContentLength(request.headers);
            String rawParams = ByteChannels.slurp(ByteChannels.limit(request.body, contentLength));
            request.parameters = Arrays.stream(rawParams.split("&"))
                    .map(rp -> rp.split("="))
                    .collect(Collectors.toMap(p -> p[0], p -> p.length < 2 ? "" : decode(p[1])));
        }
        return handler.handle(request);
    }

    private static long parseContentLength(Map<String, String> headers) {
        String contentLength = headers.getOrDefault(Header.CONTENT_LENGTH, "0");
        return Long.parseLong(contentLength);
    }

    private String decode(String formEncodedStr) {
        try {
            return URLDecoder.decode(formEncodedStr, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
