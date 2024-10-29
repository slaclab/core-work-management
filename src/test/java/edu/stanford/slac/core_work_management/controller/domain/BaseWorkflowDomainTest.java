package edu.stanford.slac.core_work_management.controller.domain;

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.service.HelperService;
import edu.stanford.slac.core_work_management.task.ManageBucketWorkflowUpdate;
import edu.stanford.slac.core_work_management.task.ManageWorkflowUpdateByEventTrigger;
import jakarta.validation.constraints.NotEmpty;
import org.assertj.core.api.AssertionsForClassTypes;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseWorkflowDomainTest {
    private final HelperService helperService;
    private final ManageBucketWorkflowUpdate manageBucketWorkflowUpdate;
    private final ManageWorkflowUpdateByEventTrigger manageWorkflowUpdateByEventTrigger;
    public BaseWorkflowDomainTest(HelperService helperService, ManageBucketWorkflowUpdate manageBucketWorkflowUpdate, ManageWorkflowUpdateByEventTrigger manageWorkflowUpdateByEventTrigger) {
        this.helperService = helperService;
        this.manageBucketWorkflowUpdate = manageBucketWorkflowUpdate;
        this.manageWorkflowUpdateByEventTrigger = manageWorkflowUpdateByEventTrigger;
    }

    abstract public DomainTestInfo init();
    abstract public void clean(DomainTestInfo domainTestInfo);

    /**
     * Get the custom field id by name
     *
     * @param workTypeDTO     the work type dto
     * @param customFieldName the custom field name
     * @return the custom field id
     */
    public @NotEmpty String getCustomFileIdByName(WorkTypeDTO workTypeDTO, String customFieldName) {
        return workTypeDTO.customFields().stream()
                .filter(customField -> customField.name().compareToIgnoreCase(customFieldName) == 0)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Custom field not found"))
                .id();
    }

    /**
     * Get the work lov value id by group name and index
     *
     * @param workDTO      the work dto
     * @param fieldName    the field name
     * @param elementIndex the element index
     * @return the work lov value id by group name and index
     * @throws Exception the exception
     */
    public @NotEmpty(message = "The value is mandatory field") String
    getWorkLovValueIdByGroupNameAndIndex(WorkDTO workDTO, String fieldName, int elementIndex) throws Exception {
        assertThat(workDTO).isNotNull();
        return workDTO.workType().customFields().stream()
                .filter(customField -> customField.name().compareToIgnoreCase(fieldName) == 0)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Custom field not found"))
                .lovValues()
                .get(elementIndex)
                .id();
    }

    public @NotEmpty(message = "The value is mandatory field") String
    getWorkLovValueIdByGroupNameAndIndex(WorkTypeDTO workTypeDTO, String fieldName, int elementIndex) throws Exception {
        assertThat(workTypeDTO).isNotNull();
        var lovElementDTOs = helperService.getCustomFiledLOVValue(LOVDomainTypeDTO.Work, workTypeDTO.domainId(), workTypeDTO.id(), fieldName);
        assertThat(lovElementDTOs).isNotNull();
        return lovElementDTOs.get(elementIndex).id();
    }

    public boolean checkWorkflowStatus(String domainId, String workId, WorkflowStateDTO status) {
       return helperService.checkStatusOnWork(domainId, workId, status);
    }

    public String toString(LocalDateTime date) {
        return helperService.toString(date);
    }


    /**
     * Process the pending request at the given date and wait for the work to be in progress
     * @param processingDate the processing date
     * @param workId the request id
     */
    public void processPendingRequestAtDateAndWait(Clock clock, LocalDateTime processingDate, String domainId, String workId) {
        // now advance the day up the planned start date expiration
        when(clock.instant()).thenReturn(processingDate.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        // Now when LocalDateTime.now() is called, it returns the fixed time
        // simulate the bucket fire event
        manageBucketWorkflowUpdate.processStartAndStop();
        // process the event trigger
        manageWorkflowUpdateByEventTrigger.processTriggeredEvent();
        await()
                .atMost(20, SECONDS)
                .pollInterval(1, SECONDS)
                .until(
                        () -> {
                            return checkWorkflowStatus(domainId, workId, WorkflowStateDTO.InProgress);
                        }
                );

    }
}
