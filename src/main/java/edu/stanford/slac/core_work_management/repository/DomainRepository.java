package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.Domain;
import jakarta.validation.constraints.NotNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DomainRepository  extends MongoRepository<Domain, String> {
    @Override
    @Cacheable("domains")
    Optional<Domain> findById(String id);
    Optional<Domain> findByName(String name);
}
