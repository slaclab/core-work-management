package edu.stanford.slac.core_work_management.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.getAllMethodInCall;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "The group name has not been found")
public class LOVGroupNameNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "byName")
    public LOVGroupNameNotFound(Integer errorCode, String name) {
        super(errorCode,
                String.format("The group name has not been found for %s", name),
                getAllMethodInCall()
        );
    }
}