package server;

public enum Status {
    OK(200, "OK"),
    CREATED(201, "Created"),
    BAD_REQUEST(400, "Bad Request"),
    NOT_FOUND(404, "Not Found"),
    CONFLICT(409, "Conflict"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    public final int code;
    public final String reason;

    Status(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }
}
