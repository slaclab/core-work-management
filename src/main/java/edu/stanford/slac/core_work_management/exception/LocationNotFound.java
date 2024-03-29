package edu.stanford.slac.core_work_management.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.getAllMethodInCall;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Work has not been found")
public class LocationNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "notFoundById")
    public LocationNotFound(Integer errorCode, String locationId) {
        super(errorCode,
                String.format("The location with id '%s' has not been found", locationId),
                getAllMethodInCall()
        );
    }
}