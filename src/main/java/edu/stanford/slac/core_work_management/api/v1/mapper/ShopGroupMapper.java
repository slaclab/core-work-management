package edu.stanford.slac.core_work_management.api.v1.mapper;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.mapper.AuthMapper;
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
import edu.stanford.slac.core_work_management.api.v1.dto.NewShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ShopGroupDTO;
import edu.stanford.slac.core_work_management.model.ShopGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class ShopGroupMapper {
    AuthMapper authMapper;
    PeopleGroupService peopleGroupService;
    /**
     * Map a shop group to a DTO
     *
     * @param newShopGroupDTO the new shop group
     * @return the DTO
     */
    public abstract ShopGroup toModel(NewShopGroupDTO newShopGroupDTO);

    @Mapping(target = "userEmails", expression = "java(fillPersonByMailList(shopGroup.getUserEmails()))")
    public abstract ShopGroupDTO toDTO(ShopGroup shopGroup);

    /**
     * Fill the person by mail list
     * @param userEmails the user emails
     * @return the list of person
     */
    public Set<PersonDTO> fillPersonByMailList(Set<String> userEmails){
        if (userEmails == null || userEmails.isEmpty()){
            return emptySet();
        }
        return userEmails
                .stream()
                .map((email)-> peopleGroupService.findPersonByEMail(email))
                .collect(Collectors.toSet());
    }
}
