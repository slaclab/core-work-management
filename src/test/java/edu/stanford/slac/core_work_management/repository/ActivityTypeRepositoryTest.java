package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.api.v1.dto.NewActivityTypeDTO;
import edu.stanford.slac.core_work_management.model.ActivityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ActivityTypeRepositoryTest {
    @Autowired
    private ActivityTypeRepository activityTypeRepository;
    @Autowired
    private MongoTemplate mongoTemplate;
    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), ActivityType.class);
    }

    @Test
    public void testEnsureActivity() {
        ActivityType activityType = ActivityType.builder().title("Test").build();
        String newId = assertDoesNotThrow(
                ()-> activityTypeRepository.ensureActivityType(activityType)
        );
        assertThat(newId).isNotNull();
    }
    @Test
    public void testEnsureActivityTypeWithMultipleThreads() throws InterruptedException, ExecutionException {
        int numberOfThreads = 20; // Number of concurrent threads
        List<Future<String>> futures;
        try (ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads)) {
            List<Callable<String>> tasks = new ArrayList<>();
            ActivityType activityType = ActivityType.builder().title("Test").build();

            for (int i = 0; i < numberOfThreads*10; i++) {
                tasks.add(() -> activityTypeRepository.ensureActivityType(activityType));
            }

            futures = executorService.invokeAll(tasks);

            // Shut down the executor service
            executorService.shutdown();
        }

        // Assert that all threads received the same ID
        String expectedId = null;
        for (Future<String> future : futures) {
            String id = future.get();
            if (expectedId == null) {
                expectedId = id; // Set the expected ID from the first thread
            }
            assertThat(id).isEqualTo(expectedId);
        }
    }
}
