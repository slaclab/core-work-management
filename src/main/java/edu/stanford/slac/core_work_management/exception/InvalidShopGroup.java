package edu.stanford.slac.core_work_management.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.getAllMethodInCall;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Invalid shop group")
public class InvalidShopGroup extends ControllerLogicException {
    @Builder(builderMethodName = "byShopGroupNameDomainId")
    public InvalidShopGroup(Integer errorCode, String shopGroupName, String domainId) {
        super(errorCode,
                String.format("The shop group '%s' is invalid for the domain '%s'", shopGroupName, domainId),
                getAllMethodInCall()
        );
    }
}