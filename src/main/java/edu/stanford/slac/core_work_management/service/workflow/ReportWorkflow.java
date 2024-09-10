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
                WorkflowState.Created, Set.of(WorkflowState.Assigned),
                // Rule: if one or more safety forms were required, those forms must be attached. if no safety forms are required, move on to next state
                WorkflowState.Assigned, Set.of(WorkflowState.PendingPaperwork, WorkflowState.PendingApproval),
                // Rule: when admin changes field of workStatus = "Approve"
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
    public void update(Work work, WorkType workType, UpdateWorkflowState updateWorkflowState) {
        // assigned to can be empty only in created state
        checkAssignedTo(work);

        // get current state
        var currentStatus = work.getCurrentStatus().getStatus();
        switch (currentStatus) {
            case Created -> {
                if(work.getAssignedTo() != null) {
                   moveToState(work, UpdateWorkflowState.builder().newState(WorkflowState.Assigned).build());
                }
            }
            case Submitted -> {
            }
            case PendingAssignment -> {
            }
            case Assigned -> {
//                var subsystemAttribute = checkFiledPresence(
//                        work.getDomainId().getCustomFields(),
//                        newWorkValidation.getNewWorkDTO().customFieldValues(),
//                        "radiationControlForm",
//                        context);
            }
            case ReadyForWork -> {
            }
            case InProgress -> {
            }
            case PendingApproval -> {
            }
            case PendingPaperwork -> {
            }
            case Approved -> {
            }
            case WorkComplete -> {
            }
            case ReviewToClose -> {
            }
            case Closed -> {
            }
            case None -> {
            }
        }
    }

    @Override
    public boolean isValid(@NotNull NewWorkValidation newWorkValidation, ConstraintValidatorContext context) {
        List<Boolean> validationResult = new ArrayList<>();
        context.disableDefaultConstraintViolation();

        validationResult.add(checkStringField(newWorkValidation.getNewWorkDTO().title(), "title", context));
        validationResult.add(checkStringField(newWorkValidation.getNewWorkDTO().description(), "description", context));
        validationResult.add(checkStringField(newWorkValidation.getNewWorkDTO().locationId(),"locationId", context));
        validationResult.add(checkStringField(newWorkValidation.getNewWorkDTO().shopGroupId(),"shopGroupId", context));
        // Find attribute into custom attribute values
        // find subsystem custom attribute
        var subsystemAttribute = checkFiledPresence(
                newWorkValidation.getWorkType().getCustomFields(),
                newWorkValidation.getNewWorkDTO().customFieldValues(),
                "subsystem",
                context);
        validationResult.add(subsystemAttribute.isPresent());
        // group custom attribute
        var groupAttribute = checkFiledPresence(
                newWorkValidation.getWorkType().getCustomFields(),
                newWorkValidation.getNewWorkDTO().customFieldValues(),
                "group",
                context);
        validationResult.add(groupAttribute.isPresent());
        // urgency custom attribute
        var urgencyAttribute = checkFiledPresence(
                newWorkValidation.getWorkType().getCustomFields(),
                newWorkValidation.getNewWorkDTO().customFieldValues(),
                "urgency",
                context);
        validationResult.add(urgencyAttribute.isPresent());
        return validationResult.stream().anyMatch(b -> b);
    }

    @Override
    public boolean isValid(@NotNull UpdateWorkValidation updateWorkValidation, ConstraintValidatorContext context) {
        return true;
    }

    @Override
    public void canUpdate(String identityId, Work work) {
    }

    @Override
    public boolean canCreateChild(Work work) {
        return false;
    }

    @Override
    public boolean isCompleted(Work work) {
        return false;
    }

    @Override
    public Set<WorkflowState> permittedStatus(Work work) {
        return Set.of();
    }

    /**
     * Check if the string field is not null or empty
     *
     * @param work the work to check
     * @throws  ControllerLogicException if the field is null or empty or with unalloyed user
     */
    private void checkAssignedTo(Work work) {
        // the assignedTo can be null or empty only if we are in created state
        if(work.getCurrentStatus().getStatus()!=WorkflowState.Created &&(work.getAssignedTo() == null || work.getAssignedTo().isEmpty())) {
            throw ControllerLogicException
                    .builder()
                    .errorCode(-1)
                    .errorMessage("The assignedTo field is required in the current state")
                    .errorDomain("ReportWorkflow::checkAssignedTo")
                    .build();
        }

        for (String user : work.getAssignedTo()) {
            assertion(
                    () -> shopGroupService.checkContainsAUserEmail(work.getDomainId(), work.getShopGroupId(), user),
                    ControllerLogicException
                            .builder()
                            .errorCode(-3)
                            .errorMessage("The user is not part of the shop group")
                            .errorDomain("WorkService::update")
                            .build()
            );
        }
    }
}
