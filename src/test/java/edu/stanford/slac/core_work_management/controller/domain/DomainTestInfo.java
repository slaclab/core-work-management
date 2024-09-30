package edu.stanford.slac.core_work_management.controller.domain;

import edu.stanford.slac.core_work_management.api.v1.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;

public class DomainTestInfo {
    public DomainDTO domain;
    public List<ShopGroupDTO> shopGroups = new ArrayList<>();
    public List<WorkTypeDTO> workTypes = new ArrayList<>();;
    public List<LOVElementDTO> lovElementBucketType = new ArrayList<>();;
    public List<LOVElementDTO> lovElementBucketStatus = new ArrayList<>();;
    public List<LocationDTO> locationsIDs = new ArrayList<>();

    public WorkTypeDTO getWorkTypeByName(String workTypeName) {
       return workTypes.stream().filter(wt -> wt.title().compareToIgnoreCase(workTypeName)==0).findFirst().orElseThrow(
                () -> new RuntimeException("Work type not found: " + workTypeName)
       );
    }

    public LocationDTO getLocationByName(String locationName) {
        return locationsIDs.stream().filter(l -> l.name().compareToIgnoreCase(locationName)==0).findFirst().orElseThrow(
                () -> new RuntimeException("Location not found: " + locationName)
        );
    }

    public ShopGroupDTO getShopGroupByName(String shopGroupName) {
        return shopGroups.stream().filter(sg -> sg.name().compareToIgnoreCase(shopGroupName)==0).findFirst().orElseThrow(
                () -> new RuntimeException("Shop group not found: " + shopGroupName)
        );
    }
}
