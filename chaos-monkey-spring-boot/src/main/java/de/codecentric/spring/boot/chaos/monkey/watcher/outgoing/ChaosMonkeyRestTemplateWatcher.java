package de.codecentric.spring.boot.chaos.monkey.watcher.outgoing;

import de.codecentric.spring.boot.chaos.monkey.component.ChaosMonkeyRequestScope;
import de.codecentric.spring.boot.chaos.monkey.component.ChaosTarget;
import de.codecentric.spring.boot.chaos.monkey.configuration.AssaultProperties;
import de.codecentric.spring.boot.chaos.monkey.configuration.WatcherProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** @author Marcel Becker */
public class ChaosMonkeyRestTemplateWatcher implements ClientHttpRequestInterceptor {

  private final WatcherProperties watcherProperties;
  private final ChaosMonkeyRequestScope chaosMonkeyRequestScope;
  private final AssaultProperties assaultProperties;

  public ChaosMonkeyRestTemplateWatcher(
      final ChaosMonkeyRequestScope chaosMonkeyRequestScope,
      final WatcherProperties watcherProperties,
      AssaultProperties assaultProperties) {
    this.chaosMonkeyRequestScope = chaosMonkeyRequestScope;
    this.watcherProperties = watcherProperties;
    this.assaultProperties = assaultProperties;
  }

  @Override
  public ClientHttpResponse intercept(
      final HttpRequest httpRequest,
      byte[] bytes,
      ClientHttpRequestExecution clientHttpRequestExecution)
      throws IOException {
    ClientHttpResponse response;
    response = clientHttpRequestExecution.execute(httpRequest, bytes);
    if (watcherProperties.isRestTemplate()) {
      try {
        chaosMonkeyRequestScope.callChaosMonkey(
            ChaosTarget.REST_TEMPLATE, this.getClass().getSimpleName());
      } catch (final Exception exception) {
        try {
          if (exception.getClass().equals(assaultProperties.getException().getExceptionClass())) {
            response = new ErrorResponse();
          } else {
            throw exception;
          }
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return response;
  }

  static class ErrorResponse extends AbstractClientHttpResponse {

    static final String ERROR_TEXT = "This error is generated by Chaos Monkey for Spring Boot";
    static final String ERROR_BODY =
        "{\"error\": \"This is a Chaos Monkey for Spring Boot generated failure\"}";

    static final int[] ERROR_STATUS_CODES = {
      HttpStatus.INTERNAL_SERVER_ERROR.value(),
      HttpStatus.BAD_REQUEST.value(),
      HttpStatus.FORBIDDEN.value(),
      HttpStatus.UNAUTHORIZED.value(),
      HttpStatus.NOT_FOUND.value(),
    };

    @Override
    public int getRawStatusCode() {
      return ERROR_STATUS_CODES[new Random().nextInt(ERROR_STATUS_CODES.length)];
    }

    @Override
    public String getStatusText() {
      return ERROR_TEXT;
    }

    @Override
    public void close() {}

    @Override
    public InputStream getBody() throws IOException {
      InputStream inputStream =
          new ByteArrayInputStream(ERROR_BODY.getBytes(StandardCharsets.UTF_8));
      inputStream.close();
      return inputStream;
    }

    @Override
    public HttpHeaders getHeaders() {
      return new HttpHeaders();
    }
  }
}
