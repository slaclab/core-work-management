package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.utility.ResourceUtility;
import groovy.lang.GroovyClassLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ScriptService {
    private final ApplicationContext applicationContext;
    private final GroovyClassLoader classLoader = new GroovyClassLoader();
    private final Map<String, Class<?>> scriptCache = new ConcurrentHashMap<>();

    /**
     * Execute a script content
     * @param scriptContent the content of the script
     * @param interfaceClass the interface class
     * @param methodName method name
     * @param args method arguments
     * @return the result of the script
     * @param <T> the type of the interface
     * @param <R> the type of the return value
     */
    @Async("taskExecutor")
    public <T, R> CompletableFuture<R> executeScriptContent(String scriptContent, Class<T> interfaceClass, String methodName, Object... args){
        try {
            // Generate a unique key for the script using a hash of the script content
            String scriptKey = hashScript(scriptContent);

            // Check if the script has already been compiled and cached
            Class<?> groovyClass = scriptCache.computeIfAbsent(scriptKey, key -> compileScript(scriptContent));

            // Ensure the loaded class implements the provided interface
            if (!interfaceClass.isAssignableFrom(groovyClass)) {
                throw new IllegalArgumentException("The script does not implement the required interface: " + interfaceClass.getName());
            }

            // Create an instance of the Groovy class
            T instance = (T) groovyClass.getDeclaredConstructor().newInstance();
            // Autowire dependencies into the instance using Spring's AutowireCapableBeanFactory
            AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
            factory.autowireBean(instance);  // Injects @Autowired dependencies into the script

            // Use reflection to invoke the method dynamically
            Method method = groovyClass.getMethod(methodName, getParameterTypes(args));
            Object result = method.invoke(instance, args);

            // Return the result as a CompletableFuture of the actual type
            if (method.getReturnType() == Void.TYPE) {
                return CompletableFuture.completedFuture(null);  // For void methods
            } else {
                return CompletableFuture.completedFuture((R) result);  // Return result for non-void methods
            }
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof ControllerLogicException) {
                throw (ControllerLogicException)e.getCause();// Throw the actual exception thrown by the script
            } else {
                throw ControllerLogicException.builder()
                        .errorMessage("Failed to execute script: " + e.getTargetException().getMessage())
                        .errorCode(-1)
                        .errorDomain("ScriptService::executeScriptContent")
                        .build();
            }
        } catch (Exception e) {
            throw ControllerLogicException.builder()
                    .errorMessage("Failed to execute script: " + e.getMessage())
                    .errorCode(-2)
                    .errorDomain("ScriptService::executeScriptContent")
                    .build();
        }
    }

    /**
     * Execute a script file
     * @param scriptFileName the name of the script file
     * @param interfaceClass the interface class
     * @param methodName the method name
     * @param args the method arguments
     * @param <T> the type of the interface
     * @param <R> the type of the return value
     * @return the result of the script
     */
    @Async("taskExecutor")
    public <T, R> CompletableFuture<R> executeScriptFile(String scriptFileName, Class<T> interfaceClass, String methodName, Object... args) {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(scriptFileName);
            return executeScriptContent(loadISContent(inputStream), interfaceClass, methodName, args);
        } catch (IOException e) {
            throw ControllerLogicException.builder()
                    .errorMessage("Failed to load script file: " + e.getMessage())
                    .errorCode(-1)
                    .errorDomain("ScriptService::executeScriptFile")
                    .build();
        }
    }

    /**
     * Get an interface implementation from a script content
     * @param scriptContent the content of the script
     * @return the interface implementation
     * @param <T> the type of the interface
     */
    public <T> T getInterfaceImplementation(String scriptContent, Class<T> expectedType) {
        T instance = null;
        try {
            AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
            // Generate a unique key for the script using a hash of the script content
            String scriptKey = hashScript(scriptContent);

            // Check if the script has already been compiled and cached
            Class<?> groovyClass = scriptCache.computeIfAbsent(scriptKey, key -> compileScript(scriptContent));
            if (!expectedType.isAssignableFrom(groovyClass)) {
                throw ControllerLogicException
                        .builder()
                        .errorMessage("The script does not implement the required interface: " + expectedType.getName())
                        .errorCode(-1)
                        .errorDomain("ScriptService::getInterfaceImplementation")
                        .build();
            }
            // Create an instance of the Groovy class
            // noinspection unchecked
            instance = (T) factory.createBean(groovyClass);  // Create a new instance of the script
        } catch (Exception e) {
            throw ControllerLogicException.builder()
                    .errorMessage("Failed to create script instances: " + e.getMessage())
                    .errorCode(-2)
                    .errorDomain("ScriptService::executeScriptContent")
                    .build();
        }
        return instance;
    }

    /**
     * Get an interface implementation from a script file
     * @param scriptFileName the name of the script file
     * @return the interface implementation
     * @param <T> the type of the interface
     */
    public <T> T getInterfaceImplementationFromFile(String scriptFileName, Class<T> tClass) {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(scriptFileName);
            return getInterfaceImplementation(loadISContent(inputStream), tClass);
        } catch (IOException e) {
            throw ControllerLogicException.builder()
                    .errorMessage("Failed to load script file: " + e.getMessage())
                    .errorCode(-1)
                    .errorDomain("ScriptService::executeScriptFile")
                    .build();
        }
    }

    /**
     * Get the hash of a script file
     * @param scriptFileName the name of the script file
     * @return the hash of the script file
     */
    public String getScriptFileHash(String scriptFileName) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(scriptFileName);
        try {
            return hashScript(loadISContent(inputStream));
        } catch (IOException e) {
            return null;
        }
    }

    // Helper method to compile the script
    private Class<?> compileScript(String scriptText) {
        try {
            return classLoader.parseClass(scriptText);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compile script: " + e.getMessage(), e);
        }
    }

    // Helper method to generate a hash of the script content for caching
    private String hashScript(String scriptText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(scriptText.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash script content", e);
        }
    }

    // Helper method to extract parameter types
    private Class<?>[] getParameterTypes(Object[] args) {
        return args != null ?
                java.util.Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new) :
                new Class<?>[0];
    }

    /**
     * Load the content of a file from the input stream
     * @param inputStream the input stream
     * @return the content of the file
     * @throws IOException if an I/O error occurs
     */
    private String loadISContent(InputStream inputStream) throws IOException {
        if(inputStream == null) {
            throw new IOException("The input stream is null");
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder scriptContent = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            scriptContent.append(line).append("\n");
        }
        reader.close();
        return scriptContent.toString();
    }
}