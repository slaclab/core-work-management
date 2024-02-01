package edu.stanford.slac.core_work_management.migration;

import edu.stanford.slac.core_work_management.api.v1.dto.NewShopGroupDTO;
import edu.stanford.slac.core_work_management.service.ShopGroupService;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Profile({"init-shop-group"})
@ChangeUnit(id = "init-shop-group", order = "1002", author = "bisegni")
public class InitShopGroup {
    private final ShopGroupService shopGroupService;

    @Execution
    public void changeSet() {
        List<NewShopGroupDTO> newShopGroupDTOList = List.of(
                NewShopGroupDTO.builder()
                        .name("Accelerator Physics")
                        .description("Accelerator Physics")
                        .userEmails(Set.of("user1@slac.stanford.edu"))
                        .build(),
                NewShopGroupDTO.builder()
                        .name("Accelerator Controls Systems")
                        .description("Accelerator Controls Systems")
                        .userEmails(Set.of("user2@slac.stanford.edu", "user3@slac.stanford.edu"))
                        .build()
        );
        for (NewShopGroupDTO shopGroupDTO : newShopGroupDTOList) {
            shopGroupService.createNew(shopGroupDTO);
        }
    }

    @RollbackExecution
    public void rollback() {

    }
}