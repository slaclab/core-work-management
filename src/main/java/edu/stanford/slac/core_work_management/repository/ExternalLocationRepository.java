package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.cis_api.api.InventoryElementControllerApi;
import edu.stanford.slac.core_work_management.cis_api.dto.InventoryElementDTO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Repository
@AllArgsConstructor
public class ExternalLocationRepository {
    private final InventoryElementControllerApi inventoryElementControllerApi;

    /**
     * Get the location info from the external inventory
     *
     * @param externalLocationIdentifier the external location identifier
     * @return the location info
     */
    public InventoryElementDTO getLocationInfo(String externalLocationIdentifier) {
        String[] domainElementIds = externalLocationIdentifier.split("/");
        assertion(
                ()->domainElementIds.length==2,
                ControllerLogicException.builder()
                        .errorCode(-1)
                        .errorMessage("The externalLocationIdentifier should be in the form of domainId/elementId")
                        .errorDomain("ExternalLocationRepository::getLocationInfo")
                        .build()
        );

        var foundElement = wrapCatch(
                ()->inventoryElementControllerApi.findElementById(domainElementIds[0], domainElementIds[1]),
                -2,
                "Error finding the location in the external inventory"
        );

        assertion(
                ()->foundElement!=null && foundElement.getErrorCode()==0,
                ControllerLogicException.builder()
                        .errorCode(foundElement.getErrorCode())
                        .errorMessage(foundElement.getErrorMessage())
                        .errorDomain(foundElement.getErrorDomain())
                        .build()
        );
        return foundElement.getPayload();
    }
}
