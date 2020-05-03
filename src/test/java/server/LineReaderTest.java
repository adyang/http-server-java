package server;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class LineReaderTest {
    @Test
    void singleLine() throws IOException {
        ByteArrayInputStream in = inputStreamOf("line one\r\n");

        assertThat(LineReader.readLine(in)).isEqualTo("line one");
        assertThat(LineReader.readLine(in)).isEqualTo(null);
    }

    @Test
    void multipleLines() throws IOException {
        ByteArrayInputStream in = inputStreamOf("line one\r\nline two\r\n");

        assertThat(LineReader.readLine(in)).isEqualTo("line one");
        assertThat(LineReader.readLine(in)).isEqualTo("line two");
        assertThat(LineReader.readLine(in)).isEqualTo(null);
    }

    @Test
    void emptyLine() throws IOException {
        ByteArrayInputStream in = inputStreamOf("\r\n");

        assertThat(LineReader.readLine(in)).isEqualTo("");
        assertThat(LineReader.readLine(in)).isEqualTo(null);
    }

    @Test
    void incompleteLine() {
        ByteArrayInputStream in = inputStreamOf("incomplete line");

        Throwable error = catchThrowable(() -> LineReader.readLine(in));

        assertThat(error).isInstanceOf(LineReader.InvalidLineException.class);
        assertThat(error).hasMessageContaining("Missing line terminator before end of stream: incomplete line");
    }

    @Test
    void invalidLineTerminator_singleCarriageReturn() {
        ByteArrayInputStream in = inputStreamOf("line one\rline two\r\n");

        Throwable error = catchThrowable(() -> LineReader.readLine(in));

        assertThat(error).isInstanceOf(LineReader.InvalidLineException.class);
        assertThat(error).hasMessageContaining("Invalid line terminator, expecting <CR><LF> but got: <CR><l>");
    }

    @Test
    void invalidLineTerminator_singleLineFeed() {
        ByteArrayInputStream in = inputStreamOf("line one\nline two\r\n");

        Throwable error = catchThrowable(() -> LineReader.readLine(in));

        assertThat(error).isInstanceOf(LineReader.InvalidLineException.class);
        assertThat(error).hasMessageContaining("Invalid line terminator, expecting <CR><LF> but got: <LF>");
    }

    private ByteArrayInputStream inputStreamOf(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
