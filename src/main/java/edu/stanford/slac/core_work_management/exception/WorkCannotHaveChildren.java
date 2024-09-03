package edu.stanford.slac.core_work_management.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.getAllMethodInCall;

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Work cannot have children")
public class WorkCannotHaveChildren extends ControllerLogicException {
    @Builder(builderMethodName = "byId")
    public WorkCannotHaveChildren(Integer errorCode, String workId) {
        super(errorCode,
                String.format("The work with id '%s' cannot have children", workId),
                getAllMethodInCall()
        );
    }
}