package server;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class Response {
    public final Status status;
    public final Object body;
    public final Map<String, Object> headers;

    public Response(Status status, Object body) {
        this(status, Collections.emptyMap(), body);
    }

    public Response(Status status, Map<String, Object> headers, Object body) {
        this.status = status;
        this.body = body;
        this.headers = headers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Response response = (Response) o;
        return status == response.status &&
                Objects.equals(body, response.body) &&
                Objects.equals(headers, response.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, body, headers);
    }

    @Override
    public String toString() {
        return "Response{" +
                "status=" + status +
                ", body=" + body +
                ", headers=" + headers +
                '}';
    }
}
