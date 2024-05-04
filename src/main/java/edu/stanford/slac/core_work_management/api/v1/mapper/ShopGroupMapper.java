package edu.stanford.slac.core_work_management.api.v1.mapper;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.mapper.AuthMapper;
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.model.ShopGroup;
import edu.stanford.slac.core_work_management.model.ShopGroupUser;
import edu.stanford.slac.core_work_management.service.DomainService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING
)
public abstract class ShopGroupMapper {
    @Autowired
    private AuthMapper authMapper;
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
    public abstract ShopGroup toModel(NewShopGroupDTO newShopGroupDTO);

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
