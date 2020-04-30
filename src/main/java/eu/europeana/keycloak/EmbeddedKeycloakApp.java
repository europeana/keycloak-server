package eu.europeana.keycloak;

import com.jayway.jsonpath.JsonPath;
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

    private static final Logger LOG   = LogManager.getLogger(EmbeddedKeycloakApp.class);

    @Value("${keycloak.manager-client.id}")
    private String managerId;

    @Value("${keycloak.manager-client.secret}")
    private String managerSecret;

    @Value("${keycloak.warmup.request}")
    private String warmupRequest;

    @Value("${keycloak.forward.token-service}")
    private String tokenBasePath;

    public static void main(String[] args) {
        SpringApplication.run(EmbeddedKeycloakApp.class, args);
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> onApplicationReadyEventListener(ServerProperties serverProperties) {
        return evt -> {
            Integer port = serverProperties.getPort();
            String rootContextPath = serverProperties.getContextPath();
            String keycloakContextPath = StaticPropertyUtil.getContextPath();

            if (areWarmUpPropertiesSet(managerId, managerSecret, warmupRequest)){
                Runnable warmup = () -> doWarmup(port);
                // run this as a separate thread so start-up can finish (even in case of errors during warm-up)
                Thread thread = new Thread(warmup);
                thread.start();
            } else {
                LOG.info("Skipping warmup because not all necessary properties have been set");
            }
            LOG.info("Embedded Keycloak started: http://localhost:{}{}{} to use keycloak", port, rootContextPath, keycloakContextPath);
        };
    }

    private void doWarmup(Integer port) {
        LOG.info("Starting warm-up...");
        long startTime = System.currentTimeMillis();
        String host = "http://localhost:" + port;
        String token = getToken(host + tokenBasePath + "/token", startTime);

        if (StringUtils.isNotEmpty(token)) {
            HttpHeaders listClientHeaders = new HttpHeaders();
            listClientHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            listClientHeaders.add("Authorization", "Bearer " + token);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Warm-up request = {}", host + warmupRequest);
            }
            ResponseEntity<String> warmUpResponse = new RestTemplate().exchange(
                    host + warmupRequest, HttpMethod.GET, new HttpEntity<String>(listClientHeaders), String.class);

            if (warmUpResponse.getStatusCode() != HttpStatus.OK) {
                LOG.error("Warm-up failed after {} seconds with HTTP status {} ",
                        (System.currentTimeMillis() - startTime)/1000, warmUpResponse.getStatusCode());
            } else {
                LOG.info("Warm-up completed after {} seconds with HTTP status {}",
                        (System.currentTimeMillis() - startTime)/1000, warmUpResponse.getStatusCode());
            }
        }
    }

    private boolean areWarmUpPropertiesSet(String... requiredProperties){
        return StringUtils.isNoneBlank(requiredProperties) && !StringUtils.equalsAnyIgnoreCase("REMOVED", requiredProperties);
    }

    private String getToken(String path, long startTime) {
        RestTemplate tokenTemplate = new RestTemplate();
        HttpHeaders  tokenHeaders  = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", managerId);
        map.add("client_secret", managerSecret);
        map.add("grant_type", "client_credentials");
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, tokenHeaders);
        LOG.debug("Token request = {}", path);
        ResponseEntity<String> tokenResponse = tokenTemplate.postForEntity(path, request , String.class);

        LOG.info("Access token retrieved in {} ms with HTTP status {}",
                (System.currentTimeMillis() - startTime), tokenResponse.getStatusCode());

        if (tokenResponse.getStatusCode() != HttpStatus.OK) {
            LOG.error("Could not obtain access token");
            return null;
        }
        return JsonPath.read(tokenResponse.getBody(), "access_token");
    }
}
