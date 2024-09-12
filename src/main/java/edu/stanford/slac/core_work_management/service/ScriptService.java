package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.utility.ResourceUtility;
import groovy.lang.GroovyClassLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    @Async("taskExecutor")
    public <T, R> CompletableFuture<R> executeScriptFile(String scriptFileName, Class<T> interfaceClass, String methodName, Object... args) {
        try {
            return executeScriptContent(ResourceUtility.loadFileFromResource(scriptFileName), interfaceClass, methodName, args);
        } catch (IOException e) {
            throw ControllerLogicException.builder()
                    .errorMessage("Failed to load script file: " + e.getMessage())
                    .errorCode(-1)
                    .errorDomain("ScriptService::executeScriptFile")
                    .build();
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
}