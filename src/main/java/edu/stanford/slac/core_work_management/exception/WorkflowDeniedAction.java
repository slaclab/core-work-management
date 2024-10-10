package edu.stanford.slac.core_work_management.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.getAllMethodInCall;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Workflow denied operation")
public class WorkflowDeniedAction extends ControllerLogicException {
    @Builder(builderMethodName = "byErrorMessage")
    public WorkflowDeniedAction(Integer errorCode, String errorMessage) {
        super(errorCode,
                errorMessage,
                getAllMethodInCall()
        );
    }
}