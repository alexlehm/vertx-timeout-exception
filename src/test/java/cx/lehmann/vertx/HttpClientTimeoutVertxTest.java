package cx.lehmann.vertx;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.test.core.VertxTestBase;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientTimeoutVertxTest extends VertxTestBase {

  static final Logger logger = LoggerFactory.getLogger(HttpClientTimeoutVertxTest.class);

  private final int port = 8080;

  @Test
  public void testHttpClientTimeout() throws InterruptedException {

    int idleTimeoutSecs = 1;  // TimeUnit.SECONDS
    int connectTimeoutMillis = 1000; // TimeUnit.MILLISECONDS

    final HttpClient client = vertx.createHttpClient(new HttpClientOptions().setIdleTimeout(idleTimeoutSecs)
        .setConnectTimeout(connectTimeoutMillis));

    Date startDate = new Date();

    HttpClientRequest req = client.request(HttpMethod.GET, port, "localhost", "/")
        .handler(
            resp -> {
              logger.info("handler CALLED AFTER {} ms BUT THE TIMEOUT IS SET TO {} MILLISECONDS ONLY!",
                  mseconds(startDate, new Date()), connectTimeoutMillis);
            })
        .endHandler(
            handler -> {
              logger.error("endHandler CALLED AFTER {} ms BUT THE TIMEOUT IS SET TO {} MILLISECONDS ONLY!",
                  mseconds(startDate, new Date()), connectTimeoutMillis);
              fail("A timeout exception was expected after one second.");
            })
        .exceptionHandler(th -> {
          if (th instanceof AssertionError) {
            throw ((AssertionError) th);
          } else {
            logger.info("exceptionHandler called after {} ms ({})", mseconds(startDate, new Date()), th.toString());
            testComplete();
          }
        });

    logger.info("Start http request with timeout of {} milliseconds", connectTimeoutMillis);
    req.end();

    await(30, TimeUnit.SECONDS);
  }

  /**
   * It returns the difference in seconds between two dates
   * 
   * @return
   */
  private long mseconds(final Date first, final Date second) {
    long millis = second.getTime() - first.getTime();
    return millis;
  }

  private HttpServer server;

  @Before
  public void startServer() {
    server = vertx.createHttpServer(new HttpServerOptions().setPort(8080)).requestHandler(req -> {
      logger.info("waiting before reply");
      vertx.setTimer(2000, l -> {
        logger.info("sending response");
        req.response().end("Hello World!");
      });
    }).listen();
  }

  @After
  public void stopServer() {
    server.close();
  }

}
