package eu.europeana.keycloak;

import eu.europeana.keycloak.web.ForwardController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the redirect controller
 * @author Patrick Ehlert
 * Created on Feb 26, 2020
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(ForwardController.class)
public class ForwardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    DataSource datasource; // needed by EmbeddedKeycloakConfig

    @Value("${keycloak.forward.account-service}")
    private String loginEndpoint;

    @Value("${keycloak.forward.token-service}")
    private String tokenEndpoint;

    /**
     * Test redirecting of GET request to /login
     */
    @Test
    public void testLoginGet() throws Exception {
        testForwardGet("/login", loginEndpoint);
    }

    private void testForwardGet(String path, String expectedPath) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl(expectedPath));
    }

    private void testForwardPost(String path, String expectedPath) throws Exception {
        mockMvc.perform(post(path))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl(expectedPath));
    }

    /**
     * Test redirecting of GET requests to various /oidc endpoints
     */
    @Test
    public void testOidcGet() throws Exception {
        String test1 = "/certs";
        testForwardGet(ForwardController.BASE_PATH_OIDC + test1, tokenEndpoint + test1);

        String test2 = "/logout";
        testForwardGet(ForwardController.BASE_PATH_OIDC + test2, tokenEndpoint + test2);
    }

    /**
     * Test if request parameters are included in the forward
     *
     * NOTE: when redirecting this is useful test because it's easy to forget enabling the option that handles this, but
     * for forwarding it seems there is no way to check if it works properly as the parameter is automatically stripped
     * from the path and not available in the MockMvc response anymore (but it does work!)
     */
    @Test
    public void testOidcGetParams() throws Exception {
        String test = "/login-status-iframe.html";
//        testForwardGet(ForwardController.BASE_PATH_OIDC + test + "?extraParam=true",
//                tokenEndpoint + test + "?extraParam=true");

        // it seems we cannot retrieve parameters anymore after forwarding
        testForwardGet(ForwardController.BASE_PATH_OIDC + test + "?extraParam=true",
                tokenEndpoint + test);
    }

    /**
     * Test that we don't forward GET requests that we don't support
     */
    @Test
    public void testOidcGetNotFound() throws Exception {
        mockMvc.perform(get(ForwardController.BASE_PATH_OIDC + "/notsupported"))
                .andExpect(status().isNotFound());
    }

    /**
     * Test redirecting of POST requests to various /oidc endpoints
     */
    @Test
    public void testOidcPost() throws Exception {
        String test1 = "/auth";
        testForwardPost(ForwardController.BASE_PATH_OIDC + test1, tokenEndpoint + test1);

        String test2 = "/token/introspect";
        testForwardPost(ForwardController.BASE_PATH_OIDC + test2, tokenEndpoint + test2);
    }

    @Test
    public void testOidcPostNotFound() throws Exception {
        mockMvc.perform(post(ForwardController.BASE_PATH_OIDC + "/notsupported"))
                .andExpect(status().isNotFound());
    }

    /**
     * Test if posted form data is included in the forward.
     *
     * NOTE: when redirecting this is can be a useful test because it seems redirecting does not allow you to including
     * the post data in the redirected request. For forwarding it seems there is no way to check if it works properly
     * as the parameters are not available in the MockMvc response (but it does work!)
     */
    @Test
    public void testOidcPostFormData() throws Exception {
        String path = "/token";
        mockMvc.perform(post(ForwardController.BASE_PATH_OIDC + path)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                // when posting using curl (e.g. curl -X POST http://localhost:8080/oidc/token -d 'grant_type=password' -d 'username=test')
                // then the request content is empty and the data is automatically converted to parameters by Spring
                // so to mimic this we use parameters as well
                .param("grant_type", "password")
                .param("username", "test"))
                // alternatively (see also https://stackoverflow.com/questions/36568518/testing-form-posts-through-mockmvc) we can use:
                //.content(EntityUtils.toString(new UrlEncodedFormEntity(Arrays.asList(
                //        new BasicNameValuePair("grant_type", "password"),
                //        new BasicNameValuePair("username", "test"))))))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl(tokenEndpoint + path))
                // it seems we cannot retrieve and check the content/parameters anymore after forwarding
                .andExpect(content().string(""));
    }

}
