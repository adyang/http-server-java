package server.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class ByteChannelsTest {
    @Test
    void limit_isLessThanChannelSize() throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(new ByteArrayInputStream("123456789".getBytes(StandardCharsets.UTF_8)));

        ReadableByteChannel limited = ByteChannels.limit(rbc, 3);

        ByteBuffer buffer = ByteBuffer.allocate(9);
        assertThat(limited.read(buffer)).isEqualTo(3);
        assertThat(stringOf(buffer)).isEqualTo("123");
    }

    @Test
    void limit_isZero() throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(new ByteArrayInputStream("123456789".getBytes(StandardCharsets.UTF_8)));

        ReadableByteChannel limited = ByteChannels.limit(rbc, 0);

        ByteBuffer buffer = ByteBuffer.allocate(9);
        assertThat(limited.read(buffer)).isEqualTo(-1);
        assertThat(stringOf(buffer)).isEqualTo("");
    }

    @Test
    void limit_isNegative() {
        ReadableByteChannel rbc = Channels.newChannel(new ByteArrayInputStream("123456789".getBytes(StandardCharsets.UTF_8)));

        Throwable error = catchThrowable(() -> ByteChannels.limit(rbc, -1));

        assertThat(error).isInstanceOf(IllegalArgumentException.class);
        assertThat(error).hasMessageContaining("Limit should be non-negative");
    }

    @Test
    void limit_isMoreThanChannelSize() throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(new ByteArrayInputStream("123456789".getBytes(StandardCharsets.UTF_8)));

        ReadableByteChannel limited = ByteChannels.limit(rbc, 10);

        ByteBuffer buffer = ByteBuffer.allocate(9);
        assertThat(limited.read(buffer)).isEqualTo(9);
        assertThat(stringOf(buffer)).isEqualTo("123456789");
        buffer.clear();
        assertThat(limited.read(buffer)).isEqualTo(-1);
        assertThat(stringOf(buffer)).isEqualTo("");
    }

    @Test
    void limit_growingChannelSize() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream("123456789".getBytes(StandardCharsets.UTF_8));
        ReadableByteChannel rbc = Channels.newChannel(in);

        ReadableByteChannel limited = ByteChannels.limit(rbc, 10);
        ByteBuffer buffer = ByteBuffer.allocate(9);
        assertThat(limited.read(buffer)).isEqualTo(9);
        buffer.clear();
        assertThat(limited.read(buffer)).isEqualTo(-1);
        in.reset();

        buffer.clear();
        assertThat(limited.read(buffer)).isEqualTo(1);
        assertThat(stringOf(buffer)).isEqualTo("1");
    }

    @Test
    void limit_multipleReads() throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(new ByteArrayInputStream("123456789".getBytes(StandardCharsets.UTF_8)));

        ReadableByteChannel limited = ByteChannels.limit(rbc, 7);

        ByteBuffer buffer = ByteBuffer.allocate(4);
        assertThat(limited.read(buffer)).isEqualTo(4);
        assertThat(stringOf(buffer)).isEqualTo("1234");
        buffer.clear();
        assertThat(limited.read(buffer)).isEqualTo(3);
        assertThat(stringOf(buffer)).isEqualTo("567");
        buffer.clear();
        assertThat(limited.read(buffer)).isEqualTo(-1);
        assertThat(stringOf(buffer)).isEqualTo("");
    }

    @Test
    void isOpen_underlyingChannelIsOpen() {
        ReadableByteChannel rbc = Channels.newChannel(new ByteArrayInputStream("123456789".getBytes(StandardCharsets.UTF_8)));

        ReadableByteChannel limited = ByteChannels.limit(rbc, 7);

        assertThat(limited.isOpen()).isTrue();
    }

    @Test
    void isOpen_underlyingChannelIsClosed() throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(new ByteArrayInputStream("123456789".getBytes(StandardCharsets.UTF_8)));

        ReadableByteChannel limited = ByteChannels.limit(rbc, 7);
        rbc.close();

        assertThat(limited.isOpen()).isFalse();
    }

    private String stringOf(ByteBuffer buffer) {
        buffer.flip();
        return StandardCharsets.UTF_8.decode(buffer).toString();
    }
}
