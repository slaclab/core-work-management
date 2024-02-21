package edu.stanford.slac.core_work_management.controller;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.auth.JWTHelper;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.core_work_management.api.v1.dto.LocationDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLocationDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewShopGroupDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.ShopGroupDTO;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
@Service()
public class TestControllerHelperService {
    private final JWTHelper jwtHelper;
    private final AppProperties appProperties;

    public TestControllerHelperService(JWTHelper jwtHelper, AppProperties appProperties) {
        this.jwtHelper = jwtHelper;
        this.appProperties = appProperties;

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
            NewShopGroupDTO newShopGroupDTO
    ) throws Exception {
        var requestBuilder = post("/v1/shop-group")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(newShopGroupDTO));
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
            Optional<String> userInfo
    ) throws Exception {
        var requestBuilder = get("/v1/shop-group")
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
            String id
    ) throws Exception {
        var requestBuilder = get("/v1/shop-group/{id}", id)
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
    public ApiResultResponse<String> locationControllerCreateNew(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            NewLocationDTO newLocationDTO
    ) throws Exception {
        var requestBuilder = post("/v1/location")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(newLocationDTO));
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
            String locationId
    ) throws Exception {
        var requestBuilder = get("/v1/location/{locationId}", locationId)
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
            Optional<String> filter
    ) throws Exception {
        var requestBuilder = get("/v1/location")
                .accept(MediaType.APPLICATION_JSON);
        filter.ifPresent(s -> requestBuilder.param("filter", s));
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
                .andExpect(resultMatcher)
                .andReturn();
        //Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (result.getResolvedException() != null) {
            throw result.getResolvedException();
        }
        return new ObjectMapper().readValue(result.getResponse().getContentAsString(), typeRef);
    }

}
