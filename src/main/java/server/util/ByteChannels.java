package server.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

public class ByteChannels {
    private static final int BUFFER_SIZE = 1024;
    private static final int EOS = -1;

    public static String slurp(ReadableByteChannel rbc) {
        StringBuilder builder = new StringBuilder();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        while (readInto(rbc, buffer) != EOS) {
            buffer.flip();
            builder.append(StandardCharsets.UTF_8.decode(buffer).toString());
            buffer.clear();
        }
        return builder.toString();
    }

    private static int readInto(ReadableByteChannel rbc, ByteBuffer buffer) {
        try {
            return rbc.read(buffer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static ReadableByteChannel limit(ReadableByteChannel rbc, long limit) {
        return new LimitedReadableByteChannel(rbc, limit);
    }

    private static class LimitedReadableByteChannel implements ReadableByteChannel {
        private final ReadableByteChannel rbc;
        private long limit;

        public LimitedReadableByteChannel(ReadableByteChannel rbc, long limit) {
            if (limit < 0) throw new IllegalArgumentException("Limit should be non-negative");
            this.rbc = rbc;
            this.limit = limit;
        }

        @Override
        public synchronized int read(ByteBuffer dst) throws IOException {
            if (limit == 0) return -1;

            ByteBuffer buffer = ByteBuffer.allocate((int) Math.min(dst.limit(), limit));
            int bytesRead = rbc.read(buffer);
            if (bytesRead > -1) {
                buffer.flip();
                dst.put(buffer);
                limit -= bytesRead;
            }
            return bytesRead;
        }

        @Override
        public boolean isOpen() {
            return rbc.isOpen();
        }

        @Override
        public void close() throws IOException {
            rbc.close();
        }
    }
}
