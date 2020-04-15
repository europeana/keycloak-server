package eu.europeana.keycloak;

import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.util.*;

/**
 * Main application
 */
@SpringBootApplication(exclude = LiquibaseAutoConfiguration.class)
@EnableConfigurationProperties(KeycloakServerProperties.class)
@PropertySource(value = "classpath:build.properties")
@PropertySource(value = "classpath:keycloak.properties")
@PropertySource(value = "classpath:keycloak-user.properties", ignoreResourceNotFound = true)
public class EmbeddedKeycloakApp {

    @Value("${keycloak.token-path}")
    private String tokenPath;

    @Value("${keycloak.list-clients-path}")
    private String listClientsPath;

    @Value("${keycloak.server.curl-client-id}")
    private String curlClientId;

    @Value("${keycloak.server.curl-client-secret}")
    private String curlClientSecret;

    @Value("${keycloak.server.list-client-delay}")
    private long listClientDelay;

    private static final Logger LOG   = LogManager.getLogger(EmbeddedKeycloakApp.class);

    public static void main(String[] args) {
        SpringApplication.run(EmbeddedKeycloakApp.class, args);
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> onApplicationReadyEventListener(ServerProperties serverProperties) {
        return evt -> {
            Integer port = serverProperties.getPort();
            String rootContextPath = serverProperties.getContextPath();
            String keycloakContextPath = StaticPropertyUtil.getContextPath();
            if (areAutowarmPropertiesSet(tokenPath, listClientsPath, curlClientId, curlClientSecret)){
                triggerClientLoading("http://localhost:" + port);
            } else {
                LOG.info("Skipping autowarming because not all necessary properties have been set");
            }
            LOG.info("Embedded Keycloak started: http://localhost:{}{}{} to use keycloak", port, rootContextPath, keycloakContextPath);
        };
    }

    public void triggerClientLoading(String kcBasePath) {
        TimerTask task = new TimerTask() {
            public void run() {

                LOG.info("Client list request triggered {} sec after Keycloak started, at {}"
                        , String.format("%.3f", listClientDelay / 1000f)
                        , new Date());

                long         t1 = System.currentTimeMillis();
                RestTemplate tokenTemplate = new RestTemplate();
                final String tokenUrl      = kcBasePath + tokenPath;
                HttpHeaders  tokenHeaders  = new HttpHeaders();
                tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
                map.add("client_id", curlClientId);
                map.add("client_secret", curlClientSecret);
                map.add("grant_type", "client_credentials");
                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, tokenHeaders);
                ResponseEntity<String> tokenResponse = tokenTemplate.postForEntity(
                        tokenUrl, request , String.class);

                LOG.info("Token for autowarming request retrieved in {} milliseconds with HTTP status {}"
                        , String.format("%.3f", (System.currentTimeMillis() - t1) / 1000f)
                        , tokenResponse.getStatusCode());

                if (tokenResponse.getStatusCode() != HttpStatus.OK) {
                    LOG.error("Could not obtain token to trigger autowarming");
                } else {

                    String token = JsonPath.read(tokenResponse.getBody(), "access_token");
                    final String listClientsUrl     = kcBasePath + listClientsPath;
                    HttpHeaders  listClientHeaders  = new HttpHeaders();
                    listClientHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                    listClientHeaders.add("Authorization", "Bearer " + token);
                    ResponseEntity<String> clientsListresponse = new RestTemplate().exchange(
                            listClientsUrl, HttpMethod.GET, new HttpEntity<String>(listClientHeaders),
                            String.class);

                    if (clientsListresponse.getStatusCode() != HttpStatus.OK ) {
                        LOG.error("The Keycloak listClients request used for autowarming returned HTTP status {} " +
                                "after {} seconds, indicating that the autowarming after startup has probably failed"
                                , clientsListresponse.getStatusCode()
                                , String.format("%.3f", (System.currentTimeMillis() - t1) / 1000f));
                    } else {
                        LOG.info("Autowarming completed after {} seconds with HTTP status {}"
                                , String.format("%.3f", (System.currentTimeMillis() - t1) / 1000f)
                                , clientsListresponse.getStatusCode());
                    }
                }
            }
        };
        Timer timer = new Timer("ListClientTrigger");
        timer.schedule(task, listClientDelay);
    }

    private boolean areAutowarmPropertiesSet(String... awProperties){
        return StringUtils.isNoneBlank(awProperties) && !StringUtils.equalsAnyIgnoreCase("REMOVED", awProperties);
    }
}
