package edu.stanford.slac.core_work_management.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.getAllMethodInCall;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Field reference not found")
public class LOVValueNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "byId")
    public LOVValueNotFound(Integer errorCode, String id) {
        super(errorCode,
                String.format("The lov value of id %s has been found".formatted(id)),
                getAllMethodInCall()
        );
    }
}