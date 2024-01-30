package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.*;
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

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LocationRepositoryTest {
    @Autowired
    LocationRepository locationRepository;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    public void cleanCollection() {
        mongoTemplate.remove(new Query(), Location.class);
    }
    @Test
    public void testPathToRoot() {
        var location1 = locationRepository.save(Location.builder().name("1").build());
        var location2 = locationRepository.save(Location.builder().parentId(location1.getId()).name("2").build());
        var location3 = locationRepository.save(Location.builder().parentId(location2.getId()).name("3").build());
        var location4 = locationRepository.save(Location.builder().parentId(location3.getId()).name("4").build());
        var location5 = locationRepository.save(Location.builder().parentId(location4.getId()).name("5").build());

        List<Location> pathToRoot = locationRepository.findPathToRoot(location3.getId());
        assertThat(pathToRoot)
                .hasSize(2)
                .extracting(Location::getName)
                .containsExactly("2", "1");
        List<Location> pathToLeaf = locationRepository.findIdPathToLeaf(location3.getId());
        assertThat(pathToLeaf)
                .hasSize(2)
                .extracting(Location::getName)
                .containsExactly("4", "5");
    }
}
