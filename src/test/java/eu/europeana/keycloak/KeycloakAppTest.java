package eu.europeana.keycloak;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:keycloak.test.properties")
@SpringBootTest(classes = EmbeddedKeycloakApp.class)
public class KeycloakAppTest {

	@Test
	public void contextLoads() {
		// just do context loading
	}

}
