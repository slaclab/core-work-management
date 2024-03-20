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
import edu.stanford.slac.core_work_management.model.WorkType;
import org.assertj.core.api.AssertionsForClassTypes;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
    public void testGentNextActivitiesNumber() {
        var savedWork = assertDoesNotThrow(
                ()->workRepository.save(Work.builder().title("name").build())
        );
        assertThat(savedWork).isNotNull();
        assertThat(savedWork.getActivitiesNumber()).isEqualTo(0);

        var nextId = assertDoesNotThrow(
                ()->workRepository.getNextActivityNumber(savedWork.getId())
        );

        assertThat(nextId).isEqualTo(1);
    }

    @Test
    public void testGentNextActivitiesMultiThreading() throws InterruptedException, ExecutionException {
        var savedWork = assertDoesNotThrow(
                ()->workRepository.save(Work.builder().title("name").build())
        );
        assertThat(savedWork).isNotNull();
        assertThat(savedWork.getActivitiesNumber()).isEqualTo(0);

        int numberOfThreads = 20; // Number of concurrent threads
        List<Future<Integer>> futures;
        List<Integer> generatedIds = new ArrayList<>();
        try (ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads)) {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < numberOfThreads*10; i++) {
                tasks.add(() -> workRepository.getNextActivityNumber(savedWork.getId()));
            }

            futures = executorService.invokeAll(tasks);

            // Shut down the executor service
            executorService.shutdown();
        }

        // Assert that all threads received the same ID
        for (Future<Integer> future : futures) {
            generatedIds.add(future.get());
        }
        var array = generatedIds.toArray();
        Arrays.sort(array);

        //check if all the ids are unique
        for (int i = 0; i < array.length - 1; i++) {
            assertThat(array[i]).isNotEqualTo(array[i + 1]);
        }
    }
}
