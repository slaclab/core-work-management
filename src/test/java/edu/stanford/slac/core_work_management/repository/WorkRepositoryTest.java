/*
 * -----------------------------------------------------------------------------
 * Title      : WorkRepositoryTest
 * ----------------------------------------------------------------------------
 * File       : WorkRepositoryTest.java
 * Author     : Claudio Bisegni, bisegni@slac.stanford.edu
 * ----------------------------------------------------------------------------
 * This file is part of core-work-management. It is subject to
 * the license terms in the LICENSE.txt file found in the top-level directory
 * of this distribution and at:
 * <a href="https://confluence.slac.stanford.edu/display/ppareg/LICENSE.html"/>.
 * No part of core-work-management, including this file, may be
 * copied, modified, propagated, or distributed except according to the terms
 *  contained in the LICENSE.txt file.
 * ----------------------------------------------------------------------------
 */

package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.Work;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class WorkRepositoryTest {
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    WorkRepository workRepository;

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Work.class);
    }

    @Test
    public void testWorkStatusStatistics() {
        // Create work in new state
//        for (int i = 0; i < 1000; i++) {
//            int finalI = i;
//            String workTypeId = "workType-" + ThreadLocalRandom.current().nextInt(1, 10);
//            var savedWork = assertDoesNotThrow(
//                    () -> workRepository.save(Work.builder().domainId("dom1").title("w-new-%d".formatted(finalI)).workTypeId(workTypeId).currentStatus(WorkStatusLog.builder().status(Work.New).build()).build())
//            );
//            assertThat(savedWork).isNotNull();
//        }
//
//        // Create work in closed state
//        for (int i = 0; i < 1000; i++) {
//            int finalI = i;
//            String workTypeId = "workType-" + ThreadLocalRandom.current().nextInt(1, 10);
//            var savedWork = assertDoesNotThrow(
//                    () -> workRepository.save(Work.builder().domainId("dom1").title("w-closed-%d".formatted(finalI)).workTypeId(workTypeId).currentStatus(WorkStatusLog.builder().status(WorkStatus.Closed).build()).build())
//            );
//            assertThat(savedWork).isNotNull();
//        }
//
//        // Create work in review state
//        for (int i = 0; i < 1000; i++) {
//            int finalI = i;
//            String workTypeId = "workType-" + ThreadLocalRandom.current().nextInt(1, 10);
//            var savedWork = assertDoesNotThrow(
//                    () -> workRepository.save(Work.builder().domainId("dom1").title("w-review-%d".formatted(finalI)).workTypeId(workTypeId).currentStatus(WorkStatusLog.builder().status(WorkStatus.Review).build()).build())
//            );
//            assertThat(savedWork).isNotNull();
//        }
//
//        // Create work in progress state
//        for (int i = 0; i < 1000; i++) {
//            int finalI = i;
//            String workTypeId = "workType-" + ThreadLocalRandom.current().nextInt(1, 10);
//            var savedWork = assertDoesNotThrow(
//                    () -> workRepository.save(Work.builder().domainId("dom1").title("w-progress-%d".formatted(finalI)).workTypeId(workTypeId).currentStatus(WorkStatusLog.builder().status(WorkStatus.InProgress).build()).build())
//            );
//            assertThat(savedWork).isNotNull();
//        }
//
//        // Create work in scheduled state
//        for (int i = 0; i < 1000; i++) {
//            int finalI = i;
//            String workTypeId = "workType-" + ThreadLocalRandom.current().nextInt(1, 10);
//            var savedWork = assertDoesNotThrow(
//                    () -> workRepository.save(Work.builder().domainId("dom1").title("w-scheduled-job-%d".formatted(finalI)).workTypeId(workTypeId).currentStatus(WorkStatusLog.builder().status(WorkStatus.ScheduledJob).build()).build())
//            );
//            assertThat(savedWork).isNotNull();
//        }
//
//        var emptyListWrongDomainId = assertDoesNotThrow(
//                () -> workRepository.getWorkStatisticsByDomainId("dom2")
//        );
//        assertThat(emptyListWrongDomainId).isEmpty();
//
//        var workStatistic = assertDoesNotThrow(
//                () -> workRepository.getWorkStatisticsByDomainId("dom1")
//        );
//
//        // Check if the statistics are correct for each work type ID
//        for (int i = 1; i <= 10; i++) {
//            String workTypeId = "workType-" + i;
//            WorkTypeStatusStatistics stats = workStatistic.stream()
//                    .filter(stat -> stat.getWorkTypeId().equals(workTypeId))
//                    .findFirst()
//                    .orElse(null);
//
//            if(stats == null) {
//                continue;
//            }
//            Map<WorkStatus, Integer> statusCounts = stats.getStatus().stream()
//                    .collect(Collectors.toMap(WorkStatusCountStatistics::getStatus, WorkStatusCountStatistics::getCount));
//
//            assertThat(statusCounts.getOrDefault(WorkStatus.New, 0)).isEqualTo(workRepository.countByWorkTypeIdAndCurrentStatus_StatusIs(workTypeId, WorkStatus.New));
//            assertThat(statusCounts.getOrDefault(WorkStatus.Closed, 0)).isEqualTo(workRepository.countByWorkTypeIdAndCurrentStatus_StatusIs(workTypeId, WorkStatus.Closed));
//            assertThat(statusCounts.getOrDefault(WorkStatus.Review, 0)).isEqualTo(workRepository.countByWorkTypeIdAndCurrentStatus_StatusIs(workTypeId, WorkStatus.Review));
//            assertThat(statusCounts.getOrDefault(WorkStatus.InProgress, 0)).isEqualTo(workRepository.countByWorkTypeIdAndCurrentStatus_StatusIs(workTypeId, WorkStatus.InProgress));
//            assertThat(statusCounts.getOrDefault(WorkStatus.ScheduledJob, 0)).isEqualTo(workRepository.countByWorkTypeIdAndCurrentStatus_StatusIs(workTypeId, WorkStatus.ScheduledJob));
//        }
    }
}
