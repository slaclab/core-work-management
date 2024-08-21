package edu.stanford.slac.core_work_management.api.v1.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ModelChangesHistoryDTO;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.ad.eed.baselib.service.ModelHistoryService;
import edu.stanford.slac.core_work_management.api.v1.dto.ActivityDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ActivityQueryParameterDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ActivityStatusDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ActivitySummaryDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ActivityTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.CustomFieldDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.DomainDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.LOVDomainTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.LOVValueDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.LocationDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewActivityDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewActivityTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewWorkTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateActivityDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateActivityTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.UpdateWorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ValueDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ValueTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WATypeCustomFieldDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkDetailsOptionDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkQueryParameterDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkTypeDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkTypeSummaryDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WriteCustomFieldDTO;
import static edu.stanford.slac.core_work_management.config.AuthorizationStringConfig.WORK_AUTHORIZATION_TEMPLATE;
import edu.stanford.slac.core_work_management.exception.ActivityTypeNotFound;
import edu.stanford.slac.core_work_management.exception.CustomAttributeNotFound;
import edu.stanford.slac.core_work_management.exception.WorkTypeNotFound;
import edu.stanford.slac.core_work_management.model.Activity;
import edu.stanford.slac.core_work_management.model.ActivityQueryParameter;
import edu.stanford.slac.core_work_management.model.ActivityStatus;
import edu.stanford.slac.core_work_management.model.ActivityStatusLog;
import edu.stanford.slac.core_work_management.model.ActivityType;
import edu.stanford.slac.core_work_management.model.CustomField;
import edu.stanford.slac.core_work_management.model.WATypeCustomField;
import edu.stanford.slac.core_work_management.model.Work;
import edu.stanford.slac.core_work_management.model.WorkQueryParameter;
import edu.stanford.slac.core_work_management.model.WorkType;
import edu.stanford.slac.core_work_management.model.value.AbstractValue;
import edu.stanford.slac.core_work_management.model.value.BooleanValue;
import edu.stanford.slac.core_work_management.model.value.DateTimeValue;
import edu.stanford.slac.core_work_management.model.value.DateValue;
import edu.stanford.slac.core_work_management.model.value.DoubleValue;
import edu.stanford.slac.core_work_management.model.value.NumberValue;
import edu.stanford.slac.core_work_management.model.value.StringValue;
import edu.stanford.slac.core_work_management.repository.ActivityTypeRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import edu.stanford.slac.core_work_management.service.DomainService;
import edu.stanford.slac.core_work_management.service.LOVService;
import edu.stanford.slac.core_work_management.service.LocationService;
import edu.stanford.slac.core_work_management.service.ShopGroupService;
import edu.stanford.slac.core_work_management.service.StringUtility;

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
    ActivityTypeRepository activityTypeRepository;
    @Autowired
    ModelHistoryService modelHistoryService;

    /**
     * Convert the {@link NewWorkTypeDTO} to a {@link WorkType}
     *
     * @param newWorkTypeDTO the DTO to convert
     * @return the converted entity
     */
    abstract public WorkType toModel(NewWorkTypeDTO newWorkTypeDTO);

    /**
     * Convert the {@link NewActivityTypeDTO} to a {@link ActivityType}
     *
     * @param newActivityTypeDTO the DTO to convert
     * @return the converted work type
     */
    abstract public ActivityType toModel(NewActivityTypeDTO newActivityTypeDTO);

    /**
     * Convert the {@link NewWorkDTO} to a {@link Work}
     *
     * @param newWorkDTO the DTO to convert
     * @return the converted entity
     */
    @Mapping(target = "customFields", expression = "java(toCustomFieldValues(newWorkDTO.customFieldValues()))")
    abstract public Work toModel(Long workNumber, NewWorkDTO newWorkDTO);

    /**
     * Update the {@link Work} with the data from the {@link UpdateWorkDTO}
     *
     * @param dto  the DTO with the data to update
     * @param work the entity to update
     */
    abstract public void updateModel(UpdateWorkDTO dto, @MappingTarget Work work);

    /**
     * Convert the {@link WorkType} to a {@link WorkTypeDTO}
     *
     * @param workType the entity to convert
     * @return the converted DTO
     */
    abstract public WorkTypeDTO toDTO(WorkType workType);

    /**
     * Convert the {@link WorkType} to a {@link WorkTypeSummaryDTO}
     *
     * @param workType the entity to convert
     * @return the converted DTO
     */
    abstract public WorkTypeSummaryDTO toSummaryDTO(WorkType workType);

    /**
     * Convert the {@link ActivityType} to a {@link ActivityTypeDTO}
     *
     * @param activityType the entity to convert
     * @return the converted DTO
     */
    abstract public ActivityTypeDTO toDTO(ActivityType activityType);

    @Mapping(target = "isLov", expression = "java(checkIsLOV(customField))")
    abstract public WATypeCustomFieldDTO toDTO(WATypeCustomField customField);

    /**
     * Check if the custom field is a LOV
     *
     * @param customField the custom field to check
     * @return true if the custom field is a LOV
     */
    public boolean checkIsLOV(WATypeCustomField customField) {
        if (customField.getLovFieldReference() == null) return false;
        return wrapCatch(
                ()->lovService.checkIfFieldReferenceIsInUse(customField.getLovFieldReference()),
                -1
        );
    }

    /**
     * Convert the {@link ActivityStatus} to a {@link ActivityStatusDTO}
     *
     * @param activityStatus the entity to convert
     * @return the converted DTO
     */
    abstract public ActivityStatusDTO toDTO(ActivityStatus activityStatus);

    /**
     * Convert the {@link NewActivityDTO} to a {@link Activity}
     *
     * @param newActivityDTO the DTO to convert
     * @param workId         the id of the work
     * @return the converted entity
     */
    @Mapping(target = "customFields", expression = "java(toCustomFieldValues(newActivityDTO.customFieldValues()))")
    abstract public Activity toModel(NewActivityDTO newActivityDTO, String workId, Long workNumber, String domainId, Long activityNumber);

    /**
     * Update the {@link Activity} with the data from the {@link UpdateActivityDTO}
     *
     * @param dto  the DTO with the data to update
     * @param work the entity to update
     */
    @Mapping(target = "customFields", expression = "java(toCustomFieldValues(dto.customFieldValues()))")
    abstract public void updateModel(UpdateActivityDTO dto, @MappingTarget Activity activity);

    /**
     * Convert the {@link ActivityStatusDTO} to a {@link ActivityStatus}
     *
     * @param activityStatusDTO the DTO to convert
     * @return the converted entity
     */
    abstract public ActivityStatus toModel(ActivityStatusDTO activityStatusDTO);

    /**
     * Convert the {@link NewActivityTypeDTO} to a {@link ActivityType}
     *
     * @param dto the DTO to convert
     * @return the converted entity
     */
    @Mapping(target = "customFields", expression = "java(updateModelCustomActivityTypeField(dto.customFields(), activityType.getCustomFields()))")
    abstract public ActivityType updateModel(UpdateActivityTypeDTO dto, @MappingTarget ActivityType activityType);

    /**
     * Convert the {@link Work} to a {@link WorkDTO}
     *
     * @param work the entity to convert
     * @return the converted DTO
     */
    @Mapping(target = "workType", expression = "java(toWorkTypeDTOFromWorkTypeId(work.getWorkTypeId()))")
    @Mapping(target = "shopGroup", expression = "java(toShopGroupDTOById(work.getShopGroupId()))")
    @Mapping(target = "location", expression = "java(toLocationDTOById(work.getLocationId()))")
    @Mapping(target = "customFields", expression = "java(toCustomFieldValuesDTOForWork(work.getWorkTypeId(), work.getCustomFields()))")
    @Mapping(target = "domain", expression = "java(toDomainDTO(work.getDomainId()))")
    @Mapping(target = "changesHistory", expression = "java(getChanges(work.getId(), workDetailsOptionDTO))")
    @Mapping(target = "project", expression = "java(toLOVValueDTO(work.getProject()))")
    abstract public WorkDTO toDTO(Work work, WorkDetailsOptionDTO workDetailsOptionDTO);

    /**
     * Fetch the list of changed on the work
     *
     * @param workId the entity to convert
     * @return the list of changed
     */
    public List<ModelChangesHistoryDTO> getChanges(String workId, WorkDetailsOptionDTO workDetailsOptionDTO) {
        if(workDetailsOptionDTO==null || workDetailsOptionDTO.changes()==null || workDetailsOptionDTO.changes().isPresent()==false) return Collections.emptyList();
        if(workDetailsOptionDTO.changes().get()==false) return Collections.emptyList();
        return modelHistoryService.findChangesByModelId(Work.class, workId);
    }

    /**
     * Convert the {@link Activity} to a {@link ActivityDTO}
     *
     * @param activity the entity to convert
     * @return the converted DTO
     */
    @Mapping(target = "activityType", expression = "java(toActivityTypeDTOFromActivityTypeId(activity.getActivityTypeId()))")
    @Mapping(target = "access", expression = "java(getActivityAuthorizationByWorkId(activity.getWorkId()))")
    @Mapping(target = "customFields", expression = "java(toCustomFieldValuesDTOForActivity(activity.getActivityTypeId(), activity.getCustomFields()))")
    @Mapping(target = "domain", expression = "java(toDomainDTO(activity.getDomainId()))")
    @Mapping(target = "project", expression = "java(toLOVValueDTO(activity.getProject()))")
    abstract public ActivityDTO toDTO(Activity activity);

    /**
     * Convert the {@link Activity} to a {@link ActivitySummaryDTO}
     *
     * @param activity the entity to convert
     * @return the converted DTO
     */
    @Mapping(target = "activityType", expression = "java(toActivityTypeDTOFromActivityTypeId(activity.getActivityTypeId()))")
    @Mapping(target = "access", expression = "java(getActivityAuthorizationByWorkId(activity.getWorkId()))")
    @Mapping(target = "domain", expression = "java(toDomainDTO(activity.getDomainId()))")
    abstract public ActivitySummaryDTO toSummaryDTO(Activity activity);

    /**
     * Convert the {@link WorkQueryParameterDTO} to a {@link WorkQueryParameter}
     *
     * @param workQueryParameterDTO the DTO to convert
     * @return the converted entity
     */
    abstract public WorkQueryParameter toModel(WorkQueryParameterDTO workQueryParameterDTO);

    abstract public ActivityQueryParameter toModel(ActivityQueryParameterDTO activityQueryParameterDTO);

    abstract public WATypeCustomField toModel(WATypeCustomFieldDTO WATypeCustomFieldDTO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    abstract public WATypeCustomField toModel(String id, String name, WATypeCustomFieldDTO WATypeCustomFieldDTO);


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
                                toValueDTO(
                                        tryToLOV(customAttribute.getValue())
                                )
                        )
                        .build()
        ).toList();
    }

    /**
     * Convert the {@link CustomField} to a {@link CustomFieldDTO}
     *
     * @param activityTypeId         the activity id
     * @param customAttributesValues the list of custom attribute
     * @return the list of the {@link CustomFieldDTO}
     */
    public List<CustomFieldDTO> toCustomFieldValuesDTOForActivity(String activityTypeId, List<CustomField> customAttributesValues) {
        return customAttributesValues.stream().map(
                customAttribute -> CustomFieldDTO.builder()
                        .id(customAttribute.getId())
                        .name(
                                activityTypeRepository
                                        .findCustomFieldById(activityTypeId, customAttribute.getId())
                                        .map(WATypeCustomField::getName)
                                        .orElseThrow(
                                                () -> CustomAttributeNotFound.notFoundById()
                                                        .id(customAttribute.getId())
                                                        .build()
                                        )
                        )
                        .value(
                                toValueDTO(
                                        tryToLOV(customAttribute.getValue())
                                )
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
        if (value.getClass().isAssignableFrom(StringValue.class)) {
            var lovElementFound = lovService.findLovValueByIdNoException(((StringValue) value).getValue());
            if (lovElementFound.isPresent()) {
                return StringValue.builder().value(lovElementFound.get().getValue()).build();
            }
        }
        return value;
    }

    /**
     * Convert the {@link WATypeCustomFieldDTO} to a {@link WATypeCustomField}
     *
     * @param customFieldsDTO the lists of the new custom fields
     * @param customFields    the list of the old custom fields
     * @return the converted entity
     */
    public List<WATypeCustomField> updateModelCustomActivityTypeField(List<WATypeCustomFieldDTO> customFieldsDTO, List<WATypeCustomField> customFields) {
        List<WATypeCustomField> updatedCustomAttributesList = new ArrayList<>();
        customFieldsDTO.forEach(
                customFieldDTO -> {
                    // try to find the custom field in the old list
                    Optional<WATypeCustomField> customField = Objects.requireNonNullElse(customFields, Collections.<WATypeCustomField>emptyList()).stream()
                            .filter(
                                    customField1 -> customField1.getId().equals(customFieldDTO.id())
                            ).findFirst();

                    if (customField.isPresent()) {
                        updatedCustomAttributesList.add(toModel(customFieldDTO));
                    } else {
                        updatedCustomAttributesList.add(
                                toModel(
                                        UUID.randomUUID().toString(),
                                        StringUtility.toCamelCase(customFieldDTO.label()),
                                        customFieldDTO
                                )
                        );
                    }
                }
        );
        return updatedCustomAttributesList;
    }

    /**
     * Get the authorization level on activity
     */
    public AuthorizationTypeDTO getActivityAuthorizationByWorkId(String workId) {
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
                workId,
                authentication.getCredentials().toString()
        )) return AuthorizationTypeDTO.Write;
        return AuthorizationTypeDTO.Read;
    }

    /**
     * Convert the {@link ActivityStatusDTO} to a {@link ActivityStatusLog}
     *
     * @param activityStatusDTO the DTO to convert
     * @return the converted entity
     */
    public ActivityStatusLog mapWorkStatusToLogModel(ActivityStatusDTO activityStatusDTO) {
        return ActivityStatusLog.builder().status(activityStatusDTO != null ? toModel(activityStatusDTO) : ActivityStatus.New).build();
    }

    /**
     * Convert the {@link String} shop group id to a {@link ShopGroupDTO}
     *
     * @param shopGroupId the id of the shop group
     * @return the converted DTO
     */
    public ShopGroupDTO toShopGroupDTOById(String shopGroupId) {
        if (shopGroupId == null) return null;
        return shopGroupService.findById(shopGroupId);
    }

    /**
     * Convert the {@link String} location id to a {@link LocationDTO}
     *
     * @param locationId the id of the location
     * @return the converted DTO
     */
    public LocationDTO toLocationDTOById(String locationId) {
        if (locationId == null) return null;
        return locationService.findById(locationId);
    }

    /**
     * Convert the {@link String} work type id to a {@link WorkTypeDTO}
     *
     * @param workTypeId the id of the work type
     * @return the converted DTO
     */
    public WorkTypeDTO toWorkTypeDTOFromWorkTypeId(String workTypeId) {
        return wrapCatch(
                () -> workTypeRepository.findById(workTypeId).map(this::toDTO).orElseThrow(
                        () -> WorkTypeNotFound.notFoundById()
                                .errorCode(-1)
                                .workId(workTypeId)
                                .build()
                ),
                -1
        );
    }

    public ActivityTypeDTO toActivityTypeDTOFromActivityTypeId(String activityTypeId) {
        return wrapCatch(
                () -> activityTypeRepository.findById(activityTypeId).map(this::toDTO).orElseThrow(
                        () -> ActivityTypeNotFound.notFoundById()
                                .errorCode(-1)
                                .activityTypeId(activityTypeId)
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
    protected void afterMapping(@MappingTarget final ActivityDTO.ActivityDTOBuilder target, Activity source) {
        var listOfReferenced = lovService.getLOVFieldReference(LOVDomainTypeDTO.Activity, source.getActivityTypeId()).keySet();
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

    @AfterMapping
    protected void afterMapping(@MappingTarget final WorkDTO.WorkDTOBuilder target, Work source) {
        var listOfReferenced = lovService.getLOVFieldReference(LOVDomainTypeDTO.Work, source.getWorkTypeId()).keySet();
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
