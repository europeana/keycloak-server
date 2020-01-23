package eu.europeana.keycloak.password;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.keycloak.Config;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.PasswordHashProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class BCryptPasswordHashProviderFactory implements PasswordHashProviderFactory {

    private static final Logger LOG = LogManager.getLogger(BCryptPasswordHashProviderFactory.class);

    public static final String ID = "BCrypt";

    private static final int DEFAULT_LOG_ROUNDS = 13;
    private static final int MIN_LOG_ROUNDS = 4;
    private static final int MAX_LOG_ROUNDS = 31;

    private int logRounds = DEFAULT_LOG_ROUNDS;

    @Override
    public PasswordHashProvider create(KeycloakSession keycloakSession) {
        LOG.info("Creating BCryptPasswordHashProvider ...");
        return new BCryptPasswordHashProvider(ID, logRounds);
    }

    @Override
    public void init(Config.Scope scope) {
        LOG.info("Initialising BCryptPasswordHashProviderFactory ...");
        Integer configLogRounds = scope.getInt("log-rounds");
        if (configLogRounds != null && configLogRounds >= MIN_LOG_ROUNDS && configLogRounds <= MAX_LOG_ROUNDS) {
            logRounds = configLogRounds;
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return ID;
    }
}
