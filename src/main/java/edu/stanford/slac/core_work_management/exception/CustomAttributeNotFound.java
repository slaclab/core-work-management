package edu.stanford.slac.core_work_management.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.getAllMethodInCall;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Activity type custom attribute has not been found")
public class CustomAttributeNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "notFoundById")
    public CustomAttributeNotFound(Integer errorCode, String id) {
        super(errorCode,
                String.format("The custom attribute with id '%s' has not been found", id),
                getAllMethodInCall()
        );
    }
}