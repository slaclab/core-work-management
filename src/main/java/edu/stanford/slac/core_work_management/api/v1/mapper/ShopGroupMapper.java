package edu.stanford.slac.core_work_management.api.v1.mapper;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.mapper.AuthMapper;
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
import edu.stanford.slac.core_work_management.api.v1.dto.NewShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ShopGroupUserDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ShopGroupUserInputDTO;
import edu.stanford.slac.core_work_management.model.ShopGroup;
import edu.stanford.slac.core_work_management.model.ShopGroupUser;
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
    PeopleGroupService peopleGroupService;

    /**
     * Map a shop group to a DTO
     *
     * @param newShopGroupDTO the new shop group
     * @return the DTO
     */
    public abstract ShopGroup toModel(NewShopGroupDTO newShopGroupDTO);

//    @Mapping(target = "users", expression = "java(fillPersonDTOsByIdsList(shopGroup.getUsers()))")
    public abstract ShopGroupDTO toDTO(ShopGroup shopGroup);

    @Mapping(target = "user", expression = "java(fillPersonDTOById(shopGroupUserInputDTO.userId()))")
    public abstract ShopGroupUser toModel(ShopGroupUserInputDTO shopGroupUserInputDTO);

    public PersonDTO fillPersonDTOById(String userId){
        return peopleGroupService.findPersonByEMail(userId);
    }

    /**
     * Fill the person by mail list
     * @param users the user information
     * @return the list of person
     */
//    public Set<ShopGroupUserDTO> fillPersonDTOsByIdsList(Set<ShopGroupUser> users){
//        if (users == null || users.isEmpty()){
//            return emptySet();
//        }
//        return users
//                .stream()
//                // the uid is the email
//                .map((user)-> )
//                .collect(Collectors.toSet());
//    }
}
