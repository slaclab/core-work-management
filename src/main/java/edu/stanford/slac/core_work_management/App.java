package edu.stanford.slac.core_work_management;

import edu.stanford.slac.core_work_management.config.CWMAppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"edu.stanford.slac"})
@EnableConfigurationProperties({CWMAppProperties.class})
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
