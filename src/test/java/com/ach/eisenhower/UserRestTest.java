package com.ach.eisenhower;

import jakarta.servlet.http.Cookie;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.is;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserRestTest extends EisenhowerBaseTest {
    @Test
    public void testRegister() throws Exception {
        mvc.perform(post("/auth/register")
                        .content("{ \"email\": \"test\", \"password\":\"test\" }")
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("token"))
                .andExpect(jsonPath("email", is("test")))
                .andExpect(jsonPath("token", anything()));
    }

    @Test
    public void testLoginWithCorrectCredentials() throws Exception {
        var email = RandomStringUtils.randomAlphabetic(8) + "@test.com";
        var password = RandomStringUtils.randomAlphabetic(8);
        userTestingUtils.createTestUser(email, password);

        mvc.perform(post("/auth/login")
                        .content("{ \"email\": \"" + email + "\", \"password\":\"" + password + "\" }")
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("token"))
                .andExpect(jsonPath("email", is(email)))
                .andExpect(jsonPath("token", anything()));
    }

    @Test
    public void testLoginWithWrongCredentials() throws Exception {
        var email = RandomStringUtils.randomAlphabetic(8) + "@test.com";
        var password = RandomStringUtils.randomAlphabetic(8);
        userTestingUtils.createTestUser(email, password);

        mvc.perform(post("/auth/login")
                        .content("{ \"email\": \"" + email + "\", \"password\":\"" + password + "wrong" + "\" }")
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("token"));
    }

    @Test
    public void testGetUserDetails() throws Exception {
        mvc.perform(get("/auth/user-details")
                        .cookie(new Cookie("token", userTestingUtils.getValidAccessToken()))
                        .secure(true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("roles", anything()));
    }
}
