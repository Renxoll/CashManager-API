package pe.smartcash.cash.groups.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.smartcash.cash.groups.domain.exception.DuplicateMembershipException;
import pe.smartcash.cash.groups.domain.exception.GroupNotFoundException;
import pe.smartcash.cash.groups.domain.exception.InvitedUserNotRegisteredException;
import pe.smartcash.cash.groups.domain.exception.MembershipNotFoundException;
import pe.smartcash.cash.groups.domain.exception.NotAGroupMemberException;
import pe.smartcash.cash.shared.interfaces.rest.ApiError;

/** Mapeo de excepciones propias de este bounded context; el resto lo cubre {@code
 * GlobalExceptionHandler} (ver la nota ahí). */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class GroupsExceptionHandler {

  @ExceptionHandler(GroupNotFoundException.class)
  ResponseEntity<ApiError> handleGroupNotFound(GroupNotFoundException ex, HttpServletRequest request) {
    return notFound(ex, request);
  }

  @ExceptionHandler(MembershipNotFoundException.class)
  ResponseEntity<ApiError> handleMembershipNotFound(MembershipNotFoundException ex, HttpServletRequest request) {
    return notFound(ex, request);
  }

  @ExceptionHandler(InvitedUserNotRegisteredException.class)
  ResponseEntity<ApiError> handleInvitedUserNotRegistered(InvitedUserNotRegisteredException ex, HttpServletRequest request) {
    return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "Bad Request", ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(NotAGroupMemberException.class)
  ResponseEntity<ApiError> handleNotAGroupMember(NotAGroupMemberException ex, HttpServletRequest request) {
    return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "Bad Request", ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(DuplicateMembershipException.class)
  ResponseEntity<ApiError> handleDuplicateMembership(DuplicateMembershipException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(Instant.now(), 409, "Conflict", ex.getMessage(), request.getRequestURI()));
  }

  private ResponseEntity<ApiError> notFound(RuntimeException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError(Instant.now(), 404, "Not Found", ex.getMessage(), request.getRequestURI()));
  }
}
