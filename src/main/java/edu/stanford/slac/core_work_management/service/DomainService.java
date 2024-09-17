package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.api.v1.mapper.DomainMapper;
import edu.stanford.slac.core_work_management.exception.DomainNotFound;
import edu.stanford.slac.core_work_management.exception.LOVGroupNameNotFound;
import edu.stanford.slac.core_work_management.exception.WorkTypeNotFound;
import edu.stanford.slac.core_work_management.exception.WorkflowNotFound;
import edu.stanford.slac.core_work_management.model.Domain;
import edu.stanford.slac.core_work_management.model.WorkStatusCountStatistics;
import edu.stanford.slac.core_work_management.model.WorkType;
import edu.stanford.slac.core_work_management.model.WorkTypeStatusStatistics;
import edu.stanford.slac.core_work_management.model.value.ValueType;
import edu.stanford.slac.core_work_management.repository.DomainRepository;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import edu.stanford.slac.core_work_management.service.workflow.BaseWorkflow;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Log4j2
@Service
@Validated
@AllArgsConstructor
public class DomainService {
    private final ApplicationContext applicationContext;

    private final DomainMapper domainMapper;

    private final LOVService lovService;
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
        return createNewAndGet(newDomainDTO).id();
    }

    /**
     * Create a new domain and return the domain
     *
     * @param newDomainDTO the DTO to create the domain
     * @return the created domain
     */
    public DomainDTO createNewAndGet(@Valid NewDomainDTO newDomainDTO) {
        Domain savedDomain = wrapCatch(
                () -> domainRepository.save(domainMapper.toModel(newDomainDTO)),
                -1
        );
        log.info("Domain created: {} by {}", savedDomain.getName(), savedDomain.getCreatedBy());
        return domainMapper.toDTO(savedDomain);
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
     * create a new activity type
     *
     * @param newWorkTypeDTO the new work type to create
     * @return the activity id
     */
    @Transactional
    public String createNew(@NotEmpty String domainId, @Valid NewWorkTypeDTO newWorkTypeDTO) {
        // set the id of the custom attributes
        var toSave = domainMapper.toModel(domainId, newWorkTypeDTO);

        // check domain existence
        Domain d = wrapCatch(
                () -> domainRepository.findById(domainId).orElseThrow(
                        () -> DomainNotFound
                                .notFoundById()
                                .errorCode(-2)
                                .id(domainId)
                                .build()
                ),
                -1
        );
        // check for existence workflow id
        if(newWorkTypeDTO.workflowId()!=null) {
            d.getWorkflows().stream()
                    .filter(w -> w.getId().equals(newWorkTypeDTO.workflowId()))
                    .findFirst()
                    .orElseThrow(
                            () -> ControllerLogicException
                                    .builder()
                                    .errorCode(-3)
                                    .errorMessage("Workflow not found")
                                    .errorDomain("DomainService::createNew(String, NewWorkTypeDTO)")
                                    .build()
                    );
        }

        // check for WorkType used as child
        newWorkTypeDTO.childWorkTypeIds().forEach(
                (childId) -> {
                    assertion(
                            ControllerLogicException
                                    .builder()
                                    .errorCode(-4)
                                    .errorMessage("Child WorkType of id '%s' not found".formatted(childId))
                                    .errorDomain("DomainService::createNew(String, NewWorkTypeDTO)")
                                    .build(),
                            () -> wrapCatch(
                                    () -> workTypeRepository.existsByDomainIdAndId(domainId, childId),
                                    -5
                            )
                    );
                }
        );
        // check for custom fields
        toSave.getCustomFields().forEach(
                (customField) -> {
                    customField.setId(UUID.randomUUID().toString());
                    customField.setName(
                            customField.getLabel() == null ?
                                    StringUtility.toCamelCase(customField.getName()) :
                                    StringUtility.toCamelCase(customField.getLabel())
                    );
                    customField.setLabel(customField.getLabel());

                    if(customField.getValueType()== ValueType.LOV &&
                            customField.getAdditionalMappingInfo()!=null &&
                            !customField.getAdditionalMappingInfo().isEmpty()) {
                        // check for group name existence
                        assertion(
                                LOVGroupNameNotFound
                                        .byName()
                                        .errorCode(-6)
                                        .name(customField.getAdditionalMappingInfo())
                                        .build(),
                                () -> lovService.existsByGroupName(customField.getAdditionalMappingInfo())
                        );
                        // for each custom field that is LOV we need to associate it to right LOV field
                        lovService.addFieldReferenceToGroupName(
                                customField.getAdditionalMappingInfo(),
                                List.of(customField.getLovFieldReference())
                        );
                    }
                }
        );


        // save the work type
        return wrapCatch(
                () -> workTypeRepository.save(toSave),
                -16
        ).getId();
    }

    /**
     * Check if a work type exists by domain id and id
     *
     * @param domainId   the id of the domain
     * @param workTypeId the id of the work type
     * @return true if the work type exists, false otherwise
     */
    public boolean existsWrkTypeByDomainIdAndId(String domainId, String workTypeId) {
        return wrapCatch(
                () -> workTypeRepository.existsByDomainIdAndId(domainId, workTypeId),
                -1
        );
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

    /**
     * Update the work type
     *
     * @param domainId    the id of the domain
     * @param workTypeId  the id of the work type
     * @param workTypeDTO the DTO to update the work type
     */
    public void updateWorkType(@NotEmpty String domainId, @Valid String workTypeId, @Valid UpdateWorkTypeDTO workTypeDTO) {
        var workType = wrapCatch(
                () -> workTypeRepository.findByDomainIdAndId(domainId, workTypeId).orElseThrow(
                        () -> WorkTypeNotFound
                                .notFoundById()
                                .errorCode(-1)
                                .workId(workTypeId)
                                .build()
                ),
                -1
        );
        // check for domain id
        var updatedWorkType = domainMapper.updateModel(workTypeDTO, workType);
        wrapCatch(
                () -> workTypeRepository.save(updatedWorkType),
                -3
        );
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
     * Try to find the workflow instance
     *
     * @param domainId   the id of the domain
     * @param workTypeId the id work type
     */
//    @Cacheable(value = "workflow", key = "#domainId + #workTypeId")
    public BaseWorkflow getWorkflowInstanceByDomainIdAndWorkTypeId(String domainId, String workTypeId) {
        var domain = findById(domainId);
        var workType = findWorkTypeById(domainId, workTypeId);

        var workflow = domain.workflows()
                .stream()
                .filter(w -> w.id().compareTo(workType.workflow().id()) == 0)
                .findFirst()
                .orElseThrow(
                        () -> WorkflowNotFound
                                .notFoundById()
                                .errorCode(-1)
                                .workflowId(workType.workflow().id())
                                .build()
                );
        return (BaseWorkflow) applicationContext.getBean(workflow.implementation());
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
