/* (C)2020 */
package ru.mail.jira.plugins.myteam.configuration;

import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import ru.mail.jira.plugins.commons.SentryClient;

public class ThirdPartyServiceInitializer implements LifecycleAware, DisposableBean {
  private static final Logger log = LoggerFactory.getLogger(ThirdPartyServiceInitializer.class);
  private static final String DSN =
      "http://488a7fa73b2343afb523a7139a7226fa:08f1454a8d514306a3cd2837cfc080a3@sentry.intdev.devmail.ru/19";

  @Override
  public void onStart() {
    log.info("Init SentryClient");
    SentryClient.init(DSN);

    log.info("Init Unirest");
    Unirest.setTimeouts(10_000, 300_000);
    Unirest.setObjectMapper(
        new ObjectMapper() {
          private org.codehaus.jackson.map.ObjectMapper jacksonObjectMapper =
              new org.codehaus.jackson.map.ObjectMapper();

          @Override
          public <T> T readValue(String value, Class<T> valueType) {
            try {
              return jacksonObjectMapper.readValue(value, valueType);
            } catch (IOException e) {
              log.error(
                  String.format(
                      "Unirest JacksonObjectMapper exception during reading value = %s as type = %s",
                      value, valueType.toString()),
                  e);
              throw new RuntimeException(e);
            }
          }

          @Override
          public String writeValue(Object value) {
            try {
              return jacksonObjectMapper.writeValueAsString(value);
            } catch (IOException e) {
              log.error(
                  String.format(
                      "Unirest JacksonObjectMapper exception during writing  value = %s ", value),
                  e);
              throw new RuntimeException(e);
            }
          }
        });
  }

  @Override
  public void onStop() {
    log.info("Stop SentryClient");
    SentryClient.close();

    try {
      log.info("Stop Unirest");
      Unirest.shutdown();
    } catch (IOException e) {
    }
  }

  @Override
  public void destroy() throws Exception {}
}
