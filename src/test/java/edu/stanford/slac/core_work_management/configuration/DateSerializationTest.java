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
import edu.stanford.slac.core_work_management.api.v1.dto.ValueDTO;
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
    @Test
    public void dateDeserialization() {
        ValueDTO test = assertDoesNotThrow(()-> objectMapper.readValue("{\"type\": \"Date\", \"value\": \"2024-03-28T14:00:00.000Z\"}", ValueDTO.class));
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
