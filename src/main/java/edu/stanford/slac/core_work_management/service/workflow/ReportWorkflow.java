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
    private final WorkRepository workRepository;
    private final ShopGroupService shopGroupService;

    // This map defines the valid transitions for each state
    @PostConstruct
    public void init() {
        validTransitions = Map.of(
                // Rule: AssignedTo != null
                WorkflowState.Created, Set.of(WorkflowState.ReadyForWork),
                // Rule: (wait for attachment on radiation control form if it is required) and when admin set workflow to = "Ready to work"
                WorkflowState.PendingApproval, Set.of(WorkflowState.ReadyForWork),
                // Rule: all required paperwork attached and admin set directly the status to "Approved"
                WorkflowState.ReadyForWork, Set.of(WorkflowState.Approved),
                // Rule: when the start date is reached or user automatically set the state in progress
                WorkflowState.Approved, Set.of(WorkflowState.InProgress),
                // Rule: In Progress state becomes active based on start date and time of scheduled activity
                WorkflowState.InProgress, Set.of(WorkflowState.WorkComplete),
                // Rule: user/admin manually se the work to closed
                WorkflowState.WorkComplete, Set.of(WorkflowState.ReviewToClose),
                // Rule: When all activities have ended admin can close the work
                WorkflowState.ReviewToClose, Set.of(WorkflowState.Closed)
        );
    }

    @Override
    public void canUpdate(String identityId, Work work) {
    }


    @Override
    public boolean isCompleted(Work work) {
        return false;
    }

    @Override
    public Set<WorkflowState> permittedStatus(Work work) {
        return Set.of();
    }
}
