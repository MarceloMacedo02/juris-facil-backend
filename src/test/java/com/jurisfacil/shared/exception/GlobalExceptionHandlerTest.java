package com.jurisfacil.shared.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var environment = new MockEnvironment().withProperty("spring.profiles.active", "dev");
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ErrorProbeController())
                .setControllerAdvice(new GlobalExceptionHandler(environment))
                .setValidator(validator)
                .build();
    }

    @Test
    void mapsBeanValidationErrorsToProblemDetails() throws Exception {
        mockMvc.perform(post("/error-probe/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.invalidParams[0].name").value("name"))
                .andExpect(jsonPath("$.invalidParams[0].reason").value("must not be blank"));
    }

    @Test
    void mapsBusinessExceptionStatusFromCanonicalCode() throws Exception {
        mockMvc.perform(get("/error-probe/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DUPLICATE_PROCESS_IN_TENANT"))
                .andExpect(jsonPath("$.detail").value("The process already exists."));
    }

    @Test
    void mapsUnexpectedExceptionToGenericInternalProblemAndCorrelationDetailInDev() throws Exception {
        mockMvc.perform(get("/error-probe/unexpected").header("X-Correlation-Id", "corr-123"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail", not(containsString("boom"))))
                .andExpect(jsonPath("$.detail", containsString("corr-123")))
                .andExpect(jsonPath("$.instance").value("/error-probe/unexpected"));
    }

    @RestController
    static class ErrorProbeController {

        @PostMapping("/error-probe/validation")
        String validation(@Valid @RequestBody EchoRequest request) {
            return request.name();
        }

        @GetMapping("/error-probe/business")
        String business() {
            throw new DuplicateProcessException();
        }

        @GetMapping("/error-probe/unexpected")
        String unexpected() {
            throw new RuntimeException("boom");
        }
    }

    record EchoRequest(@NotBlank String name) {
    }

    static class DuplicateProcessException extends AbstractBusinessException {

        DuplicateProcessException() {
            super("DUPLICATE_PROCESS_IN_TENANT", "The process already exists.");
        }
    }
}
