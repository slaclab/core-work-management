package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.NewAuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
import edu.stanford.slac.core_work_management.api.v1.dto.NewShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ShopGroupUserInputDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.mapper.ShopGroupMapper;
import edu.stanford.slac.core_work_management.exception.ShopGroupNotFound;
import edu.stanford.slac.core_work_management.model.ShopGroup;
import edu.stanford.slac.core_work_management.repository.ShopGroupRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;
import static edu.stanford.slac.core_work_management.config.AuthorizationStringConfig.SHOP_GROUP_AUTHORIZATION_TEMPLATE;

@Service
@Validated
@AllArgsConstructor
public class ShopGroupService {
    AuthService authService;
    ShopGroupMapper shopGroupMapper;
    PeopleGroupService peopleGroupService;
    ShopGroupRepository shopGroupRepository;

    /**
     * Create a new shop group
     *
     * @param domainId        the id of the domain
     * @param newShopGroupDTO the DTO to create the shop group
     * @return the id of the created shop group
     */
    @Transactional
    public String createNew(@NotEmpty String domainId, @Valid NewShopGroupDTO newShopGroupDTO) {
        // validate user emails
        ShopGroup savedShopGroup = wrapCatch(
                () -> shopGroupRepository.save(shopGroupMapper.toModel(domainId, newShopGroupDTO)),
                -1
        );

        // generate authorization for admin user
        updateShopGroupAuthorization(savedShopGroup.getId(), newShopGroupDTO.users());
        return savedShopGroup.getId();
    }

    /**
     * Update a shop group
     *
     * @param shopGroupId        the id of the shop group
     * @param updateShopGroupDTO the DTO to update the shop group
     */
    public void update(@NotEmpty String domainId, @NotEmpty String shopGroupId, @Valid UpdateShopGroupDTO updateShopGroupDTO) {
        var storedShopGroup = wrapCatch(
                () -> shopGroupRepository.findByDomainIdAndId(domainId, shopGroupId),
                -1
        ).orElseThrow(
                () -> ShopGroupNotFound.notFoundById()
                        .errorCode(-2)
                        .shopGroupId(shopGroupId)
                        .build()
        );
        wrapCatch(
                () -> shopGroupRepository.save(shopGroupMapper.updateModel(updateShopGroupDTO, storedShopGroup)),
                -3
        );

        // update authorization for the shop-group
        updateShopGroupAuthorization(shopGroupId, updateShopGroupDTO.users());
    }

    /**
     * Delete a shop group
     *
     * @param shopGroupId            the id of the shop group
     * @param shopGroupUserInputDTOS the DTOs of the shop group users
     */
    private void updateShopGroupAuthorization(String shopGroupId, Set<ShopGroupUserInputDTO> shopGroupUserInputDTOS) {
        if (shopGroupUserInputDTOS == null) return;
        // remove all old authorizations
        authService.deleteAuthorizationForResourcePrefix(SHOP_GROUP_AUTHORIZATION_TEMPLATE.formatted(shopGroupId));

        // create only the admin authorization for the leaders
        shopGroupUserInputDTOS.forEach(
                (shopGroupUserDTO) -> {
                    if (shopGroupUserDTO.isLeader()) {
                        // check if the person exists
                        peopleGroupService.findPersonByEMail(shopGroupUserDTO.userId());

                        authService.addNewAuthorization(
                                NewAuthorizationDTO.builder()
                                        .owner(shopGroupUserDTO.userId())
                                        // authorize on specific shop group
                                        .resource(SHOP_GROUP_AUTHORIZATION_TEMPLATE.formatted(shopGroupId))
                                        .authorizationType(AuthorizationTypeDTO.Admin)
                                        .ownerType(AuthorizationOwnerTypeDTO.User)
                                        .build()
                        );
                    }
                }
        );
    }

    /**
     * Get all shop groups
     *
     * @return the list of shop groups
     */
    public List<ShopGroupDTO> findAllByDomainId(@NotEmpty String domainId) {
        return wrapCatch(
                () -> shopGroupRepository.findAllByDomainId(domainId),
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

    /**
     * Find a shop group by id
     *
     * @param shopGroupId the id of the shop group
     * @return the shop group
     */
    public ShopGroupDTO findByDomainIdAndId(@NotEmpty String domainId, @NotEmpty String shopGroupId) {
        return wrapCatch(
                () -> shopGroupRepository.findByDomainIdAndId(domainId, shopGroupId)
                        .map(shopGroupMapper::toDTO)
                        .orElseThrow(
                                () -> ShopGroupNotFound.notFoundById()
                                        .errorCode(-2)
                                        .shopGroupId(shopGroupId)
                                        .build()
                        ),
                -3
        );
    }

    /**
     * Check if a specific shop group contains a user email
     *
     * @param domainId    the id of the domain
     * @param shopGroupId the id of the shop group
     * @param userEmail   the email of the user
     * @return true if the shop group exists
     */
    public Boolean checkContainsAUserEmail(String domainId, String shopGroupId, String userEmail) {
        return wrapCatch(
                () -> shopGroupRepository.existsByDomainIdAndIdAndUsers_User_mail_ContainingIgnoreCase(domainId, shopGroupId, userEmail),
                -1
        );
    }

}
