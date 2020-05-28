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
        if (isUrlEncodedForm(request)) request.parameters = parseFormParameters(request);
        if (hasQuery(request)) request.parameters = parseParameters(request.query);
        return handler.handle(request);
    }

    private boolean isUrlEncodedForm(Request request) {
        return APPLICATION_FORM_URLENCODED.equals(request.headers.get(Header.CONTENT_TYPE));
    }

    private Map<String, String> parseFormParameters(Request request) {
        long contentLength = parseContentLength(request.headers);
        String rawParams = ByteChannels.slurp(ByteChannels.limit(request.body, contentLength));
        return parseParameters(rawParams);
    }

    private boolean hasQuery(Request request) {
        return request.query != null;
    }

    private Map<String, String> parseParameters(String rawParams) {
        return Arrays.stream(rawParams.split("&"))
                .map(rp -> rp.split("="))
                .collect(Collectors.toMap(p -> p[0], p -> p.length < 2 ? "" : decode(p[1])));
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
