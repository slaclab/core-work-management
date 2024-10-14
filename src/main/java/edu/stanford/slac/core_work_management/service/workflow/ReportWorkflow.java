package edu.stanford.slac.core_work_management.service.workflow;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateWorkDTO;
import edu.stanford.slac.core_work_management.model.UpdateWorkflowState;
import edu.stanford.slac.core_work_management.model.WATypeCustomField;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkType;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import edu.stanford.slac.core_work_management.service.ShopGroupService;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

@Workflow(
        name = "Report",
        description = "The workflow for a report"
)
@Component("ReportWorkflow")
@AllArgsConstructor
public class ReportWorkflow extends BaseWorkflow {
    // This map defines the valid transitions for each state
    @PostConstruct
    public void init() {
        validTransitions = Map.of(
                // Rule:one or more child are in ReadyForWork
                WorkflowState.Created, Set.of(WorkflowState.Scheduled),
                // Rule: one or more child are in InProgress
                WorkflowState.Scheduled, Set.of(WorkflowState.InProgress),
                // Rule: All child requests or records must be completed before transitioning.
                // If a new child request/record is added after all others are completed, return to the "In Progress" state.
                WorkflowState.InProgress, Set.of(WorkflowState.ReviewToClose),
                // Rule: Admin manually reviews and sets the work to "Review to Close" if no new child requests/records are added.
                WorkflowState.ReviewToClose, Set.of(WorkflowState.Closed)
        );
    }
}
