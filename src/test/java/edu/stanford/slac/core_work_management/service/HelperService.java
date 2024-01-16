package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.ActivityStatusDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkStatusDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Service

public class HelperService {
    @Autowired
    WorkService workService;


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
}
