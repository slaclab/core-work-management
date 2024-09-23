package edu.stanford.slac.core_work_management.task;

import edu.stanford.slac.core_work_management.api.v1.dto.BucketSlotDTO;
import edu.stanford.slac.core_work_management.config.CWMAppProperties;
import edu.stanford.slac.core_work_management.model.ProcessWorkflowInfo;
import edu.stanford.slac.core_work_management.service.BucketService;
import edu.stanford.slac.core_work_management.service.WorkService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Log4j2
@Component
@AllArgsConstructor
public class ManageBucketWorkflowUpdate {
    private final CWMAppProperties cwmAppProperties;
    private final WorkService workService;
    private final BucketService bucketService;

    private final TransactionTemplate transactionTemplate;
    private final KafkaTemplate<String, ProcessWorkflowInfo> processWorkflowInfo;

    @Scheduled(fixedDelay = 60000)
    public void processStartAndStop() {
        // Start transaction
        transactionTemplate.execute(status -> {
            processBucketStartEvent();
            return null;
        });

        // Stop transaction
        transactionTemplate.execute(status -> {
            processBucketStopEvent();
            return null;
        });
    }

    /**
     * This method is used to process the bucket start event
     * find all bucket that are started(date from) when this method is executed
     * but never processed and find all the work associated to each one to
     * update the workflow status
     */

    public void processBucketStartEvent() {
        log.info("Check which bucket need to be started");
        BucketSlotDTO selectedBucket = null;
        var now = LocalDateTime.now();
        // use the bucket service to find the next bucket to start
        while ((selectedBucket = bucketService.findNextBucketToStart(now, now.minusSeconds(30))) != null) {
            processBucket(selectedBucket);
            // set the bucket as completed
            bucketService.completeStartEventProcessing(selectedBucket.id());
        }
        log.info("Startup bucket completed");
    }

    /**
     * This method is used to process the bucket stop event
     * find all bucket that are stopped(date to) when this method is executed
     * but never processed and find all the work associated to each one to
     * update the workflow status
     */
    public void processBucketStopEvent() {
        log.info("Check which bucket need to be stopped");
        BucketSlotDTO selectedBucket = null;
        var now = LocalDateTime.now();
        // use the bucket service to find the next bucket to start
        while ((selectedBucket = bucketService.findNextBucketToStop(now, now.minusSeconds(30))) != null) {
            processBucket(selectedBucket);
            bucketService.completeStopEventProcessing(selectedBucket.id());
        }
        log.info("Stop bucket completed");
    }

    /**
     * This method is used to process the bucket
     * find all the work associated to the bucket and send to kafka
     * the workflow update
     *
     * @param selectedBucket the bucket to process
     */
    private void processBucket(BucketSlotDTO selectedBucket) {
        log.info("Processing bucket {}", selectedBucket);
        // find work that belong to the bucket
        var allWorkToProcess = workService.findAllByBucketId(selectedBucket.id());
        log.info("Found {} work to process", allWorkToProcess.size());
        // to process send to kafka the bucket to start
        allWorkToProcess.forEach(
                work -> {
                    log.info("Processing work with id:{} and did:{}", work.domain().id(), work.id());
                    processWorkflowInfo.send(
                            // topic
                            cwmAppProperties.getWorkflowProcessingTopic(),
                            // key
                            "%s/%s".formatted(work.domain().id(), work.id()),
                            // value
                            ProcessWorkflowInfo.builder().domainId(work.domain().id()).workId(work.id()).build()
                    );
                }
        );
    }

}
