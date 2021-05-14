package de.codecentric.spring.boot.chaos.monkey.web.client;

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
import org.springframework.http.client.ClientHttpResponse;

public class RestTemplateErrorResponseAssault implements RestTemplateAssault {

  @Override
  public ClientHttpResponse process(HttpRequest httpRequest,
      ClientHttpRequestExecution clientHttpRequestExecution) {
    return new ErrorResponse();
  }

  static class ErrorResponse extends AbstractClientHttpResponse {

    //TODO: discuss text
    static final String ERROR_TEXT = "This error is generated by Chaos Monkey for Spring Boot";

    static final String ERROR_BODY = "{\"error\": \"This Chaos Monkey for Spring Boot generated failure\"}";

    //TODO: discuss which are valid responses status codes
    private static final int[] ERROR_STATUS_CODES = {
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
    public void close() {

    }

    @Override
    public InputStream getBody() throws IOException {
      ByteArrayInputStream inputStream = new ByteArrayInputStream(
          ERROR_BODY.getBytes(StandardCharsets.UTF_8));
      inputStream.close();
      return inputStream;
    }

    @Override
    public HttpHeaders getHeaders() {
      return new HttpHeaders();
    }
  }

}
