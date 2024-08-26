package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.api.v1.mapper.DomainMapper;
import edu.stanford.slac.core_work_management.exception.DomainNotFound;
import edu.stanford.slac.core_work_management.exception.WorkTypeNotFound;
import edu.stanford.slac.core_work_management.model.Domain;
import edu.stanford.slac.core_work_management.model.WorkStatusCountStatistics;
import edu.stanford.slac.core_work_management.model.WorkType;
import edu.stanford.slac.core_work_management.model.WorkTypeStatusStatistics;
import edu.stanford.slac.core_work_management.repository.DomainRepository;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Log4j2
@Service
@Validated
@AllArgsConstructor
public class DomainService {
    private final DomainMapper domainMapper;

    private final WorkRepository workRepository;
    private final DomainRepository domainRepository;
    private final WorkTypeRepository workTypeRepository;

    private final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();

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
        log.info("Domain created: {} by {}", savedDomain.getName(), savedDomain.getCreatedBy());
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
        if (domainId == null || domainId.isEmpty() || domainId.isBlank()) {
            return false;
        }
        return wrapCatch(
                () -> domainRepository.existsById(domainId),
                -1
        );
    }

    /**
     * Find a domain by its name
     *
     * @param name the name of the domain
     * @return the domain
     */
    public DomainDTO findByName(@NotEmpty String name) {
        return wrapCatch
                (
                        () -> domainRepository.findByName(name.toLowerCase()),
                        -1
                )
                .map(domainMapper::toDTO)
                .orElseThrow(() -> DomainNotFound.notFoundByName().errorCode(-2).name(name).build());
    }

    /**
     * Create a new work type
     *
     * @param newWorkTypeDTO the DTO to create the work type
     * @return the id of the created work type
     */
    public String ensureWorkType(@NotEmpty String domainId, @Valid NewWorkTypeDTO newWorkTypeDTO) {
        List<WATypeCustomFieldDTO> normalizedCustomField = new ArrayList<>();
        newWorkTypeDTO.customFields().forEach(
                (customField) -> {
                    var tmpName = customField.name();
                    normalizedCustomField.add(
                            customField.toBuilder()
                                    .id(UUID.randomUUID().toString())
                                    .label(tmpName)
                                    .name(StringUtility.toCamelCase(tmpName))
                                    .build()
                    );
                }
        );
        var normalizedActivityDTO = newWorkTypeDTO.toBuilder().customFields(normalizedCustomField).build();
        return wrapCatch(
                () -> workTypeRepository.ensureWorkType(
                        domainMapper.toModel(domainId, newWorkTypeDTO)
                ),
                -1
        );
    }

    /**
     * create a new activity type
     *
     * @param newWorkTypeDTO the new work type to create
     * @return the activity id
     */
    public String createNew(@NotEmpty String domainId, @Valid NewWorkTypeDTO newWorkTypeDTO) {
        // set the id of the custom attributes
        var toSave = domainMapper.toModel(domainId, newWorkTypeDTO);
        toSave.getCustomFields().forEach(
                (customField) -> {
                    customField.setId(UUID.randomUUID().toString());
                    customField.setName(
                            customField.getLabel() == null ?
                                    StringUtility.toCamelCase(customField.getName()) :
                                    StringUtility.toCamelCase(customField.getLabel())
                    );
                    customField.setLabel(customField.getLabel());
                }
        );
        return wrapCatch(
                () -> workTypeRepository.save(toSave),
                -1
        ).getId();
    }

    /**
     * Return the work type  by his id
     *
     * @param domainId the id of the domain
     * @param workId   the id of the activity type
     * @return the activity type
     */
    public WorkTypeDTO findWorkTypeById(@NotEmpty String domainId, @NotEmpty String workId) {
        return wrapCatch
                (
                        () -> workTypeRepository.findById(
                                workId
                        ),
                        -1
                )
                // check if the id is the same as the domain id
                .filter(wt -> wt.getDomainId().equals(domainId))
                .map(domainMapper::toDTO).orElseThrow(
                        () -> WorkTypeNotFound
                                .notFoundById()
                                .errorCode(-2)
                                .workId(workId)
                                .build()
                );
    }

    public void updateWorkType(@NotEmpty String domainId, @Valid String workTypeId, @Valid UpdateWorkTypeDTO workTypeDTO) {
       //todo implement
    }
    /**
     * Return all the work types for a specific domain id
     *
     * @param domainId the id of the domain
     * @return the list of work types
     */
    public List<WorkTypeDTO> findAllWorkTypes(@NotEmpty String domainId) {
        var workTypeList = wrapCatch(
                () -> workTypeRepository.findAllByDomainId(domainId),
                -1
        );
        return workTypeList.stream().map(domainMapper::toDTO).toList();
    }

    /**
     * Update the domain statistics
     *
     * @param domainId the id of the domain
     */
    @Async("taskExecutor")
    public void updateDomainStatistics(@NotEmpty String domainId) {
        log.info("[statistic domainId {}] Updating domain statistics", domainId);
        Optional<Domain> domain = domainRepository.findById(domainId);
        if (domain.isEmpty()) {
            log.error("[statistic domainId {}] Domain with id not found", domainId);
            return;
        }
        log.info("[statistic domainId {}] Acquiring lock for domain", domainId);
        Lock lock = locks.computeIfAbsent(domainId, k -> new ReentrantLock());
        lock.lock();
        try {
            log.info("[statistic domainId {}] Fetch statistic for domain", domainId);
            var workStatistics = wrapCatch(
                    () -> workRepository.getWorkStatisticsByDomainId(domainId),
                    -1
            );

            log.info("[statistic domainId {}] Fetch work types", domainId);
            List<String> workTypeIds = workTypeRepository.findAll().stream().map(WorkType::getId).toList();


            // Check if the statistics are correct for each work type ID
            Map<String, List<WorkStatusCountStatistics>> statMap = new ConcurrentHashMap<>();
            log.info("[statistic domainId {}] Updating statistics for domain", domainId);
            for (String workTypeId : workTypeIds) {
                WorkTypeStatusStatistics stats = workStatistics.stream()
                        .filter(stat -> stat.getWorkTypeId().equals(workTypeId))
                        .findFirst()
                        .orElse(null);
                if (stats == null) {
                    continue;
                }

                statMap.put(workTypeId, stats.getStatus());
            }
            domain.get().setWorkTypeStatusStatistics(statMap);
            log.info("[statistic domainId {}] Saving domain statistics", domainId);
            domainRepository.save(domain.get());
        } catch (Throwable e) {
            log.error("[statistic domainId {}] Error updating domain statistics for domain {}", domainId, e.getMessage());
        } finally {
            lock.unlock();
            locks.remove(domainId);
        }
        log.info("[statistic domainId {}] Domain statistics updated", domainId);
    }
}
