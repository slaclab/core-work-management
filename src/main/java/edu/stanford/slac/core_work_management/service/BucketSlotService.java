package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.BucketSlotDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewBucketSlotDTO;
import edu.stanford.slac.core_work_management.api.v1.mapper.BucketSlotMapper;
import edu.stanford.slac.core_work_management.exception.ActivityAlreadyAssociatedToSlot;
import edu.stanford.slac.core_work_management.exception.BucketSlotNotFound;
import edu.stanford.slac.core_work_management.model.BucketSlot;
import edu.stanford.slac.core_work_management.model.BucketSlotActivityStatus;
import edu.stanford.slac.core_work_management.repository.ActivityRepository;
import edu.stanford.slac.core_work_management.repository.BucketSlotActivityRepository;
import edu.stanford.slac.core_work_management.repository.BucketSlotRepository;
import edu.stanford.slac.core_work_management.service.validation.BucketValidationService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;
import static org.springframework.http.HttpStatus.ACCEPTED;

@Validated
@Service
@AllArgsConstructor
public class BucketSlotService {
    private final ActivityRepository activityRepository;
    private final BucketSlotRepository bucketSlotRepository;
    private final BucketSlotActivityRepository bucketSlotActivityRepository;
    private final BucketSlotMapper bucketSlotMapper;
    private final BucketValidationService bucketValidationService;

    /**
     * This method is used to create a new bucket slot
     *
     * @param newBucketSlotDTO the new bucket slot DTO
     * @return the id of the new bucket slot
     */
    public String createNew(@Valid NewBucketSlotDTO newBucketSlotDTO) {
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
