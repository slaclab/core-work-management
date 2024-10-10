package edu.stanford.slac.core_work_management.consumer;

import edu.stanford.slac.core_work_management.exception.WorkNotFound;
import edu.stanford.slac.core_work_management.model.ProcessWorkflowInfo;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import edu.stanford.slac.core_work_management.service.WorkService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Clock;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Log4j2
@Component
@AllArgsConstructor
public class ProcessingWorkflowUpdate {
    private final WorkRepository workRepository;
    private final WorkService workService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 2),
            kafkaTemplate = "workflowProcessingKafkaTemplate"
    )
    @KafkaListener(
            topics = "${edu.stanford.slac.core-work-management.workflow-processing-topic}",
            containerFactory = "workflowProcessingKafkaListenerContainerFactory"
    )
    public void processWorkWorkflow(
            ProcessWorkflowInfo processWorkflowInfo,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset
    ) throws RuntimeException, IOException {
        log.info("Start workflow processing for work: {}", processWorkflowInfo);
        // fetch the work object of the workflow updates
        try {
            var workToProcessOptional = wrapCatch(() -> workRepository.findByDomainIdAndId(processWorkflowInfo.getDomainId(), processWorkflowInfo.getWorkId()), -1);
            if (workToProcessOptional.isEmpty()) {
                log.error("Work with id {} not found", processWorkflowInfo.getWorkId());
                acknowledgment.acknowledge();
                return;
            }
            var workToProcess = workToProcessOptional.get();
            // we can proceed with the workflow update
            log.info("Processing workflow update for work: {}", workToProcess);
            // lastly we need to update the workflow
            workService.updateWorkWorkflow(workToProcess, null);

            // save the work with all the automatic update on the workflow
            workRepository.save(workToProcess);

            // after this work is update we need to update all the
            // tree up to the ancestor
            if (workToProcess.getParentWorkId() != null) {
                // find parent work
                var parentWork = wrapCatch(
                        () -> workRepository.findByDomainIdAndId(workToProcess.getDomainId(), workToProcess.getParentWorkId()).orElseThrow(
                                () -> WorkNotFound
                                        .notFoundById()
                                        .errorCode(-1)
                                        .workId(workToProcess.getParentWorkId())
                                        .build()
                        ),
                        -7
                );
                // try to find parent work type
                workService.updateParentWorkWorkflow(parentWork);
            }
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing workflow update for work: {}", processWorkflowInfo, e);
        }
    }
}
