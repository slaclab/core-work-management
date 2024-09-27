package edu.stanford.slac.core_work_management.controller;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.auth.JWTHelper;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.core_work_management.api.v1.dto.*;
import org.assertj.core.api.AssertionsForClassTypes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Service()
public class TestControllerHelperService {
    private final JWTHelper jwtHelper;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public TestControllerHelperService(ObjectMapper objectMapper, JWTHelper jwtHelper, AppProperties appProperties) {
        this.jwtHelper = jwtHelper;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;

    }

    public ApiResultResponse<String> attachmentControllerCreateNew(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            MockMultipartFile file) throws Exception {
        var requestBuilder = multipart("/v1/attachment").file(file);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result_upload = mockMvc.perform(
                        requestBuilder
                )
                .andExpect(resultMatcher)
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result_upload.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        ApiResultResponse<String> res = new ObjectMapper().readValue(result_upload.getResponse().getContentAsString(), new TypeReference<>() {
        });
        assertThat(res.getErrorCode()).isEqualTo(0);
        return res;
    }

    /**
     * Create a dummy PDF attachment
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @return the id of the newly created attachment
     * @throws Exception the exception
     */
    public ApiResultResponse<String> createDummyPDFAttachment(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "test".getBytes());
        return attachmentControllerCreateNew(
                mockMvc,
                resultMatcher,
                userInfo,
                new MockMultipartFile(
                        "uploadFile",
                        "file.pdf",
                        MediaType.APPLICATION_PDF_VALUE,
                        "<<pdf data>>".getBytes(StandardCharsets.UTF_8)
                )
        );
    }

    /**
     * Check if the file is correctly downloaded
     *
     * @param mockMvc
     * @param resultMatcher
     * @param userInfo
     * @param attachmentID
     * @param mediaType
     * @throws Exception
     */
    public void checkDownloadedFile(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String attachmentID,
            String mediaType) throws Exception {
        var requestBuilder = get("/v1/attachment/{id}/download", attachmentID)
                .contentType(mediaType);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        requestBuilder
                )
                .andExpect(resultMatcher)
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        AssertionsForClassTypes.assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(0);
        AssertionsForClassTypes.assertThat(result.getResponse().getContentType()).isEqualTo(mediaType);
    }

    /**
     * Check if the file is correctly downloaded
     *
     * @param mockMvc
     * @param resultMatcher
     * @param userInfo
     * @param attachmentID
     * @param mediaType
     * @throws Exception
     */
    public void checkDownloadedPreview(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String attachmentID,
            String mediaType) throws Exception {
        var requestBuilder = get("/v1/attachment/{id}/preview.jpg", attachmentID)
                .contentType(mediaType);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        requestBuilder
                )
                .andExpect(resultMatcher)
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        AssertionsForClassTypes.assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(0);
        AssertionsForClassTypes.assertThat(result.getResponse().getContentType()).isEqualTo(mediaType);
    }

    /**
     * Create new domain
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @return the id of the newly created domain
     * @throws Exception the exception
     */
    public ApiResultResponse<String> domainControllerCreateNewDomain(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            NewDomainDTO newDomainDTO
    ) throws Exception {
        var requestBuilder = post("/v1/domain")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newDomainDTO));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Find a domain by his id
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @param domainId      the id of the domain to update
     * @return the domain found of the newly created domain
     * @throws Exception the exception
     */
    public ApiResultResponse<DomainDTO> domainControllerFindById(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String domainId
    ) throws Exception {
        var requestBuilder = get("/v1/domain/{domainId}", domainId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Find all domain
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @return the list of domain
     * @throws Exception the exception
     */
    public ApiResultResponse<List<DomainDTO>> domainControllerFindAll(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo
    ) throws Exception {
        var requestBuilder = get("/v1/domain")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Create new shop group
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @return the id of the newly created shop group
     * @throws Exception the exception
     */
    public ApiResultResponse<String> shopGroupControllerCreateNew(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String domainId,
            NewShopGroupDTO newShopGroupDTO
    ) throws Exception {
        var requestBuilder = post("/v1/domain/{domainId}/shop-group", domainId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newShopGroupDTO));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Update a shop group
     *
     * @param mockMvc            the mock mvc
     * @param resultMatcher      the result matcher
     * @param userInfo           the user info
     * @param id                 the id of the shop group to update
     * @param updateShopGroupDTO the update shop group dto
     * @return the id of the newly created shop group
     * @throws Exception the exception
     */
    public ApiResultResponse<String> shopGroupControllerUpdate(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String domainId,
            String id,
            UpdateShopGroupDTO updateShopGroupDTO
    ) throws Exception {
        var requestBuilder = put("/v1/domain/{domainId}/shop-group/{id}", domainId, id)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateShopGroupDTO));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Get all shop groups
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @return the list of shop groups
     * @throws Exception the exception
     */
    public ApiResultResponse<List<ShopGroupDTO>> shopGroupControllerFindAll(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String domainId
    ) throws Exception {
        var requestBuilder = get("/v1/domain/{domainId}/shop-group", domainId)
                .accept(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Get shop group by id
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @return the list of shop groups
     * @throws Exception the exception
     */
    public ApiResultResponse<ShopGroupDTO> shopGroupControllerFindById(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String domainId,
            String id
    ) throws Exception {
        var requestBuilder = get("/v1/domain/{domainId}/shop-group/{id}", domainId, id)
                .accept(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Create new location
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @return the id of the newly created location
     * @throws Exception the exception
     */
    public ApiResultResponse<String> locationControllerCreateNewRoot(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String domainId,
            NewLocationDTO newLocationDTO
    ) throws Exception {
        var requestBuilder = post("/v1/domain/{domainId}/location", domainId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newLocationDTO));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Create new child location
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @return the id of the newly created location
     * @throws Exception the exception
     */
    public ApiResultResponse<String> locationControllerCreateNewChild(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String parentLocationId,
            NewLocationDTO newLocationDTO
    ) throws Exception {
        var requestBuilder = post("/v1/location/{locationId}", parentLocationId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newLocationDTO));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Find location by id
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @param locationId    the location id
     * @return the location dto
     * @throws Exception the exception
     */
    public ApiResultResponse<LocationDTO> locationControllerFindById(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String domainId,
            String locationId
    ) throws Exception {
        var requestBuilder = get("/v1/domain/{domainId}/location/{locationId}", domainId, locationId)
                .accept(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Find all locations
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @param filter        the filter
     * @return the list of locations
     * @throws Exception the exception
     */
    public ApiResultResponse<List<LocationDTO>> locationControllerFindAll(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String domainId,
            Optional<String> filter,
            Optional<String> externalId
    ) throws Exception {
        var requestBuilder = get("/v1/domain/{domainId}/location", domainId)
                .accept(MediaType.APPLICATION_JSON);
        filter.ifPresent(s -> requestBuilder.param("filter", s));
        externalId.ifPresent(s -> requestBuilder.param("externalId", s));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * get all work type new work type
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @return the id of the newly created work type
     * @throws Exception the exception
     */
    public ApiResultResponse<List<WorkTypeDTO>> domainControllerFindAllWorkTypes(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String domainId
    ) throws Exception {
        var requestBuilder = get("/v1/domain/{domainId}/work-type", domainId)
                .contentType(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }


    /**
     * Create new work
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @param newWorkDTO    the new work dto
     * @return the id of the newly created work
     */
    public ApiResultResponse<String> workControllerCreateNew(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String domainId,
            NewWorkDTO newWorkDTO
    ) throws Exception {
        var requestBuilder = post("/v1/domain/{domainId}/work", domainId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newWorkDTO));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Create new work
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @param newWorkDTO    the new work dto
     * @param logIf         the log if true
     * @return the id of the newly created work
     */
    public ApiResultResponse<String> workControllerCreateNew(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String domainId,
            NewWorkDTO newWorkDTO,
            Optional<Boolean> logIf
    ) throws Exception {
        var requestBuilder = post("/v1/domain/{domainId}/work", domainId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newWorkDTO));
        logIf.ifPresent(aBoolean -> requestBuilder.param("logIf", aBoolean.toString()));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Update a work
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @param workId        the id of the work to update
     * @param updateWorkDTO the update work dto
     * @return the id of the newly created work
     */
    public ApiResultResponse<Boolean> workControllerUpdate(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String domainId,
            String workId,
            UpdateWorkDTO updateWorkDTO
    ) throws Exception {
        var requestBuilder = put("/v1/domain/{domainId}/work/{workId}", domainId, workId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateWorkDTO));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Associate a work to a bucket
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @param workId        the id of the work to update
     * @param bucketId      the update work dto
     * @return the id of the newly created work
     */
    public ApiResultResponse<Boolean> workControllerAssociateWorkToBucket(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String domainId,
            String workId,
            String bucketId,
            Optional<Boolean> move
    ) throws Exception {
        var requestBuilder = put("/v1/domain/{domainId}/work/{workId}/buket/{bucketId}", domainId, workId, bucketId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);
        move.ifPresent(aBoolean -> requestBuilder.param("move", aBoolean.toString()));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Find a work by id
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @param workId        the i dof the work to find
     * @return the work
     * @throws Exception the exception
     */
    public ApiResultResponse<WorkDTO> workControllerFindWorkById(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String domainId,
            String workId,
            WorkDetailsOptionDTO workDetailsOptionDTO
    ) throws Exception {
        var requestBuilder = get("/v1/domain/{domainId}/work/{workId}", domainId, workId)
                .contentType(MediaType.APPLICATION_JSON);
        if (workDetailsOptionDTO != null && workDetailsOptionDTO.changes() != null) {
            requestBuilder.param("changes", String.valueOf(workDetailsOptionDTO.changes()));
        }
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Find all work history by id
     *
     * @param mockMvc
     * @param resultMatcher
     * @param userInfo
     * @param workId
     * @return
     * @throws Exception
     */
    public ApiResultResponse<List<WorkDTO>> workControllerFindWorkHistoryById(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String workId
    ) throws Exception {
        var requestBuilder = get("/v1/work/{workId}/history", workId)
                .contentType(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Search all the work
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @param anchorID      the anchor id
     * @param contextSize   the context size
     * @param limit         the limit
     * @param search        the search
     * @return the list of work
     * @throws Exception the exception
     */
    public ApiResultResponse<List<WorkDTO>> workControllerSearchAllWork(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> anchorID,
            Optional<Integer> contextSize,
            Optional<Integer> limit,
            Optional<String> search
    ) throws Exception {
        var requestBuilder = get("/v1/work")
                .contentType(MediaType.APPLICATION_JSON);
        anchorID.ifPresent(s -> requestBuilder.param("anchorId", s));
        contextSize.ifPresent(s -> requestBuilder.param("contextSize", s.toString()));
        limit.ifPresent(s -> requestBuilder.param("limit", s.toString()));
        search.ifPresent(s -> requestBuilder.param("search", s));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Find all field that are LOV
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @param domainTypeDTO the domain type dto
     * @return the list of all lov field dto
     */
    public ApiResultResponse<List<String>> lovControllerFindAllFieldThatAreLOV(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            LOVDomainTypeDTO domainTypeDTO,
            String domainId,
            String subtypeId
    ) throws Exception {
        var requestBuilder = get("/v1/lov/{domainType}/{domainId}/{subtypeId}", domainTypeDTO, domainId, subtypeId)
                .contentType(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Find values by domain and field name
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @param domainTypeDTO the domain type dto
     * @param fieldName     the field name
     * @return the list of activity dto
     */
    public ApiResultResponse<List<LOVElementDTO>> lovControllerFindValuesByDomainAndFieldName(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            LOVDomainTypeDTO domainTypeDTO,
            String domainId,
            String subtypeId,
            String fieldName
    ) throws Exception {
        var requestBuilder = get("/v1/lov/{domainType}/{domainId}/{subtypeId}/{fieldName}", domainTypeDTO, domainId, subtypeId, fieldName)
                .contentType(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Create a new log entry
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param workId        the work id
     * @param newEntry      the new entry
     * @param files         the files
     * @return the boolean
     * @throws Exception the exception
     */
    public ApiResultResponse<Boolean> createLogEntry(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String workId,
            NewLogEntry newEntry,
            MockMultipartFile... files) throws Exception {
        // create builder
        MockMultipartHttpServletRequestBuilder multiPartBuilder = multipart("/v1/log/work/{workId}", workId);

        // add entry
        if (newEntry.title() != null) {
            multiPartBuilder.param("title", newEntry.title());
        }
        if (newEntry.text() != null) {
            multiPartBuilder.param("text", newEntry.text());
        }
        if (newEntry.eventAt() != null) {
            multiPartBuilder.param("eventAt", newEntry.eventAt().toString());
        }

        // add file in case they are present
        for (MockMultipartFile a :
                files) {
            multiPartBuilder.file(a);
        }
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                multiPartBuilder
        );
    }

    /**
     * Create a new log entry
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @param domainId      the domain id
     * @param workId        the work id
     * @param newEntry      the new entry
     * @param files         the files
     * @return the boolean
     * @throws Exception the exception
     */
    public ApiResultResponse<Boolean> createLogEntry(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String domainId,
            String workId,
            NewLogEntry newEntry,
            MockMultipartFile... files) throws Exception {
        // create builder
        MockMultipartHttpServletRequestBuilder multiPartBuilder = multipart("/v1/domain/{domainId}/work/{workId}/log", domainId, workId);

        // add entry
        if (newEntry.title() != null) {
            multiPartBuilder.param("title", newEntry.title());
        }
        if (newEntry.text() != null) {
            multiPartBuilder.param("text", newEntry.text());
        }
        if (newEntry.eventAt() != null) {
            multiPartBuilder.param("eventAt", newEntry.eventAt().toString());
        }

        // add file in case they are present
        for (MockMultipartFile a :
                files) {
            multiPartBuilder.file(a);
        }
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                multiPartBuilder
        );
    }

    /**
     * Create a new log entry
     *
     * @param mockMvc          the mock mvc
     * @param resultMatcher    the result matcher
     * @param userInfo         the user info
     * @param newBucketSlotDTO the new bucket slot dto
     * @return the id of the new bucket slot
     * @throws Exception the exception
     */
    public ApiResultResponse<String> maintenanceControllerCreateNewBucket(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            NewBucketDTO newBucketSlotDTO
    ) throws Exception {
        var requestBuilder = post("/v1/maintenance/bucket")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newBucketSlotDTO));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Find a bucket by id
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @param id            the id of the bucket to find
     * @return the bucket dto
     * @throws Exception the exception
     */
    public ApiResultResponse<BucketSlotDTO> maintenanceControllerFindBucketById(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String id
    ) throws Exception {
        var requestBuilder = get("/v1/maintenance/bucket/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Find all buckets
     *
     * @param mockMvc       the mock mvc
     * @param resultMatcher the result matcher
     * @param userInfo      the user info
     * @param limit         the limit
     * @param contextSize   the context size
     * @param anchorId      the anchor id
     * @return the list of bucket dto
     * @throws Exception the exception
     */
    public ApiResultResponse<List<BucketSlotDTO>> maintenanceControllerFindAllBuckets(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<Integer> limit,
            Optional<Integer> contextSize,
            Optional<String> anchorId
    ) throws Exception {
        var requestBuilder = get("/v1/maintenance/bucket")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);
        limit.ifPresent(s -> requestBuilder.param("limit", s.toString()));
        contextSize.ifPresent(s -> requestBuilder.param("contextSize", s.toString()));
        anchorId.ifPresent(s -> requestBuilder.param("anchorId", s));
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    public <T> ApiResultResponse<T> executeHttpRequest(
            TypeReference<ApiResultResponse<T>> typeRef,
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            MockHttpServletRequestBuilder requestBuilder) throws Exception {
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));

        MvcResult result = mockMvc.perform(requestBuilder)
                .andReturn();
        if (result.getResolvedException() != null) {
            throw result.getResolvedException();
        }
        // check fi ti matches the expected result
        resultMatcher.match(result);
        return objectMapper.readValue(result.getResponse().getContentAsString(), typeRef);
    }

}
