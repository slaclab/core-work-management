package edu.stanford.slac.core_work_management.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.getAllMethodInCall;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "ShopGroup type has not been found")
public class ShopGroupNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "notFoundById")
    public ShopGroupNotFound(Integer errorCode, String shopGroupId) {
        super(errorCode,
                String.format("The shop group with id '%s' has not been found", shopGroupId),
                getAllMethodInCall()
        );
    }
}