package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
import edu.stanford.slac.core_work_management.api.v1.dto.LocationDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.LocationFilterDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLocationDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.mapper.LocationMapper;
import edu.stanford.slac.core_work_management.api.v1.mapper.ShopGroupMapper;
import edu.stanford.slac.core_work_management.exception.LocationNotFound;
import edu.stanford.slac.core_work_management.exception.ShopGroupNotFound;
import edu.stanford.slac.core_work_management.model.ShopGroup;
import edu.stanford.slac.core_work_management.repository.LocationRepository;
import edu.stanford.slac.core_work_management.repository.ShopGroupRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

/**
 * Service to manage locations
 */
@Service
@Validated
@AllArgsConstructor
public class LocationService {
    private final LocationMapper locationMapper;
    private final LocationRepository locationRepository;
    private final ShopGroupService shopGroupService;
    private final PeopleGroupService peopleGroupService;
    /**
     * Create a new location
     *
     * @param newLocationDTO the DTO to create the location
     * @return the id of the created location
     */
    public String createNew(@Valid NewLocationDTO newLocationDTO) {
        if(newLocationDTO.parentId()!=null && !newLocationDTO.parentId().isBlank()) {
            assertion(
                    ()->locationRepository.existsById(newLocationDTO.parentId()),
                    LocationNotFound
                            .notFoundById()
                            .errorCode(-1)
                            .locationId(newLocationDTO.parentId())
                            .build()
            );
        }

        // check if are manager exists
        assertion(
                ()->peopleGroupService.findPersonByMain(newLocationDTO.locationManagerUserId())!=null,
                ShopGroupNotFound.notFoundById()
                        .errorCode(-2)
                        .shopGroupId(
                                newLocationDTO.locationShopGroupId()
                        )
                        .build()
        );

        // check if shop group exists
        assertion(
                ()->shopGroupService.exists(newLocationDTO.locationShopGroupId()),
                ShopGroupNotFound.notFoundById()
                        .errorCode(-3)
                        .shopGroupId(
                                newLocationDTO.locationShopGroupId()
                        )
                        .build()
        );

        var newSavedLocation = wrapCatch(
                () -> locationRepository.save(locationMapper.toModel(newLocationDTO)),
                -1
        );
        return newSavedLocation.getId();
    }

    /**
     * Find a location by id
     *
     * @param locationId the id of the location
     * @return the location
     */
    public LocationDTO findById(String locationId) {
        return wrapCatch(
                () -> locationRepository.findById(locationId),
                -1
        )
                .map(locationMapper::toDTO)
                .orElseThrow(
                        () -> LocationNotFound
                                .notFoundById()
                                .errorCode(-1)
                                .locationId(locationId)
                                .build()
                );
    }

    /**
     * Find all locations
     *
     * @param locationFilterDTO the filter to use
     * @return the list of locations
     */
    public List<LocationDTO> findAll(LocationFilterDTO locationFilterDTO) {
        var filter = locationMapper.toModel(locationFilterDTO);
        return wrapCatch(
                () -> locationRepository.findByLocationFilter(filter),
                -1
        )
                .stream()
                .map(locationMapper::toDTO)
                .toList();
    }
}
