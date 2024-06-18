package edu.stanford.slac.core_work_management.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.getAllMethodInCall;

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Activity already associated to slot")
public class ActivityAlreadyAssociatedToSlot extends ControllerLogicException {
    @Builder(builderMethodName = "byActivityIdAndSlotId")
    public ActivityAlreadyAssociatedToSlot(Integer errorCode, String activityId, String slotId) {
        super(errorCode,
                String.format("The activity with id '%s' is already associated to slot '%s'", activityId, slotId),
                getAllMethodInCall()
        );
    }
}