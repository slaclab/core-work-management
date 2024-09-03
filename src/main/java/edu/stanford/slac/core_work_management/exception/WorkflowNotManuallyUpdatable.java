package edu.stanford.slac.core_work_management.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.getAllMethodInCall;

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Workflow cannot be manually updated")
public class WorkflowNotManuallyUpdatable extends ControllerLogicException {
    @Builder(builderMethodName = "of")
    public WorkflowNotManuallyUpdatable(Integer errorCode) {
        super(errorCode,
                "The workflow cannot be manually updated",
                getAllMethodInCall()
        );
    }
}