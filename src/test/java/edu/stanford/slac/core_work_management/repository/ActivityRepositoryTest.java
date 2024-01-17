package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.Activity;
import edu.stanford.slac.core_work_management.model.ActivityStatus;
import edu.stanford.slac.core_work_management.model.ActivityStatusLog;
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

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ActivityRepositoryTest {
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Activity.class);
    }

    @Test
    public void testRetrieveAllActivityStatusForTheSameWorkId() {
        activityRepository.save(
                Activity
                        .builder()
                        .title("Update the documentation")
                        .currentStatus(ActivityStatusLog
                                .builder()
                                .status(ActivityStatus.New)
                                .build())
                        .workId("1")
                        .build()
        );
        activityRepository.save(
                Activity
                        .builder()
                        .title("Update the software")
                        .currentStatus(ActivityStatusLog
                                .builder()
                                .status(ActivityStatus.Roll)
                                .build())
                        .workId("1")
                        .build()
        );

        List<ActivityStatusLog> allStatus = activityRepository.findAllActivityStatusByWorkId("1");

        assertThat(allStatus)
                .isNotNull()
                .hasSize(2)
                .extracting(ActivityStatusLog::getStatus)
                .contains(ActivityStatus.New, ActivityStatus.Roll);
    }
}
