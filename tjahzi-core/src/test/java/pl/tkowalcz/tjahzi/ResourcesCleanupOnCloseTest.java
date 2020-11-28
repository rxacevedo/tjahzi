package pl.tkowalcz.tjahzi;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.tkowalcz.tjahzi.http.ClientConfiguration;
import pl.tkowalcz.tjahzi.http.HttpClientFactory;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ResourcesCleanupOnCloseTest {

    private WireMockServer wireMockServer;
    private TjahziInitializer initializer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(
                wireMockConfig()
                        .dynamicPort()
                        .dynamicHttpsPort()
        );

        wireMockServer.start();
        initializer = new TjahziInitializer();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldStopThreads() {
        // Given
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .withConnectionTimeoutMillis(10_000)
                .withHost("localhost")
                .withPort(wireMockServer.port())
                .withMaxRetries(1)
                .build();

        NettyHttpClient httpClient = HttpClientFactory.defaultFactory()
                .getHttpClient(
                        clientConfiguration,
                        new String[]{
                                "X-Scope-OrgID", "Circus",
                                "C", "Control"
                        }
                );

        LoggingSystem loggingSystem = initializer.createLoggingSystem(
                httpClient,
                1024 * 1024,
                false
        );

        // Verify our assumptions that we can find threads started by Tjahzi
        Awaitility.await().untilAsserted(() -> {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);

            assertThat(threadInfos)
                    .extracting(ThreadInfo::getThreadName)
                    .contains(
                            "ReadingLogBufferAndSendingHttp",
                            "tjahzi-worker"
                    );
        });

        //When
        loggingSystem.close(
                (int) TimeUnit.SECONDS.toMillis(10),
                System.out::println
        );

        // Then
        Awaitility.await().untilAsserted(() -> {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);

            assertThat(threadInfos)
                    .extracting(ThreadInfo::getThreadName)
                    .doesNotContain(
                            "ReadingLogBufferAndSendingHttp",
                            "tjahzi-worker"
                    );
        });
    }
}