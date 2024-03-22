package edu.stanford.slac.core_work_management.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.getAllMethodInCall;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Field reference not found")
public class LOVFieldReferenceNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "byFieldName")
    public LOVFieldReferenceNotFound(Integer errorCode, String fieldName) {
        super(errorCode,
                String.format("The field reference has been found for %s", fieldName),
                getAllMethodInCall()
        );
    }
}