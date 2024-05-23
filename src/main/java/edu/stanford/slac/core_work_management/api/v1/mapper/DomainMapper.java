package edu.stanford.slac.core_work_management.api.v1.mapper;

import edu.stanford.slac.core_work_management.api.v1.dto.DomainDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewDomainDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.WorkStatusCountStatisticsDTO;
import edu.stanford.slac.core_work_management.model.Domain;
import edu.stanford.slac.core_work_management.model.WorkStatusCountStatistics;
import edu.stanford.slac.core_work_management.service.StringUtility;
import org.mapstruct.*;

import javax.swing.text.Utilities;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class DomainMapper {
    @Mapping(target = "name", source = "name", qualifiedByName = "normalizeName")
    public abstract Domain toModel(NewDomainDTO newDomainDTO);

    public abstract DomainDTO toDTO(Domain domain);

    @Named("normalizeName")
    public String modifyName(String name) {
        return name.trim().toLowerCase().replace(" ", "-");
    }

    public abstract WorkStatusCountStatisticsDTO toDTO(WorkStatusCountStatistics model);

    public Map<String,List<WorkStatusCountStatisticsDTO>> map(Map<String, List<WorkStatusCountStatistics>> value) {
        if(value == null) return new HashMap<>();
        return value.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().stream().map(this::toDTO).toList()
                        )
                );
    }
}