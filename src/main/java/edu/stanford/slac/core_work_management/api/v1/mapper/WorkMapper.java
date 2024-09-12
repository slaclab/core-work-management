package edu.stanford.slac.core_work_management.api.v1.mapper;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ModelChangesHistoryDTO;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.ad.eed.baselib.service.ModelHistoryService;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.CustomAttributeNotFound;
import edu.stanford.slac.core_work_management.exception.LOVValueNotFound;
import edu.stanford.slac.core_work_management.exception.WorkTypeNotFound;
import edu.stanford.slac.core_work_management.model.CustomField;
import edu.stanford.slac.core_work_management.model.WATypeCustomField;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkQueryParameter;
import edu.stanford.slac.core_work_management.model.value.*;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import edu.stanford.slac.core_work_management.service.DomainService;
import edu.stanford.slac.core_work_management.service.LOVService;
import edu.stanford.slac.core_work_management.service.LocationService;
import edu.stanford.slac.core_work_management.service.ShopGroupService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;
import static edu.stanford.slac.core_work_management.config.AuthorizationStringConfig.WORK_AUTHORIZATION_TEMPLATE;

/**
 * Mapper for the entity {@link Work}
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public abstract class WorkMapper {
    @Autowired
    AuthService authService;
    @Autowired
    ShopGroupService shopGroupService;
    @Autowired
    LocationService locationService;
    @Autowired
    LOVService lovService;
    @Autowired
    DomainService domainService;
    @Autowired
    WorkTypeRepository workTypeRepository;
    @Autowired
    ModelHistoryService modelHistoryService;
    @Autowired
    DomainMapper domainMapper;

    /**
     * Convert the {@link NewWorkDTO} to a {@link Work}
     *
     * @param newWorkDTO the DTO to convert
     * @return the converted entity
     */
    @Mapping(target = "customFields", expression = "java(toCustomFieldValues(newWorkDTO.customFieldValues()))")
    abstract public Work toModel(String domainId, Long workNumber, NewWorkDTO newWorkDTO);





    /**
     * Update the {@link Work} with the data from the {@link UpdateWorkDTO}
     *
     * @param dto  the DTO with the data to update
     * @param work the entity to update
     */
//    @Mapping(target = "currentStatus", expression = "java(toWorkStatusLog(dto.workflowStateUpdate()))")
    abstract public void updateModel(UpdateWorkDTO dto, @MappingTarget Work work);

    /**
     * Convert the {@link Work} to a {@link WorkDTO}
     *
     * @param work the entity to convert
     * @return the converted DTO
     */
    @Mapping(target = "workType", expression = "java(toWorkTypeDTOFromWorkTypeId(work.getWorkTypeId()))")
    @Mapping(target = "shopGroup", expression = "java(toShopGroupDTOById(work.getDomainId(), work.getShopGroupId()))")
    @Mapping(target = "location", expression = "java(toLocationDTOById(work.getDomainId(), work.getLocationId()))")
    @Mapping(target = "customFields", expression = "java(toCustomFieldValuesDTOForWork(work.getWorkTypeId(), work.getCustomFields()))")
    @Mapping(target = "domain", expression = "java(toDomainDTO(work.getDomainId()))")
    @Mapping(target = "changesHistory", expression = "java(getChanges(work.getId(), workDetailsOptionDTO))")
    abstract public WorkDTO toDTO(Work work, WorkDetailsOptionDTO workDetailsOptionDTO);

    /**
     * Fetch the list of changed on the work
     *
     * @param workId the entity to convert
     * @return the list of changed
     */
    public List<ModelChangesHistoryDTO> getChanges(String workId, WorkDetailsOptionDTO workDetailsOptionDTO) {
        if(workDetailsOptionDTO==null || workDetailsOptionDTO.changes()==null || !workDetailsOptionDTO.changes()) return Collections.emptyList();
        return modelHistoryService.findChangesByModelId(Work.class, workId);
    }

    /**
     * Convert the {@link WorkQueryParameterDTO} to a {@link WorkQueryParameter}
     *
     * @param workQueryParameterDTO the DTO to convert
     * @return the converted entity
     */
    abstract public WorkQueryParameter toModel(WorkQueryParameterDTO workQueryParameterDTO);

    /**
     * Convert the {@link WriteCustomFieldDTO} to a {@link CustomField}
     *
     * @param customFieldValues the list of the custom attributes
     * @return the converted entity
     */
    public List<CustomField> toCustomFieldValues(List<WriteCustomFieldDTO> customFieldValues) {
        return customFieldValues.stream().map(
                customAttributeDTO -> CustomField.builder()
                        .id(customAttributeDTO.id())
                        .value(toAbstractValue(customAttributeDTO.value()))
                        .build()
        ).toList();
    }

    /**
     * Convert the {@link String} domain id to a {@link DomainDTO}
     *
     * @param domainId the id of the domain
     * @return the converted DTO
     */
    public DomainDTO toDomainDTO(String domainId) {
        if(domainId == null) return null;
        return domainService.findById(domainId);
    }

    /**
     * Convert the {@link WriteCustomFieldDTO} to a {@link CustomField}
     *
     * @param customAttributesValues the list of the custom attributes
     * @return the converted entity
     */
    public List<CustomFieldDTO> toCustomFieldValuesDTOForWork(String workTypeId, List<CustomField> customAttributesValues) {
        return customAttributesValues.stream().map(
                customAttribute -> CustomFieldDTO.builder()
                        .id(customAttribute.getId())
                        .name(
                                workTypeRepository
                                        .findCustomFieldById(workTypeId, customAttribute.getId())
                                        .map(WATypeCustomField::getName)
                                        .orElseThrow(
                                                () -> CustomAttributeNotFound.notFoundById()
                                                        .id(customAttribute.getId())
                                                        .build()
                                        )
                        )
                        .value(
                                toValueDTO(customAttribute.getValue())
                        )
                        .build()
        ).toList();
    }

    /**
     * Convert static string field to {@link }LOVValueDTO}
     *
     * @param value the id of the lov value
     * @return the value from lov if found
     */
    public LOVValueDTO toLOVValueDTO(String value) {
        if (value == null) return null;
        var valueFound = lovService.findLovValueById(value);
        return LOVValueDTO
                .builder()
                .id(
                        value
                )
                .value(
                        valueFound
                )
                .build();
    }

    /**
     * Try to find form lov otherwise return the default value
     *
     * @param value the default value
     * @return the value from lov if found
     */
    private AbstractValue tryToLOV(AbstractValue value) {
        if (value.getClass().isAssignableFrom(LOVValue.class)) {
            var lovElementFound = lovService.findLovValueByIdNoException(((LOVValue) value).getValue());
            if (lovElementFound.isPresent()) {
                return LOVValue.builder().value(lovElementFound.get().getValue()).build();
            }
        }
        return value;
    }


    /**
     * Get the authorization level on activity
     */
    public AuthorizationTypeDTO getActivityAuthorizationByWorkId(String domainId, String workId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            // if the DTO has been requested by an anonymous user, then the access level is Read
            // in other case will should have been blocked by the security layer
            return AuthorizationTypeDTO.Read;
        }

        //check if it's a root
        boolean isRoot = authService.checkForRoot(authentication);
        // check if user can write normal field
        if (isRoot) return AuthorizationTypeDTO.Admin;
        if (authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                authentication,
                AuthorizationTypeDTO.Write,
                WORK_AUTHORIZATION_TEMPLATE.formatted(workId)
        )) return AuthorizationTypeDTO.Write;
        if (shopGroupService.checkContainsAUserEmail(
                // fire not found work exception
                domainId,
                workId,
                authentication.getCredentials().toString()
        )) return AuthorizationTypeDTO.Write;
        return AuthorizationTypeDTO.Read;
    }

    /**
     * Convert the {@link String} shop group id to a {@link ShopGroupDTO}
     *
     * @param shopGroupId the id of the shop group
     * @return the converted DTO
     */
    public ShopGroupDTO toShopGroupDTOById(String domainId, String shopGroupId) {
        if (shopGroupId == null) return null;
        return shopGroupService.findByDomainIdAndId(domainId, shopGroupId);
    }

    /**
     * Convert the {@link String} location id to a {@link LocationDTO}
     *
     * @param locationId the id of the location
     * @return the converted DTO
     */
    public LocationDTO toLocationDTOById(String domainId, String locationId) {
        if (locationId == null) return null;
        return locationService.findById(domainId, locationId);
    }

    /**
     * Convert the {@link String} work type id to a {@link WorkTypeDTO}
     *
     * @param workTypeId the id of the work type
     * @return the converted DTO
     */
    public WorkTypeDTO toWorkTypeDTOFromWorkTypeId(String workTypeId) {
        return wrapCatch(
                () -> workTypeRepository.findById(workTypeId).map(domainMapper::toDTO).orElseThrow(
                        () -> WorkTypeNotFound.notFoundById()
                                .errorCode(-1)
                                .workId(workTypeId)
                                .build()
                ),
                -1
        );
    }


    /**
     * Convert the {@link ValueDTO} to a {@link AbstractValue}
     *
     * @param value the DTO to convert
     * @return the converted entity
     */
    public AbstractValue toAbstractValue(ValueDTO value) {
        try {
            switch (value.type()) {
                case String -> {
                    return StringValue
                            .builder()
                            .value(value.value())
                            .build();
                }
                case Number -> {
                    return NumberValue
                            .builder()
                            .value(Long.valueOf(value.value()))
                            .build();
                }
                case Double -> {
                    return DoubleValue
                            .builder()
                            .value(Double.valueOf(value.value()))
                            .build();
                }
                case Boolean -> {
                    return BooleanValue
                            .builder()
                            .value(Boolean.valueOf(value.value()))
                            .build();
                }
                case Date -> {
                    try {
                        return DateValue
                                .builder()
                                .value(LocalDate.parse(value.value(), DateTimeFormatter.ISO_LOCAL_DATE))
                                .build();
                    } catch (Exception e) {
                        // try to parse the date as OffsetDateTime
                        try {
                            var date = OffsetDateTime.parse(value.value());
                            return DateValue
                                    .builder()
                                    .value(date.toLocalDate())
                                    .build();
                        } catch (Exception e1) {
                            // throw error
                            throw ControllerLogicException.builder()
                                    .errorCode(-1)
                                    .errorMessage(e1.getMessage())
                                    .errorDomain("WorkMapper::toAbstractValue")
                                    .build();
                        }
                    }
                }
                case DateTime -> {
                    try {
                        return DateTimeValue
                                .builder()
                                .value(LocalDateTime.parse(value.value(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                                .build();
                    } catch (Exception e) {
                        // try to parse the date as OffsetDateTime
                        var date = OffsetDateTime.parse(value.value());
                        return DateTimeValue
                                .builder()
                                .value(date.toLocalDateTime())
                                .build();
                    }
                }
                case LOV -> {
                    return LOVValue
                            .builder()
                            .value(value.value())
                            .build();
                }
                case Attachments -> {
                    List<String> attachmentIds = Arrays.asList(value.value().split(","));
                    return AttachmentsValue
                            .builder()
                            .value(attachmentIds)
                            .build();
                }

                default -> throw ControllerLogicException.builder()
                        .errorCode(-4)
                        .errorMessage("Invalid attribute type")
                        .errorDomain("WorkMapper::toElementAttributeWithClass")
                        .build();
            }
        }catch (ControllerLogicException | NumberFormatException e){
            throw ControllerLogicException.builder()
                    .errorCode(-5)
                    .errorMessage(e.getMessage())
                    .errorDomain("WorkMapper::toElementAttributeWithClass")
                    .build();
        }
    }

    /**
     * Convert the {@link AbstractValue} to a {@link ValueDTO}
     *
     * @param abstractValue the entity to convert
     * @return the converted DTO
     */
    protected ValueDTO toValueDTO(AbstractValue abstractValue) {
        ValueDTO newAttributeValue = null;
        Class<? extends AbstractValue> valueType = abstractValue.getClass();
        if (valueType.isAssignableFrom(StringValue.class)) {
            newAttributeValue = ValueDTO
                    .builder()
                    .type(ValueTypeDTO.String)
                    .value(((StringValue) abstractValue).getValue())
                    .build();
        } else if (valueType.isAssignableFrom(BooleanValue.class)) {
            newAttributeValue = ValueDTO
                    .builder()
                    .type(ValueTypeDTO.Boolean)
                    .value(((BooleanValue) abstractValue).getValue().toString())
                    .build();
        } else if (valueType.isAssignableFrom(NumberValue.class)) {
            newAttributeValue = ValueDTO
                    .builder()
                    .type(ValueTypeDTO.Number)
                    .value(((NumberValue) abstractValue).getValue().toString())
                    .build();
        } else if (valueType.isAssignableFrom(DoubleValue.class)) {
            newAttributeValue = ValueDTO
                    .builder()
                    .type(ValueTypeDTO.Double)
                    .value(((DoubleValue) abstractValue).getValue().toString())
                    .build();
        } else if (valueType.isAssignableFrom(DateValue.class)) {
            newAttributeValue = ValueDTO
                    .builder()
                    .type(ValueTypeDTO.Date)
                    .value(((DateValue) abstractValue).getValue().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .build();
        } else if (valueType.isAssignableFrom(DateTimeValue.class)) {
            newAttributeValue = ValueDTO
                    .builder()
                    .type(ValueTypeDTO.DateTime)
                    .value(((DateTimeValue) abstractValue).getValue().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();
        } else if (valueType.isAssignableFrom(LOVValue.class)) {
            LOVValue lValue =  (LOVValue) abstractValue;
            // get the real lov value
            var lovElementFound = lovService.findLovValueByIdNoException(lValue.getValue())
                    .orElseThrow(
                            ()->LOVValueNotFound.byId()
                                    .errorCode(-1)
                                    .id(lValue.getValue())
                                    .build()
                    );
            newAttributeValue = ValueDTO
                    .builder()
                    .type(ValueTypeDTO.LOV)
                    .value(lovElementFound.getValue())
                    .build();
        } else if (valueType.isAssignableFrom(AttachmentsValue.class)) {
            //TODO: test with attachment ids
            newAttributeValue = ValueDTO
                    .builder()
                    .type(ValueTypeDTO.Attachments)
                    .value(String.join(",", ((AttachmentsValue) abstractValue).getValue()))
                    .build();
        } else {
            throw ControllerLogicException.builder()
                    .errorCode(-4)
                    .errorMessage("Invalid attribute type")
                    .errorDomain("InventoryElementMapper::toElementAttributeWithClass")
                    .build();
        }
        return newAttributeValue;
    }

    @AfterMapping
    protected void afterMapping(@MappingTarget final WorkDTO.WorkDTOBuilder target, Work source) {
        var listOfReferenced = lovService.getLOVFieldReference(LOVDomainTypeDTO.Work, source.getDomainId(), source.getWorkTypeId()).keySet();
        var targetFields = target.getClass().getDeclaredFields();
        var sourceFields = source.getClass().getDeclaredFields();
        listOfReferenced.forEach(
                field -> {
                    var staticTargetField = Arrays.stream(targetFields).filter(
                            tField -> tField.getName().equals(field)
                    ).findFirst();
                    if (staticTargetField.isPresent()) {
                        var field1 = staticTargetField.get();
                        // the model contains the id of the lov not the real value
                        var staticDynamicField = Arrays.stream(sourceFields).filter(
                                sourceField -> sourceField.getName().equals(field)
                        ).findFirst();

                        staticDynamicField.ifPresent(
                                field2 -> {
                                    try {
                                        field2.setAccessible(true);
                                        if (field2.get(source) == null) return;
                                        String idValue = field2.get(source).toString();
                                        field1.setAccessible(true);
                                        field1.set(target,
                                                LOVValueDTO.builder()
                                                        .id(idValue)
                                                        .value(lovService.findLovValueById(idValue))
                                                        .build()
                                        );
                                    } catch (IllegalAccessException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        );
                    }
                }
        );
    }
}
