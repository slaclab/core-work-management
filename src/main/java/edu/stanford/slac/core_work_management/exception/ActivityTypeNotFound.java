package edu.stanford.slac.core_work_management.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.getAllMethodInCall;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Activity type has not been found")
public class ActivityTypeNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "notFoundById")
    public ActivityTypeNotFound(Integer errorCode, String activityTypeId) {
        super(errorCode,
                String.format("The activity type with id '%s' has not been found", activityTypeId),
                getAllMethodInCall()
        );
    }
}