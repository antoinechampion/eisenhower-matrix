package com.ach.eisenhower;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = ServerApplication.class)
@Import(TestConfiguration.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureMockMvc
@EnableAutoConfiguration()
@AutoConfigureTestDatabase
public abstract class EisenhowerBaseTest {
    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected UserTestingUtil userTestingUtils;
    @Autowired
    private TestCleaningUtil testCleaningUtil;

    @AfterEach
    public void cleanDb() {
        testCleaningUtil.cleanNotes();
        testCleaningUtil.cleanBoards();
        testCleaningUtil.cleanUsers();
    }
}