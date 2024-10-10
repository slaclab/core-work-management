package edu.stanford.slac.core_work_management.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.getAllMethodInCall;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Workflow has not been found")
public class WorkflowNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "notFoundById")
    public WorkflowNotFound(Integer errorCode, String workflowId) {
        super(errorCode,
                String.format("The workflow with id '%s' has not been found", workflowId),
                getAllMethodInCall()
        );
    }
}