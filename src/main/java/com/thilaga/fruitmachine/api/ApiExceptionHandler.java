package com.thilaga.fruitmachine.api;

import com.thilaga.fruitmachine.domain.InvalidMachineConfigException;
import com.thilaga.fruitmachine.domain.MachineBusyException;
import com.thilaga.fruitmachine.domain.MachineNeedsRefillException;
import com.thilaga.fruitmachine.domain.MachineNotFoundException;
import com.thilaga.fruitmachine.domain.MachineStatus;
import com.thilaga.fruitmachine.domain.NoFreePlayAvailableException;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps errors to RFC 7807 problem details with the design 5.4 types (NFR-12). Any exception not
 * listed here, including Spring's own, becomes 500 without a stack trace (accepted decision).
 */
@RestControllerAdvice
class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(InvalidMachineConfigException.class)
  ProblemDetail invalidConfig(InvalidMachineConfigException e) {
    return validation(e.errors().stream().map(m -> new Fault(null, m)).toList());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail beanValidation(MethodArgumentNotValidException e) {
    return validation(
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> new Fault(f.getField(), f.getDefaultMessage()))
            .toList());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail unreadable(HttpMessageNotReadableException e) {
    return validation(List.of(new Fault(null, "request body is unreadable")));
  }

  @ExceptionHandler(MachineNotFoundException.class)
  ProblemDetail notFound(MachineNotFoundException e) {
    return problem(HttpStatus.NOT_FOUND, "machine-not-found", "Machine not found", e.getMessage());
  }

  @ExceptionHandler(MachineNeedsRefillException.class)
  ProblemDetail needsRefill(MachineNeedsRefillException e) {
    ProblemDetail p =
        problem(
            HttpStatus.CONFLICT,
            "machine-needs-refill",
            "Machine needs refill",
            "The float is below the reserve; an operator must refill the machine.");
    p.setProperty("floatPence", e.floatPence());
    p.setProperty("reservePence", e.reservePence());
    p.setProperty("machineStatus", MachineStatus.NEEDS_REFILL);
    return p;
  }

  @ExceptionHandler(NoFreePlayAvailableException.class)
  ProblemDetail noFreePlays(NoFreePlayAvailableException e) {
    return problem(
        HttpStatus.CONFLICT, "no-free-plays", "No free plays", "The free-play balance is 0.");
  }

  @ExceptionHandler(MachineBusyException.class)
  ResponseEntity<ProblemDetail> busy(MachineBusyException e) {
    ProblemDetail p =
        problem(
            HttpStatus.SERVICE_UNAVAILABLE,
            "machine-busy",
            "Machine busy",
            "The machine is busy; retry later.");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .header(HttpHeaders.RETRY_AFTER, "1")
        .body(p);
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail internal(Exception e) {
    log.error("unhandled error", e);
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "internal-error",
        "Internal error",
        "An unexpected error occurred.");
  }

  private static ProblemDetail validation(List<Fault> faults) {
    ProblemDetail p =
        problem(
            HttpStatus.BAD_REQUEST,
            "validation-error",
            "Validation error",
            "One or more fields are invalid.");
    p.setProperty("errors", faults);
    return p;
  }

  private static ProblemDetail problem(
      HttpStatus status, String type, String title, String detail) {
    ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
    p.setType(URI.create(type));
    p.setTitle(title);
    return p;
  }

  /** One fault in a 400 body. {@code field} is null when the fault is not tied to one field. */
  record Fault(String field, String message) {}
}
