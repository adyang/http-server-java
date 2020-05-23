package server.data;

public enum Status {
    OK(200, "OK"),
    CREATED(201, "Created"),
    PARTIAL_CONTENT(206, "Partial Content"),
    FOUND(302, "Found"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    REQUEST_TIMEOUT(408, "Request Timeout"),
    CONFLICT(409, "Conflict"),
    REQUESTED_RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
    I_AM_A_TEAPOT(418, "I'm a teapot"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED(501, "Not Implemented");

    public final int code;
    public final String reason;

    Status(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }
}
