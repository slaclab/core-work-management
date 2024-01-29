package edu.stanford.slac.core_work_management.controller;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.auth.JWTHelper;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.core_work_management.api.v1.dto.LocationDTO;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLocationDTO;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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
