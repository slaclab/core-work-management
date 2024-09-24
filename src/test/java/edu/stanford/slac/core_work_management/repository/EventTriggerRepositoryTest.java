package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.EventTrigger;
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

import java.time.LocalDateTime;


import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EventTriggerRepositoryTest {
    @Autowired
    EventTriggerRepository eventTriggerRepository;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), EventTrigger.class);
    }

    @Test
    public void testFired() {
        // Step 1: Create buckets with specific 'from' and 'to' dates
        int processingTimeoutSeconds = 10;
        LocalDateTime event1FireDate = LocalDateTime.of(2021, 1, 1, 8, 0);
        LocalDateTime event2FireDate = LocalDateTime.of(2021, 1, 1, 13, 0);

        EventTrigger evtTypeA1 = eventTriggerRepository.save(
                EventTrigger.builder()
                        .typeName("type-a")
                        .eventFireTimestamp(event1FireDate)
                        .build()
        );

        EventTrigger evtTypeA2 = eventTriggerRepository.save(
                EventTrigger.builder()
                        .typeName("type-a")
                        .eventFireTimestamp(event2FireDate)
                        .build()
        );

        assertThat(evtTypeA1).isNotNull();
        assertThat(evtTypeA2).isNotNull();

        // Step 2: Define 'currentDate' and 'processingTimeoutMinutes'
        LocalDateTime currentDate = LocalDateTime.of(2021, 1, 1, 11, 0);

        // Step 3: Invoke the function under test
        EventTrigger eventToProcess = eventTriggerRepository.findNextToProcess("type-a",currentDate, currentDate.plusSeconds(processingTimeoutSeconds));

        // Step 4: Assert the results
        assertThat(eventToProcess).isNotNull();
        assertThat(eventToProcess.getId()).isEqualTo(evtTypeA1.getId());

        // get with the same date should return false
        eventToProcess = eventTriggerRepository.findNextToProcess("type-a",currentDate, currentDate.plusSeconds(processingTimeoutSeconds));
        assertThat(eventToProcess).isNull();

        /// advance to a date that is after the bucket expired for processing
        var currentCheckDate = currentDate.plusSeconds(processingTimeoutSeconds);
        EventTrigger bucketToStartAgain = eventTriggerRepository.findNextToProcess("type-a",currentCheckDate, currentCheckDate.plusSeconds(processingTimeoutSeconds));
        assertThat(bucketToStartAgain).isNotNull();

        // now tag as completed
        assertDoesNotThrow(()->eventTriggerRepository.completeProcessing("type-a", evtTypeA1.getId()));

        // now it doesn't need to be selected again
        bucketToStartAgain = eventTriggerRepository.findNextToProcess("type-a", currentDate, currentCheckDate.plusSeconds(processingTimeoutSeconds));
        assertThat(bucketToStartAgain).isNull();

        // going to the next date and use a different event type shouldn't return anything
        LocalDateTime secondEvenTriggerDate = LocalDateTime.of(2021, 1, 1, 14, 0);
        var notFoundEvent = eventTriggerRepository.findNextToProcess("type-b", currentDate, currentCheckDate.plusSeconds(processingTimeoutSeconds));
        assertThat(notFoundEvent).isNull();
    }

    @Test
    public void testFiredFromInterleavedType() {
        // Step 1: Create buckets with specific 'from' and 'to' dates
        int processingTimeoutSeconds = 10;
        LocalDateTime event1FireDate = LocalDateTime.of(2021, 1, 1, 8, 0);
        LocalDateTime event2FireDate = LocalDateTime.of(2021, 1, 1, 13, 0);

        EventTrigger evtTypeA1 = eventTriggerRepository.save(
                EventTrigger.builder()
                        .typeName("type-a")
                        .eventFireTimestamp(event1FireDate)
                        .build()
        );

        EventTrigger evtTypeB1 = eventTriggerRepository.save(
                EventTrigger.builder()
                        .typeName("type-b")
                        .eventFireTimestamp(event2FireDate)
                        .build()
        );

        EventTrigger evtTypeA2 = eventTriggerRepository.save(
                EventTrigger.builder()
                        .typeName("type-a")
                        .eventFireTimestamp(event2FireDate)
                        .build()
        );

        assertThat(evtTypeA1).isNotNull();
        assertThat(evtTypeA2).isNotNull();
        assertThat(evtTypeB1).isNotNull();

        // Step 2: Define 'currentDate' and 'processingTimeoutMinutes'
        LocalDateTime currentDate = LocalDateTime.of(2021, 1, 1, 11, 0);

        // Step 3: Invoke the function under test
        EventTrigger eventToProcess = eventTriggerRepository.findNextToProcess("type-a",currentDate, currentDate.plusSeconds(processingTimeoutSeconds));

        // Step 4: Assert the results
        assertThat(eventToProcess).isNotNull();
        assertThat(eventToProcess.getId()).isEqualTo(evtTypeA1.getId());

        // get with the same date should return false
        eventToProcess = eventTriggerRepository.findNextToProcess("type-a",currentDate, currentDate.plusSeconds(processingTimeoutSeconds));
        assertThat(eventToProcess).isNull();

        // now tag as completed
        assertDoesNotThrow(()->eventTriggerRepository.completeProcessing("type-a", evtTypeA1.getId()));

        // now it doesn't need to be selected again
        /// advance to a date that could fire both A2 and B1
        var nextEventDate = LocalDateTime.of(2021, 1, 1, 14, 0);
        var eventA2Triggered = eventTriggerRepository.findNextToProcess("type-a", nextEventDate, nextEventDate.plusSeconds(processingTimeoutSeconds));
        assertThat(eventA2Triggered).isNotNull();
        assertThat(eventA2Triggered.getId()).isEqualTo(evtTypeA2.getId());

        // now tag as completed
        assertDoesNotThrow(()->eventTriggerRepository.completeProcessing("type-a", evtTypeA2.getId()));

        // going to the next date and use a different event type shouldn't return anything
        var farCheckDate = LocalDateTime.of(2021, 1, 2, 14, 0);
        var notFoundEvent = eventTriggerRepository.findNextToProcess("type-a", farCheckDate, farCheckDate.plusSeconds(processingTimeoutSeconds));
        assertThat(notFoundEvent).isNull();

        // check that all event of type-a have been processed
        var allEvent = eventTriggerRepository.findAll();
        assertThat(allEvent).hasSize(3);
        assertThat(allEvent).filteredOn(EventTrigger::getFired).allMatch(a->a.getTypeName().equals("type-a"));
    }
}
