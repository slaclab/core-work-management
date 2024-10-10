package edu.stanford.slac.core_work_management.utility;


import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ResourceUtility {

    /**
     * Load the file content from the resources folder
     *
     * @param fileName The file name
     * @return The file content
     * @throws IOException If the file is not found
     */
    static public String loadFileFromResource(String fileName) throws IOException {
        // Load the file from the resources folder
        ClassPathResource resource = new ClassPathResource(fileName);
        // Convert the file content to a String
        return new String(Files.readAllBytes(Paths.get(resource.getURI())));
    }

}
