package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.ActivityType;
import edu.stanford.slac.core_work_management.model.WorkType;
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
public class WorkTypeRepositoryTest {
    @Autowired
    private WorkTypeRepository workTypeRepository;
    @Autowired
    private MongoTemplate mongoTemplate;
    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), ActivityType.class);
    }

    @Test
    public void testEnsureActivity() {
        WorkType workType = WorkType.builder().title("Test").build();
        String newId = assertDoesNotThrow(
                ()-> workTypeRepository.ensureWorkType(workType)
        );
        assertThat(newId).isNotNull();
    }
    @Test
    public void testEnsureActivityTypeWithMultipleThreads() throws InterruptedException, ExecutionException {
        int numberOfThreads = 10; // Number of concurrent threads
        List<Future<String>> futures;
        try (ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads)) {
            List<Callable<String>> tasks = new ArrayList<>();
            WorkType workType = WorkType.builder().title("Test").build();

            for (int i = 0; i < numberOfThreads; i++) {
                tasks.add(() -> workTypeRepository.ensureWorkType(workType));
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
