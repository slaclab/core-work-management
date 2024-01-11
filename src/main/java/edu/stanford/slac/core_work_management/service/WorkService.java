package edu.stanford.slac.core_work_management.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
public class WorkService {
    // dedicated rest template for service to service communication
    private final RestTemplate serviceRestTemplate;
    public void createNew() {

    }
}
