package edu.stanford.slac.core_work_management.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.getAllMethodInCall;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Field reference not found")
public class LOVValueNotAssignable extends ControllerLogicException {
    @Builder(builderMethodName = "byValueAndFName")
    public LOVValueNotAssignable(Integer errorCode, String value, String fieldName) {
        super(errorCode,
                String.format("The lov value '%s' cannot be assigned to %s".formatted(value, fieldName)),
                getAllMethodInCall()
        );
    }
}