/*
 * -----------------------------------------------------------------------------
 * Title      : LocationService
 * ----------------------------------------------------------------------------
 * File       : LocationService.java
 * Author     : Claudio Bisegni, bisegni@slac.stanford.edu
 * ----------------------------------------------------------------------------
 * This file is part of core-work-management. It is subject to
 * the license terms in the LICENSE.txt file found in the top-level directory
 * of this distribution and at:
 * <a href="https://confluence.slac.stanford.edu/display/ppareg/LICENSE.html"/>.
 * No part of core-work-management, including this file, may be
 * copied, modified, propagated, or distributed except according to the terms
 *  contained in the LICENSE.txt file.
 * ----------------------------------------------------------------------------
 */

package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.ad.eed.baselib.exception.PersonNotFound;
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
import edu.stanford.slac.core_work_management.api.v1.dto.LocationDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.LocationFilterDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLocationDTO;
import edu.stanford.slac.core_work_management.api.v1.mapper.LocationMapper;
import edu.stanford.slac.core_work_management.cis_api.dto.InventoryElementDTO;
import edu.stanford.slac.core_work_management.exception.DomainNotFound;
import edu.stanford.slac.core_work_management.exception.LocationNotFound;
import edu.stanford.slac.core_work_management.model.Location;
import edu.stanford.slac.core_work_management.repository.ExternalLocationRepository;
import edu.stanford.slac.core_work_management.repository.LocationRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.*;

/**
 * Service to manage locations
 */
@Log4j2
@Service
@Validated
@AllArgsConstructor
public class LocationService {
    private final DomainService domainService;
    private final LocationMapper locationMapper;
    private final LocationRepository locationRepository;
    private final PeopleGroupService peopleGroupService;
    private final ExternalLocationRepository externalLocationRepository;

    /**
     * Create a new location
     *
     * @param newLocationDTO the DTO to create the location
     * @return the id of the created location
     */
    public String createNew(@NotNull String domainId, @Valid NewLocationDTO newLocationDTO) {
        InventoryElementDTO externalLocationDTO;
        if (
                newLocationDTO.externalLocationIdentifier() != null &&
                        !newLocationDTO.externalLocationIdentifier().isBlank()
        ) {
            // acquire external location info
            externalLocationDTO = externalLocationRepository.getLocationInfo(newLocationDTO.externalLocationIdentifier());
        } else {
            externalLocationDTO = null;
        }
        return saveLocation(locationMapper.toModel(domainId, null, newLocationDTO, externalLocationDTO)).getId();
    }

    /**
     * Create a new child location
     *
     * @param newLocationDTO the DTO to create the location
     * @return the id of the created location
     */
    public String createNewChild(@NotNull String domainId, @NotNull String parentId, @Valid NewLocationDTO newLocationDTO) {
        InventoryElementDTO externalLocationDTO;
        assertion(
                () -> locationRepository.existsById(parentId),
                LocationNotFound
                        .notFoundById()
                        .errorCode(-1)
                        .locationId(parentId)
                        .build()
        );

        if (
                newLocationDTO.externalLocationIdentifier() != null &&
                        !newLocationDTO.externalLocationIdentifier().isBlank()
        ) {
            // acquire external location info
            externalLocationDTO = externalLocationRepository.getLocationInfo(newLocationDTO.externalLocationIdentifier());
        } else {
            externalLocationDTO = null;
        }
        return saveLocation(locationMapper.toModel(domainId, parentId, newLocationDTO, externalLocationDTO)).getId();
    }

    /**
     * Check if a location exists by domain id and id
     *
     * @param domainId the domain id
     * @param locationId the id
     * @return true if the location exists, false otherwise
     */
    public boolean existsByDomainIdAndId(String domainId, String locationId) {
        return  wrapCatch(
                () -> locationRepository.existsByDomainIdAndId(domainId, locationId),
                -1
        );
    }

    /**
     * Find a location by id
     *
     * @param locationId the id of the location
     * @return the location
     */
    public LocationDTO findById(String domainId, String locationId) {
        return wrapCatch(
                () -> locationRepository.findByDomainIdAndId(domainId, locationId),
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
     * @param domainId
     * @param locationFilterDTO the filter to use
     * @return the list of locations
     */
    public List<LocationDTO> findAll(@NotNull String domainId, LocationFilterDTO locationFilterDTO) {
        var filter = locationMapper.toModel(locationFilterDTO);
        return wrapCatch(
                () -> locationRepository.findByLocationFilter(domainId, filter),
                -1
        )
                .stream()
                .map(locationMapper::toDTO)
                .toList();
    }

    /**
     * Create a new location
     *
     * @param location the DTO to create the shop group
     * @return the id of the created shop group
     */
    private Location saveLocation(@Valid Location location) {
        // check for domain if it is present
        assertion(
                DomainNotFound
                        .notFoundById()
                        .errorCode(-1)
                        .id(location.getDomainId())
                        .build(),
                () -> all(
                        () -> location.getDomainId() != null && !location.getDomainId().isBlank(),
                        () -> domainService.existsById(location.getDomainId())
                )
        );

        // check if are manager exists
        assertion(
                () -> peopleGroupService.findPersonByEMail(location.getLocationManagerUserId()) != null,
                PersonNotFound
                        .personNotFoundBuilder()
                        .errorCode(-2)
                        .email(location.getLocationManagerUserId())
                        .build()
        );

        // save the location
        return wrapCatch(() -> locationRepository.save(location), -3);
    }
}
