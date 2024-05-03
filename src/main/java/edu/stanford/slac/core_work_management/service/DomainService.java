package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.DomainDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewDomainDTO;
import edu.stanford.slac.core_work_management.api.v1.mapper.DomainMapper;
import edu.stanford.slac.core_work_management.exception.DomainNotFound;
import edu.stanford.slac.core_work_management.model.Domain;
import edu.stanford.slac.core_work_management.repository.DomainRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Service
@Validated
@AllArgsConstructor
public class DomainService {
    private final DomainMapper domainMapper;
    private final DomainRepository domainRepository;

    /**
     * Create a new domain
     *
     * @param newDomainDTO the DTO to create the domain
     * @return the id of the created domain
     */
    public String createNew(@Valid NewDomainDTO newDomainDTO) {
        Domain savedDomain = wrapCatch(
                () -> domainRepository.save(domainMapper.toModel(newDomainDTO)),
                -1
        );
        return savedDomain.getId();
    }

    /**
     * Find a domain by its id
     *
     * @param id the id of the domain
     * @return the domain
     */
    public DomainDTO findById(String id) {
        return wrapCatch(
                () -> domainRepository.findById(id)
                        .map(domainMapper::toDTO)
                        .orElseThrow(() -> DomainNotFound.notFoundById().errorCode(-1).id(id).build()),
                -2
        );
    }

    /**
     * Find all domains
     *
     * @return the list of all domains
     */
    public List<DomainDTO> finAll() {
        return domainRepository.findAll().stream()
                .map(domainMapper::toDTO)
                .toList();
    }

    /**
     * Check if a domain exists by its id
     *
     * @param domainId the id of the domain
     * @return true if the domain exists, false otherwise
     */
    public Boolean existsById(String domainId) {
        if (domainId == null || domainId.isEmpty() || domainId.isBlank()) {return false;}
        return wrapCatch(
                () -> domainRepository.existsById(domainId),
                -1
        );
    }
}
