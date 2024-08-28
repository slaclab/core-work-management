package edu.stanford.slac.core_work_management.api.v1.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
import edu.stanford.slac.core_work_management.api.v1.dto.DomainDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ShopGroupUserInputDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateShopGroupDTO;
import edu.stanford.slac.core_work_management.model.ShopGroup;
import edu.stanford.slac.core_work_management.model.ShopGroupUser;
import edu.stanford.slac.core_work_management.service.DomainService;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING
)
public abstract class ShopGroupMapper {
    @Autowired
    private DomainService domainService;
    @Autowired
    PeopleGroupService peopleGroupService;

    /**
     * Map a shop group to a DTO
     *
     * @param newShopGroupDTO the new shop group
     * @return the DTO
     */
    public abstract ShopGroup toModel(String domainId, NewShopGroupDTO newShopGroupDTO);

    public abstract ShopGroup updateModel(UpdateShopGroupDTO updateShopGroupDTO, @MappingTarget ShopGroup shopGroup);

    @Mapping(target = "domain", expression = "java(toDomainDTO(shopGroup.getDomainId()))")
    public abstract ShopGroupDTO toDTO(ShopGroup shopGroup);

    @Mapping(target = "user", expression = "java(fillPersonDTOById(shopGroupUserInputDTO.userId()))")
    public abstract ShopGroupUser toModel(ShopGroupUserInputDTO shopGroupUserInputDTO);

    public PersonDTO fillPersonDTOById(String userId){
        return peopleGroupService.findPersonByEMail(userId);
    }

    public DomainDTO toDomainDTO(String domainId) {
        if(domainId == null) return null;
        return domainService.findById(domainId);
    }
}
