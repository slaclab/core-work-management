package edu.stanford.slac.core_work_management.task;

import edu.stanford.slac.core_work_management.config.CWMAppProperties;
import edu.stanford.slac.core_work_management.model.EventTrigger;
import edu.stanford.slac.core_work_management.model.ProcessWorkflowInfo;
import edu.stanford.slac.core_work_management.repository.EventTriggerRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Log4j2
@Component
@AllArgsConstructor
public class ManageWorkflowUpdateByEventTrigger {
    private final Clock clock;
    private final EventTriggerRepository eventTriggerRepository;
    private final CWMAppProperties cwmAppProperties;
    private final KafkaTemplate<String, ProcessWorkflowInfo> processWorkflowInfoKafkaTemplate;
    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void processTriggeredEvent() {
        log.info("Check which bucket need to be started");
        EventTrigger selectedEvent = null;
        var now = LocalDateTime.now(clock);
        // use the bucket service to find the next bucket to start
        while ((selectedEvent = eventTriggerRepository.findNextToProcess("workPlannedStart", now, now.minusSeconds(30))) != null) {
            if(selectedEvent.getPayload() == null || !selectedEvent.getPayload().getClass().isAssignableFrom(ProcessWorkflowInfo.class)){
                log.error("Invalid payload for event trigger: {}", selectedEvent);
                // set the bucket as completed
                eventTriggerRepository.completeProcessing("workPlannedStart", selectedEvent.getId());
                continue;
            }
            // process the bucket
            ProcessWorkflowInfo processWorkflowInfo = (ProcessWorkflowInfo) selectedEvent.getPayload();
            processWorkflowInfoKafkaTemplate.send(
                    // topic
                    cwmAppProperties.getWorkflowProcessingTopic(),
                    // key
                    "%s/%s".formatted(processWorkflowInfo.getDomainId(), processWorkflowInfo.getWorkId()),
                    // value
                    ProcessWorkflowInfo.builder().domainId(processWorkflowInfo.getDomainId()).workId(processWorkflowInfo.getWorkId()).build()
            ).thenAccept(result -> {
                // Handle successful send
                System.out.println("Message sent successfully: " + result.getRecordMetadata());
            }).exceptionally(ex -> {
                // Handle exception in sending
                System.err.println("Message send failed: " + ex.getMessage());
                return null; // Exceptionally requires a return value, hence null
            });;
            // set the bucket as completed
            eventTriggerRepository.completeProcessing("work", selectedEvent.getId());
        }
        log.info("Startup bucket completed");
    }
}
