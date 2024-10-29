package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.api.v1.mapper.BucketSlotMapper;
import edu.stanford.slac.core_work_management.exception.ActivityAlreadyAssociatedToSlot;
import edu.stanford.slac.core_work_management.exception.BucketSlotNotFound;
import edu.stanford.slac.core_work_management.exception.DomainNotFound;
import edu.stanford.slac.core_work_management.exception.WorkTypeNotFound;
import edu.stanford.slac.core_work_management.model.BucketSlot;
import edu.stanford.slac.core_work_management.model.BucketSlotActivityStatus;
import edu.stanford.slac.core_work_management.repository.BucketRepository;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import edu.stanford.slac.core_work_management.service.validation.BucketValidationService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Validated
@Service
@AllArgsConstructor
public class BucketService {
    private final BucketRepository bucketSlotRepository;
    private final BucketSlotMapper bucketSlotMapper;
    private final BucketValidationService bucketValidationService;
    private final WorkRepository workRepository;
    private final DomainService domainService;
    private final LOVService lovService;

    /**
     * This method is used to get all the bucket types
     *
     * @return the list of bucket types
     */
    public List<LOVElementDTO> getBucketTypes() {
       return lovService.findAllByGroupName("BucketType");
    }

    /**
     * This method is used to get all the bucket status
     *
     * @return the list of bucket status
     */
    public List<LOVElementDTO> getBucketStatus() {
        return  lovService.findAllByGroupName("BucketStatus");
    }

    /**
     * This method is used to create a new bucket slot
     *
     * @param newBucketSlotDTO the new bucket slot DTO
     * @return the id of the new bucket slot
     */
    public String createNew(@Valid NewBucketDTO newBucketSlotDTO) {
        // check if the domains exists
        newBucketSlotDTO.domainIds().forEach(
                id->
               assertion(
                       DomainNotFound
                               .notFoundById()
                               .id(id)
                               .errorCode(-1)
                               .build(),
                       () -> domainService.existsById(id)
               )
        );

        // check if work type exists
        newBucketSlotDTO.admittedWorkTypeIds().forEach(
                idBundle->
                assertion(
                        WorkTypeNotFound
                                .notFoundById()
                                .workId(idBundle.workTypeId())
                                .errorCode(-2)
                                .build(),
                        () -> domainService.existsWrkTypeByDomainIdAndId(idBundle.domainId(), idBundle.workTypeId())
                )
        );

        // validate the bucket slot
        BucketSlot bs = bucketSlotMapper.toModel(newBucketSlotDTO);
        assertion(
                ControllerLogicException.builder().build(),
                () -> bucketValidationService.verify(bs)
        );
        // save and return id
        return  wrapCatch(()->bucketSlotRepository.save(bs), -3).getId();
    }

    /**
     * This method is used to update a bucket slot
     *
     * @param id the id of the bucket slot
     * @param updateBucketDTO the update bucket DTO
     */
    public void update(String id, UpdateBucketDTO updateBucketDTO) {
        var foundBucket = wrapCatch(
                ()->bucketSlotRepository
                .findById(id)
                .orElseThrow(
                        () -> BucketSlotNotFound.byId().id(id).build()
                ),
                -1
        );
        // check if the domains exists
        if(updateBucketDTO.domainIds()!=null){
            updateBucketDTO.domainIds().forEach(
                    did->
                            assertion(
                                    DomainNotFound
                                            .notFoundById()
                                            .id(did)
                                            .errorCode(-2)
                                            .build(),
                                    () -> domainService.existsById(did)
                            )
            );
        }


        // check if work type exists
        if(updateBucketDTO.admittedWorkTypeIds()!=null){
            updateBucketDTO.admittedWorkTypeIds().forEach(
                    idBundle->
                            assertion(
                                    WorkTypeNotFound
                                            .notFoundById()
                                            .workId(idBundle.workTypeId())
                                            .errorCode(-3)
                                            .build(),
                                    () -> domainService.existsWrkTypeByDomainIdAndId(idBundle.domainId(), idBundle.workTypeId())
                            )
            );
        }

        // validate the bucket slot
        BucketSlot updatedBucket = bucketSlotMapper.updateModel(updateBucketDTO, foundBucket);
        assertion(
                ControllerLogicException.builder().build(),
                () -> bucketValidationService.verify(updatedBucket)
        );
        // save and return id
        wrapCatch(()->bucketSlotRepository.save(updatedBucket), -4);
    }

    /**
     * This method is used to find a bucket slot by id
     *
     * @param id the id of the bucket slot
     * @return the bucket slot DTO
     */
    public BucketSlotDTO findById(String id) {
        return bucketSlotMapper.toDTO(
                wrapCatch(
                        () -> bucketSlotRepository
                                .findById(id)
                                .orElseThrow(
                                        () -> BucketSlotNotFound.byId().id(id).build()
                                ),
                        -1
                )
        );
    }

    /**
     * This method is used to update a bucket slot
     *
     * @param id the id of the bucket slot
     * @return the updated bucket slot DTO
     */
    public boolean existsById(String id) {
        return wrapCatch(
                ()->bucketSlotRepository.existsById(id),
                -1
        );
    }

    /**
     * Delete a bucket slot by id
     * Before delete the bucket it, should be checked if it is associated with any activity
     * @param id the id of the bucket slot
     */
    public void deleteById(String id) {
        assertion(
                ControllerLogicException
                        .builder()
                        .build(),
                ()->wrapCatch(
                        ()->workRepository.existsByCurrentBucketAssociationBucketId(id),
                        -1
                )
        );

        // now we can delete
        wrapCatch(
                ()->{bucketSlotRepository.deleteById(id); return null;},
                -2
        );
    }

    /**
     * This method is used to find all bucket slots
     *
     * @param queryParameterDTO the query parameter DTO
     * @return the list of bucket slot DTOs
     */
    public List<BucketSlotDTO> findAll(BucketQueryParameterDTO queryParameterDTO) {
        return bucketSlotRepository
                .searchAll
                        (
                                bucketSlotMapper.toModel(queryParameterDTO)
                        )
                .stream()
                .map(bucketSlotMapper::toDTO)
                .toList();
    }

    /**
     * This method is used to find all bucket slots that contains a given date
     *
     * @param date the date to check
     * @return the list of bucket slot DTOs
     */
    public List<BucketSlotDTO> findAllThatContainsDate(LocalDateTime date) {
        return bucketSlotRepository
                .findAllThatContainsDate(date)
                .stream()
                .map(bucketSlotMapper::toDTO)
                .toList();
    }

    /**
     * This method is used to find the next bucket slot that need to manage to be started up
     *
     * @param currentDate the current date
     * @param timeoutDate the date when the bucket need to be considered as timeout for processing
     * @return the bucket slot DTO to startup
     */
    public BucketSlotDTO findNextBucketToStart(LocalDateTime currentDate, LocalDateTime timeoutDate) {
        return bucketSlotMapper.toDTO(
                wrapCatch(
                        ()->bucketSlotRepository
                                .findNextBucketToStart(currentDate, timeoutDate),
                        -1
                )
        );
    }

    /**
     * This method is used to find the next bucket slot that need to manage to be stopped
     *
     * @param currentDate the current date
     * @param timeoutDate the date when the bucket need to be considered as timeout for processing
     * @return the bucket slot DTO to stop
     */
    public BucketSlotDTO findNextBucketToStop(LocalDateTime currentDate, LocalDateTime timeoutDate) {
        return bucketSlotMapper.toDTO(
                wrapCatch(
                        ()->bucketSlotRepository
                                .findNextBucketToStop(currentDate, timeoutDate),
                        -1
                )
        );
    }

    /**
     * This method is used to complete the start event processing
     *
     * @param id the id of the bucket slot
     */
    public void completeStartEventProcessing(String id) {
        wrapCatch(
                ()->{
                    bucketSlotRepository
                            .completeStartEventProcessing(id);
                    return null;
                },
                -1
        );
    }

    /**
     * This method is used to complete the stop event processing
     *
     * @param id the id of the bucket slot
     */
    public void completeStopEventProcessing(String id) {
        wrapCatch(
                ()->{
                    bucketSlotRepository
                            .completeStopEventProcessing(id);
                    return null;
                },
                -1
        );
    }
}
