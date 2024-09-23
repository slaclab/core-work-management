/*
 * -----------------------------------------------------------------------------
 * Title      : DateSerialization
 * ----------------------------------------------------------------------------
 * File       : DateSerialization.java
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

package edu.stanford.slac.core_work_management.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.core_work_management.api.v1.dto.WriteCustomFieldDTO;
import edu.stanford.slac.core_work_management.api.v1.mapper.WorkMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DateSerializationTest {
    @Autowired
    ObjectMapper  objectMapper;
    @Autowired
    WorkMapper workMapper;
    @Test
    public void dateDeserialization() {
        WriteCustomFieldDTO test = assertDoesNotThrow(()-> objectMapper.readValue("{\"id\":\"id-found\",\"value\":{\"type\": \"Date\", \"value\": \"2024-03-28T14:00:00.000Z\"}}", WriteCustomFieldDTO.class));
        var modelList = assertDoesNotThrow(
                ()-> workMapper.toCustomFieldValues(of(test))
        );
        assertThat(modelList).isNotEmpty();
    }

    @Test
    public void date2Deserialization() {
        WriteCustomFieldDTO test = assertDoesNotThrow(()-> objectMapper.readValue("{\"id\":\"id-found\",\"value\":{\"type\": \"Date\", \"value\": \"2024-03-28\"}}", WriteCustomFieldDTO.class));
        var modelList = assertDoesNotThrow(
                ()-> workMapper.toCustomFieldValues(of(test))
        );
        assertThat(modelList).isNotEmpty();
    }

    @Test
    public void dateTimeDeserialization() {
        WriteCustomFieldDTO test = assertDoesNotThrow(()-> objectMapper.readValue("{\"id\":\"id-found\",\"value\":{\"type\": \"DateTime\", \"value\": \"2024-03-28T14:00:00.000Z\"}}", WriteCustomFieldDTO.class));
        var modelList = assertDoesNotThrow(
                ()-> workMapper.toCustomFieldValues(of(test))
        );
        assertThat(modelList).isNotEmpty();
    }

    @Test
    public void dateSerialization() {
        var date = LocalDateTime.now();
        String serializedDate = assertDoesNotThrow(()-> objectMapper.writeValueAsString(date));
        // check this format: 2024-03-28T14:00:00.000Z
        var deserializedDate = assertDoesNotThrow(()-> objectMapper.readValue(serializedDate, LocalDateTime.class));
        assertThat(deserializedDate).isEqualTo(date);
    }
}
