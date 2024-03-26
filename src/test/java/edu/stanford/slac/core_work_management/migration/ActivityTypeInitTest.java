package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.core_work_management.model.ActivityType;
import edu.stanford.slac.core_work_management.model.ActivityTypeCustomField;
import edu.stanford.slac.core_work_management.model.LOVElement;
import edu.stanford.slac.core_work_management.model.WorkType;
import edu.stanford.slac.core_work_management.repository.ActivityTypeRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import edu.stanford.slac.core_work_management.service.LOVService;
import edu.stanford.slac.core_work_management.service.WorkService;
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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ActivityTypeInitTest {
    @Autowired
    private LOVService lovService;
    @Autowired
    private WorkService workService;
    @Autowired
    private ActivityTypeRepository activityTypeRepository;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), ActivityType.class);
        mongoTemplate.remove(new Query(), LOVElement.class);
    }

    @Test
    public void initTest() {
        InitActivityType initWorkType = new InitActivityType(lovService, workService, activityTypeRepository);
        assertDoesNotThrow(initWorkType::changeSet);
        var allActivityType = assertDoesNotThrow(
                () -> activityTypeRepository.findAll()
        );
        assertThat(allActivityType)
                .isNotEmpty()
                .extracting(ActivityType::getTitle).containsExactly(
                        "General Task",
                        "Software Task",
                        "Hardware Task"
                );
        // checks general activity custom fields
        assertThat(allActivityType.getFirst().getCustomFields())
                .isNotEmpty()
                .extracting(ActivityTypeCustomField::getName)
                .containsExactlyInAnyOrder(
                        "Task Priority",
                        "Task Skill Set",
                        "Percentage completed",
                        "Module",
                        "Old Serial Number",
                        "New Serial Number",
                        "Drawing",
                        "Doc Solution",
                        "Date RTC Checked",
                        "Feedback Priority"
                );
        // checks software activity custom fields
        assertThat(allActivityType.get(1).getCustomFields())
                .isNotEmpty()
                .extracting(ActivityTypeCustomField::getName)
                .containsExactlyInAnyOrder(
                        "Scheduling Priority",
                        "Time Comments",
                        "Access Requirements",
                        "Other Issues",
                        "Beam Requirements",
                        "Beam Comment",
                        "Invasive",
                        "Invasive Comment",
                        "Test Plan",
                        "Backout Plan",
                        "Systems Required",
                        "Systems Affected",
                        "Risk/Benefit",
                        "Dependencies",
                        "CD Review Date"
                );
// check hardware activity custom fields
        assertThat(allActivityType.get(2).getCustomFields())
                .isNotEmpty()
                .extracting(ActivityTypeCustomField::getName)
                .containsExactlyInAnyOrder(
                        "Scheduling Priority",
                        "Access Requirements",
                        "Other Issues",
                        "Rad Safety Work Ctl Form",
                        "Lock and Tag",
                        "PPS Interlocked",
                        "Atmospheric Work Control Form",
                        "Electric Sys Work Ctl Form",
                        "Additional Safety Information",
                        "Specify Requirements",
                        "Release Conditions Defined",
                        "Safety Issue",
                        "Number of Persons",
                        "Ongoing",
                        "Minimum Hours",
                        "Person Hours",
                        "Toco Time",
                        "Feedback Priority",
                        "Beam Requirements",
                        "Beam Comment",
                        "Invasive",
                        "Invasive Comment",
                        "Misc Job Comments",
                        "Feedback Priority",
                        "Feedback Priority Comment",
                        "Micro",
                        "Primary",
                        "Micro Other",
                        "Visual Number"
                );

    }
}
