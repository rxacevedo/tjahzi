package pl.tkowalcz.tjahzi;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LogBufferSerializerTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("pl.tkowalcz.tjahzi.LogBufferSerializerDeserializerTest#variousMessageConfigurations")
    void shouldCalculateSizeOfMessage(
            String testName,
            Map<String, String> labels,
            String logLevelLabel,
            String logLevel,
            String logLine,
            int expectedSize) {
        // Given
        UnsafeBuffer buffer = new UnsafeBuffer(new byte[expectedSize]);
        LogBufferSerializer serializer = new LogBufferSerializer(buffer);

        // When
        int actual = serializer.calculateRequiredSizeAscii(
                labels,
                logLevelLabel,
                logLevel,
                ByteBuffer.wrap(logLine.getBytes())
        );

        // Then
        assertThat(actual).isEqualTo(expectedSize);
    }
}
