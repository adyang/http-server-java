package server.handlers;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.Handler;
import server.data.Header;
import server.data.Method;
import server.data.Request;
import server.util.Maps;
import server.util.TestHandler;

import java.io.ByteArrayInputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class ParametersWrapperTest {
    private Handler wrapper;
    private TestHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TestHandler();
        wrapper = new ParametersWrapper(handler);
    }

    @Test
    void formParameters() {
        Request request = new Request(Method.GET, "/any");
        request.headers = Maps.of(Header.CONTENT_TYPE, ParametersWrapper.APPLICATION_FORM_URLENCODED);
        request.body = readableByteChannelOf("keyOne=value+One%24&keyTwo=value-two");

        wrapper.handle(request);

        assertThat(handler.receivedRequest.parameters).containsOnly(
                entry("keyOne", "value One$"),
                entry("keyTwo", "value-two")
        );
    }

    @Test
    void formParameters_noValue() {
        Request request = new Request(Method.GET, "/any");
        request.headers = Maps.of(Header.CONTENT_TYPE, ParametersWrapper.APPLICATION_FORM_URLENCODED);
        request.body = readableByteChannelOf("keyOne&keyTwo=value&keyThree=");

        wrapper.handle(request);

        assertThat(handler.receivedRequest.parameters).containsOnly(
                entry("keyOne", ""),
                entry("keyTwo", "value"),
                entry("keyThree", "")
        );
    }

    private ReadableByteChannel readableByteChannelOf(String content) {
        return Channels.newChannel(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}