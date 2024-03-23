package edu.stanford.slac.core_work_management.api.v1.mapper;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationResourceDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import edu.stanford.slac.core_work_management.exception.ActivityTypeCustomAttributeNotFound;
import edu.stanford.slac.core_work_management.exception.ActivityTypeNotFound;
import edu.stanford.slac.core_work_management.exception.WorkTypeNotFound;
import edu.stanford.slac.core_work_management.model.*;
import edu.stanford.slac.core_work_management.model.value.*;
import edu.stanford.slac.core_work_management.repository.ActivityTypeRepository;
import edu.stanford.slac.core_work_management.repository.WorkTypeRepository;
import edu.stanford.slac.core_work_management.service.LOVService;
import edu.stanford.slac.core_work_management.service.LocationService;
import edu.stanford.slac.core_work_management.service.ShopGroupService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.any;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;
import static edu.stanford.slac.core_work_management.config.AuthorizationStringConfig.SHOP_GROUP_AUTHORIZATION_TEMPLATE;
import static edu.stanford.slac.core_work_management.config.AuthorizationStringConfig.WORK_AUTHORIZATION_TEMPLATE;
import static java.util.Collections.emptyList;

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
    WorkTypeRepository workTypeRepository;
    @Autowired
    ActivityTypeRepository activityTypeRepository;

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
    abstract public Work toModel(NewWorkDTO newWorkDTO);

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
     * Convert the {@link ActivityType} to a {@link ActivityTypeDTO}
     *
     * @param activityType the entity to convert
     * @return the converted DTO
     */
    abstract public ActivityTypeDTO toDTO(ActivityType activityType);

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
    abstract public Activity toModel(NewActivityDTO newActivityDTO, String workId);

    /**
     * Update the {@link Activity} with the data from the {@link UpdateActivityDTO}
     *
     * @param dto  the DTO with the data to update
     * @param work the entity to update
     */
    abstract public void updateModel(UpdateActivityDTO dto, @MappingTarget Activity work);

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
    @Mapping(target = "accessList", expression = "java(getAuthorizationByWork(work))")
    abstract public WorkDTO toDTO(Work work);

    /**
     * Convert the {@link Activity} to a {@link ActivityDTO}
     *
     * @param activity the entity to convert
     * @return the converted DTO
     */
    @Mapping(target = "activityType", expression = "java(toActivityTypeDTOFromActivityTypeId(activity.getActivityTypeId()))")
    @Mapping(target = "access", expression = "java(getActivityAuthorizationByWorkId(activity.getWorkId()))")
    @Mapping(target = "customFields", expression = "java(toCustomFieldValuesDTO(activity.getActivityTypeId(), activity.getCustomFields()))")
//    @Mapping(target = "schedulingProperty", expression = "java(toLOVValueDTO(activity.getSchedulingProperty()))")
    abstract public ActivityDTO toDTO(Activity activity);

    @Mapping(target = "activityType", expression = "java(toActivityTypeDTOFromActivityTypeId(activity.getActivityTypeId()))")
    @Mapping(target = "access", expression = "java(getActivityAuthorizationByWorkId(activity.getWorkId()))")
    abstract public ActivitySummaryDTO toSummaryDTO(Activity activity);

    /**
     * Convert the {@link WorkQueryParameterDTO} to a {@link WorkQueryParameter}
     *
     * @param workQueryParameterDTO the DTO to convert
     * @return the converted entity
     */
    abstract public WorkQueryParameter toModel(WorkQueryParameterDTO workQueryParameterDTO);

    abstract public ActivityQueryParameter toModel(ActivityQueryParameterDTO activityQueryParameterDTO);

    abstract public ActivityTypeCustomField toModel(ActivityTypeCustomFieldDTO activityTypeCustomFieldDTO);

    @Mapping(target = "id", source = "id")
    abstract public ActivityTypeCustomField toModel(String id, ActivityTypeCustomFieldDTO activityTypeCustomFieldDTO);


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
     * Convert the {@link CustomField} to a {@link CustomFieldDTO}
     *
     * @param activityTypeId         the activity id
     * @param customAttributesValues the list of custom attribute
     * @return the list of the {@link CustomFieldDTO}
     */
    public List<CustomFieldDTO> toCustomFieldValuesDTO(String activityTypeId, List<CustomField> customAttributesValues) {
        return customAttributesValues.stream().map(
                customAttribute -> CustomFieldDTO.builder()
                        .id(customAttribute.getId())
                        .name(
                                activityTypeRepository
                                        .findCustomFiledById(activityTypeId, customAttribute.getId())
                                        .map(ActivityTypeCustomField::getName)
                                        .orElseThrow(
                                                () -> ActivityTypeCustomAttributeNotFound.notFoundById()
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
     * Convert the {@link ActivityTypeCustomFieldDTO} to a {@link ActivityTypeCustomField}
     *
     * @param customFieldsDTO the lists of the new custom fields
     * @param customFields    the list of the old custom fields
     * @return the converted entity
     */
    public List<ActivityTypeCustomField> updateModelCustomActivityTypeField(List<ActivityTypeCustomFieldDTO> customFieldsDTO, List<ActivityTypeCustomField> customFields) {
        List<ActivityTypeCustomField> updatedCustomAttributesList = new ArrayList<>();
        customFieldsDTO.forEach(
                customFieldDTO -> {
                    // try to find the custom field in the old list
                    Optional<ActivityTypeCustomField> customField = Objects.requireNonNullElse(customFields, Collections.<ActivityTypeCustomField>emptyList()).stream()
                            .filter(
                                    customField1 -> customField1.getId().equals(customFieldDTO.id())
                            ).findFirst();

                    if (customField.isPresent()) {
                        updatedCustomAttributesList.add(toModel(customFieldDTO));
                    } else {
                        updatedCustomAttributesList.add(
                                toModel(UUID.randomUUID().toString(), customFieldDTO)
                        );
                    }
                }
        );
        return updatedCustomAttributesList;
    }

    /**
     * Get the authorization level on work
     */
    public List<AuthorizationResourceDTO> getAuthorizationByWork(Work workId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            // if the DTO has been requested by an anonymous user, then the access level is Read
            // in other case will should have been blocked by the security layer
            return emptyList();
        }

        List<AuthorizationResourceDTO> accessList = new ArrayList<>();
        //check if it's a root
        boolean isRoot = authService.checkForRoot(authentication);
        // check if user can write normal field
        accessList.add(AuthorizationResourceDTO.builder()
                .field("*")
                .authorizationType(
                        any(
                                // a root users
                                () -> authService.checkForRoot(authentication),
                                // or a user that has the right as writer on the work
                                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                        authentication,
                                        AuthorizationTypeDTO.Write,
                                        WORK_AUTHORIZATION_TEMPLATE.formatted(workId)
                                ),
                                // user of the shop group are always treated as admin on the work
                                () -> shopGroupService.checkContainsAUserEmail(
                                        // fire not found work exception
                                        workId.getShopGroupId(),
                                        authentication.getCredentials().toString()
                                )
                        ) ? AuthorizationTypeDTO.Write : AuthorizationTypeDTO.Read)
                .build());

        // check if can modify location
        accessList.add(AuthorizationResourceDTO.builder()
                .field("location")
                .authorizationType(
                        any(
                                // a root users
                                () -> isRoot,
                                // or a user that has the right as admin on the work
                                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                        authentication,
                                        AuthorizationTypeDTO.Admin,
                                        WORK_AUTHORIZATION_TEMPLATE.formatted(workId)
                                )
                        ) ? AuthorizationTypeDTO.Admin : AuthorizationTypeDTO.Read)
                .build());

        // check if can modify assignTo
        accessList.add(AuthorizationResourceDTO.builder()
                .field("assignTo")
                .authorizationType(
                        any(
                                // a root users
                                () -> isRoot,
                                // or a user that is the leader of the group
                                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                        authentication,
                                        AuthorizationTypeDTO.Admin,
                                        SHOP_GROUP_AUTHORIZATION_TEMPLATE.formatted(workId.getShopGroupId())
                                )
                        ) ? AuthorizationTypeDTO.Admin : AuthorizationTypeDTO.Read)
                .build());

        return accessList;
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
                return DateValue
                        .builder()
                        .value(LocalDate.parse(value.value(), DateTimeFormatter.ISO_LOCAL_DATE))
                        .build();
            }
            case DateTime -> {
                return DateTimeValue
                        .builder()
                        .value(LocalDateTime.parse(value.value(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .build();
            }
            default -> throw ControllerLogicException.builder()
                    .errorCode(-4)
                    .errorMessage("Invalid attribute type")
                    .errorDomain("InventoryElementMapper::toElementAttributeWithClass")
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
}
