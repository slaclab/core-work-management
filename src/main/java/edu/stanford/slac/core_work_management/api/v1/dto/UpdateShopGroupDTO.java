package edu.stanford.slac.core_work_management.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.Set;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "New Shop Group DTO, a group of people that is in charge of fixing a problem")
public record UpdateShopGroupDTO(
    @Schema(description = "The name of the shop group")
    @NotEmpty(message = "The name of the shop group cannot be empty")
    String name,
    @Schema(description = "The description of the shop group")
    @NotEmpty(message = "The description of the shop group cannot be empty")
    String description,
    @Valid
    @Schema(description = "The user ids that are part of the shop group")
    @NotEmpty(message = "The user list annot be empty")
    Set<ShopGroupUserInputDTO> users){}
