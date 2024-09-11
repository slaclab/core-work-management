package edu.stanford.slac.core_work_management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class ScriptServiceTest {
    @Autowired
    private ScriptService scriptService;
    private String content = """
            package edu.stanford.slac.core_work_management.service;
            
            class DummyScript implements DummyScriptInterface {
            
                @Override
                String dummyMethod() {
                    return "This is a dummy method implementation in Groovy!"
                }
            }""";
    @Test
    public void textScriptOnBaseInterface() {
        CompletableFuture<String> resultContent = assertDoesNotThrow(
                () -> scriptService.executeScriptContent(content, DummyScriptInterface.class, "dummyMethod")
        );
        resultContent.thenAccept(result->assertThat(result).isEqualTo("This is a dummy method implementation in Groovy!"));
    }
}
