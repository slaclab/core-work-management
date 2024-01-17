package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Service

public class HelperService {
    @Autowired
    WorkService workService;

    public List<String> ensureWorkAndActivitiesTypes(NewWorkTypeDTO newWorkTypeDTO, List<NewActivityTypeDTO> newActivityTypeDTOS) {
        List<String> listIds = new ArrayList<>();
        String newWorkTypeId = assertDoesNotThrow(
                () -> workService.ensureWorkType(
                        newWorkTypeDTO
                )
        );
        assertThat(newWorkTypeId).isNotNull();
        listIds.add(newWorkTypeId);
        for(NewActivityTypeDTO newActivityDTO : newActivityTypeDTOS) {
            String newActivityTypeId = assertDoesNotThrow(
                    () -> workService.ensureActivityType(
                            newWorkTypeId,
                            newActivityDTO
                    )
            );
            assertThat(newActivityTypeId).isNotEmpty();
            listIds.add(newActivityTypeId);
        }
        return listIds;
    }

    /**
     * Fetch work and check status
     */
    public boolean checkStatusOnWork(String workId, WorkStatusDTO workStatus){
        var foundFullWork =  assertDoesNotThrow(
                ()->workService.findWorkById(workId)
        );
        assertThat(foundFullWork).isNotNull();
        return foundFullWork.currentStatus().status().equals(workStatus);
    }

    public boolean checkStatusAndHistoryOnWork(String workId, List<WorkStatusDTO> workStatusList){
        var foundFullWork =  assertDoesNotThrow(
                ()->workService.findWorkById(workId)
        );
        assertThat(foundFullWork).isNotNull();
        if(workStatusList != null && !workStatusList.isEmpty()) {
            if(foundFullWork.currentStatus().status().equals(workStatusList.getFirst())) {
                for(int idx = 0; idx < foundFullWork.statusHistory().size(); idx++)
                    if (!foundFullWork.statusHistory().get(idx).status().equals(workStatusList.get(idx + 1))) {
                        return false;
                    }
            } else
                return false;
        } else
            return false;
        return true;
    }

    /**
     * Fetch activity and check status
     */
    public boolean checkStatusOnActivity(String activityId, ActivityStatusDTO activityStatus){
        var foundFullActivity =  assertDoesNotThrow(
                ()->workService.findActivityById(activityId)
        );
        assertThat(foundFullActivity).isNotNull();
        return foundFullActivity.currentStatus().status().equals(activityStatus);
    }

    public boolean checkStatusAndHistoryOnActivity(String activityId, List<ActivityStatusDTO> activityStatus){
        var foundFullActivity =  assertDoesNotThrow(
                ()->workService.findActivityById(activityId)
        );
        assertThat(foundFullActivity).isNotNull();
        if(activityStatus != null && !activityStatus.isEmpty()) {
            if(foundFullActivity.currentStatus().status().equals(activityStatus.getFirst())) {
                for(int idx = 0; idx < foundFullActivity.statusHistory().size(); idx++)
                    if (!foundFullActivity.statusHistory().get(idx).status().equals(activityStatus.get(idx + 1))) {
                        return false;
                    }
            } else
                return false;
        } else
            return false;
        return true;
    }
}
