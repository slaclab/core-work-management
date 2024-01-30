package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@AllArgsConstructor
public class ActivitySanitizationMongodbCallback implements BeforeConvertCallback<Activity>{
    @Override
    @NonNull
    public Activity onBeforeConvert(@NonNull Activity activity, @NonNull String collection) {
        // normalize the class name
//        activity.setName(
//                normalizeStringWithReplace(
//                        activity.getName(),
//                        " ",
//                        "-"
//                )
//        );
        log.trace(
                "Normalize activity class: {}",
                activity.getTitle()
        );
        return activity;
    }
}
