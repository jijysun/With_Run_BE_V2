package UMC_8th.With_Run.common.exception;


import UMC_8th.With_Run.common.apiResponse.StndResponse;
import UMC_8th.With_Run.common.apiResponse.dto.ErrorReasonDTO;
import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class ExceptionAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<Object> validation(ConstraintViolationException ex, WebRequest request) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(constraintViolation -> constraintViolation.getMessage())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Constraint violation exception 추출 도중 에러 발생"));

        return handleExceptionInternalConstraint(ex, ErrorCode.valueOf(errorMessage), HttpHeaders.EMPTY, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().stream()
                .forEach(fieldError -> {
                    String field = fieldError.getField();
                    String errorMessage = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
                    errors.merge(field, errorMessage, (existingErrorMessage, newErrorMessage) -> existingErrorMessage + ", " + newErrorMessage);
                });

        return handleExceptionInternalArgs(ex, HttpHeaders.EMPTY, ErrorCode.valueOf("_BAD_REQUEST"), request, errors);
    }

    @ExceptionHandler
    public ResponseEntity<Object> exception(Exception ex, WebRequest request) {
        ex.printStackTrace();
        return handleExceptionInternalFalse(ex, ErrorCode.INTERNAL_SERVER_ERROR, HttpHeaders.EMPTY, ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus(), request, ex.getMessage());
    }

    @ExceptionHandler(value = GeneralException.class)
    public ResponseEntity<Object> onThrowExcpetion(GeneralException generalEx, HttpServletRequest request) {
        ErrorReasonDTO reasonHttpStatus = generalEx.getReasonHttpStatus();

        log.error("Exception occurred! : {}", reasonHttpStatus.getMessage());

        return handleExceptionInternal(generalEx, reasonHttpStatus, null, request);
    }

    private ResponseEntity<Object> handleExceptionInternal(Exception ex, ErrorReasonDTO reason, HttpHeaders headers, HttpServletRequest request) {
        StndResponse<Object> body = StndResponse.onFailure(reason.getCode(), reason.getMessage(), null);
        WebRequest webRequest = new ServletWebRequest(request);
        return super.handleExceptionInternal(ex, body, headers, reason.getHttpStatus(), webRequest);
    }

    private ResponseEntity<Object> handleExceptionInternalConstraint(Exception ex, ErrorCode errorStatus, HttpHeaders headers, WebRequest request) {
        StndResponse<Object> body = StndResponse.onFailure(errorStatus.getCode(), errorStatus.getMessage(), null);
        return super.handleExceptionInternal(ex, body, headers, errorStatus.getHttpStatus(), request);
    }

    private ResponseEntity<Object> handleExceptionInternalArgs(Exception ex, HttpHeaders headers, ErrorCode errorCommonStatus, WebRequest request, Map<String, String> errorArgs) {
        StndResponse<Object> body = StndResponse.onFailure(errorCommonStatus.getCode(), errorCommonStatus.getMessage(), errorArgs);
        return super.handleExceptionInternal(ex, body, headers, errorCommonStatus.getHttpStatus(), request);
    }

    private ResponseEntity<Object> handleExceptionInternalFalse(Exception ex, ErrorCode errorCommonStatus, HttpHeaders headers, HttpStatus status, WebRequest request, String errorPoint) {
        StndResponse<Object> body = StndResponse.onFailure(errorCommonStatus.getCode(), errorCommonStatus.getMessage(), errorPoint);
        return super.handleExceptionInternal(ex, body, headers, status, request
        );
    }
}