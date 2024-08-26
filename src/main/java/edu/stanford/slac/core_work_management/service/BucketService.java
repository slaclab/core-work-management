package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.BucketDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.BucketQueryParameterDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewBucketDTO;
import edu.stanford.slac.core_work_management.api.v1.mapper.BucketSlotMapper;
import edu.stanford.slac.core_work_management.exception.ActivityAlreadyAssociatedToSlot;
import edu.stanford.slac.core_work_management.exception.BucketSlotNotFound;
import edu.stanford.slac.core_work_management.model.BucketSlot;
import edu.stanford.slac.core_work_management.model.BucketSlotActivityStatus;
import edu.stanford.slac.core_work_management.repository.BucketActivityRepository;
import edu.stanford.slac.core_work_management.repository.BucketRepository;
import edu.stanford.slac.core_work_management.service.validation.BucketValidationService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Validated
@Service
@AllArgsConstructor
public class BucketService {
    private final BucketRepository bucketSlotRepository;
    private final BucketActivityRepository bucketSlotActivityRepository;
    private final BucketSlotMapper bucketSlotMapper;
    private final BucketValidationService bucketValidationService;

    /**
     * This method is used to create a new bucket slot
     *
     * @param newBucketSlotDTO the new bucket slot DTO
     * @return the id of the new bucket slot
     */
    public String createNew(@Valid NewBucketDTO newBucketSlotDTO) {
        BucketSlot bs = bucketSlotMapper.toModel(newBucketSlotDTO);
        BucketSlot finalBs = bs;
        assertion(
                ControllerLogicException.builder().build(),
                () -> bucketValidationService.verify(finalBs)
        );
        bs = bucketSlotRepository.save(bs);
        return bs.getId();
    }

    /**
     * This method is used to find a bucket slot by id
     *
     * @param id the id of the bucket slot
     * @return the bucket slot DTO
     */
    public BucketDTO findById(String id) {
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
     * This method is used to find all bucket slots
     *
     * @param queryParameterDTO the query parameter DTO
     * @return the list of bucket slot DTOs
     */
    public List<BucketDTO> findAll(BucketQueryParameterDTO queryParameterDTO) {
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
     * This method is used to associate an activity to a bucket slot
     *
     * @param bucketSlotId the id of the bucket slot
     * @param activityId   the id of the activity
     */
    public void associateActivityToSlot(String bucketSlotId, String activityId) {
        // check if the activity is already associated in the slot
        assertion(
                ActivityAlreadyAssociatedToSlot
                        .byActivityIdAndSlotId()
                        .slotId(bucketSlotId)
                        .activityId(activityId)
                        .build(),
                () -> !bucketSlotActivityRepository.existsByBucketSlotIdAndActivityId(bucketSlotId, activityId)
        );

        // check if the activity exists in a status that is not rolled (other statuses are only permitted to one slot)
        assertion(
                ActivityAlreadyAssociatedToSlot
                        .byActivityIdAndSlotId()
                        .slotId(bucketSlotId)
                        .activityId(activityId)
                        .build(),
                () -> !bucketSlotActivityRepository.existsByActivityIdAndStatusIn(
                        activityId,
                        List.of
                                (
                                        BucketSlotActivityStatus.ACCEPTED,
                                        BucketSlotActivityStatus.REJECTED
                                )
                )
        );
    }
}
