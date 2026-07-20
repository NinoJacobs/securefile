package com.capitec.securefile.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

class HttpExceptionHandlerTest {

    private final HttpExceptionHandler handler = new HttpExceptionHandler();

    @Test
    void handlesResponseStatusException() {
        ResponseStatusException exception = new ResponseStatusException(CONFLICT, "Duplicate statement");

        var response = handler.handleResponseStatusException(exception, request("POST", "/api/v1/customers/me/statements"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).isEqualTo("Duplicate statement");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/customers/me/statements");
    }

    @Test
    void handlesMethodArgumentNotValidExceptionWithFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new DummyBody(), "dummyBody");
        bindingResult.addError(new FieldError("dummyBody", "endDate", "endDate is required"));
        bindingResult.addError(new FieldError("dummyBody", "startDate", "startDate is required"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter(), bindingResult);

        var response = handler.handleMethodArgumentNotValidException(exception, request("POST", "/api/v1/customers/me/statements/generate"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("endDate is required; startDate is required");
        assertThat(response.getBody().getValidationErrors()).hasSize(2);
        assertThat(response.getBody().getValidationErrors().get(0).getField()).isEqualTo("endDate");
    }

    @Test
    void handlesMethodArgumentNotValidExceptionWithGlobalErrorsOnly() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new DummyBody(), "dummyBody");
        bindingResult.addError(new ObjectError("dummyBody", "custom date range is invalid"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter(), bindingResult);

        var response = handler.handleMethodArgumentNotValidException(exception, request("POST", "/api/v1/customers/me/statements/generate"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("custom date range is invalid");
        assertThat(response.getBody().getValidationErrors()).singleElement()
                .satisfies(error -> {
                    assertThat(error.getField()).isEqualTo("dummyBody");
                    assertThat(error.getMessage()).isEqualTo("custom date range is invalid");
                });
    }

    @Test
    void handlesConstraintViolationException() {
        ConstraintViolation<?> firstViolation = violation("request.startDate", "must not be null");
        ConstraintViolation<?> secondViolation = violation("request.endDate", "must not be null");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(firstViolation, secondViolation));

        var response = handler.handleConstraintViolationException(exception, request("GET", "/api/v1/customers/me/statements"));

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("request.endDate must not be null; request.startDate must not be null");
        assertThat(response.getBody().getValidationErrors()).hasSize(2);
    }

    @Test
    void handlesMethodArgumentTypeMismatchException() {
        MethodArgumentTypeMismatchException exception =
                new MethodArgumentTypeMismatchException("abc", Long.class, "statementId", null, new IllegalArgumentException("bad type"));

        var response = handler.handleMethodArgumentTypeMismatchException(exception, request("GET", "/api/v1/customers/me/statements/abc"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid value for statementId");
    }

    @Test
    void handlesAuthenticationException() {
        AuthenticationException exception = new BadCredentialsException("bad creds");

        var response = handler.handleAuthenticationException(exception, request("POST", "/api/v1/auth/login"));

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Authentication failed");
    }

    @Test
    void handlesAccessDeniedException() {
        var response = handler.handleAccessDeniedException(
                new org.springframework.security.access.AccessDeniedException("denied"),
                request("GET", "/api/v1/admin/customers"));

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
    }

    @Test
    void handlesUnexpectedException() {
        var response = handler.handleException(new IllegalStateException("boom"), request("GET", "/api/v1/customers/me/statements"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("Unexpected server error");
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        return request;
    }

    private MethodParameter methodParameter() throws NoSuchMethodException {
        Method method = DummyController.class.getDeclaredMethod("handle", DummyBody.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unchecked")
    private ConstraintViolation<?> violation(String pathValue, String message) {
        ConstraintViolation<Object> violation = (ConstraintViolation<Object>) Mockito.mock(ConstraintViolation.class);
        Path path = Mockito.mock(Path.class);
        Mockito.when(path.toString()).thenReturn(pathValue);
        Mockito.when(violation.getPropertyPath()).thenReturn(path);
        Mockito.when(violation.getMessage()).thenReturn(message);
        return violation;
    }

    private static final class DummyController {
        @SuppressWarnings("unused")
        void handle(DummyBody body) {
        }
    }

    private static final class DummyBody {
    }
}
