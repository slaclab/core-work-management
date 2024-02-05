package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
import edu.stanford.slac.core_work_management.api.v1.dto.NewShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.mapper.ShopGroupMapper;
import edu.stanford.slac.core_work_management.model.ShopGroup;
import edu.stanford.slac.core_work_management.repository.ShopGroupRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Service
@Validated
@AllArgsConstructor
public class ShopGroupService {
    ShopGroupMapper shopGroupMapper;
    PeopleGroupService peopleGroupService;
    ShopGroupRepository shopGroupRepository;

    /**
     * Create a new shop group
     *
     * @param newShopGroupDTO the DTO to create the shop group
     * @return the id of the created shop group
     */
    public String createNew(@Validated NewShopGroupDTO newShopGroupDTO) {
        // validate user emails
        newShopGroupDTO.userEmails().forEach(
                (email)->peopleGroupService.findPersonByEMail(email)
        );
        ShopGroup savedShopGroup = wrapCatch(
                () -> shopGroupRepository.save(shopGroupMapper.toModel(newShopGroupDTO)),
                -1
        );
        return savedShopGroup.getId();
    }

    /**
     * Get all shop groups
     *
     * @return the list of shop groups
     */
    public List<ShopGroupDTO> getAllShopGroups() {
        return wrapCatch(
                () -> shopGroupRepository.findAll(),
                -1
        ).stream().map(shopGroupMapper::toDTO).toList();
    }

    /**
     * Check if a shop group exists
     *
     * @param shopGroupId the id of the shop group
     * @return true if the shop group exists, false otherwise
     */
    public Boolean exists(String shopGroupId) {
        return wrapCatch(
                () -> shopGroupRepository.existsById(shopGroupId),
                -1
        );
    }
}
