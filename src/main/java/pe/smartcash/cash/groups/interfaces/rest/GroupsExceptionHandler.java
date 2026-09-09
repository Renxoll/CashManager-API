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
import pe.smartcash.cash.groups.domain.exception.ExpenseEditForbiddenException;
import pe.smartcash.cash.groups.domain.exception.ExpenseNotFoundException;
import pe.smartcash.cash.groups.domain.exception.GroupDeletionAlreadyRequestedException;
import pe.smartcash.cash.groups.domain.exception.GroupDeletionRequestNotFoundException;
import pe.smartcash.cash.groups.domain.exception.GroupHasOutstandingBalancesException;
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

  @ExceptionHandler(ExpenseNotFoundException.class)
  ResponseEntity<ApiError> handleExpenseNotFound(ExpenseNotFoundException ex, HttpServletRequest request) {
    return notFound(ex, request);
  }

  @ExceptionHandler(GroupDeletionRequestNotFoundException.class)
  ResponseEntity<ApiError> handleGroupDeletionRequestNotFound(
      GroupDeletionRequestNotFoundException ex, HttpServletRequest request) {
    return notFound(ex, request);
  }

  @ExceptionHandler(ExpenseEditForbiddenException.class)
  ResponseEntity<ApiError> handleExpenseEditForbidden(ExpenseEditForbiddenException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ApiError(Instant.now(), 403, "Forbidden", ex.getMessage(), request.getRequestURI()));
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
    return conflict(ex, request);
  }

  @ExceptionHandler(GroupDeletionAlreadyRequestedException.class)
  ResponseEntity<ApiError> handleGroupDeletionAlreadyRequested(
      GroupDeletionAlreadyRequestedException ex, HttpServletRequest request) {
    return conflict(ex, request);
  }

  @ExceptionHandler(GroupHasOutstandingBalancesException.class)
  ResponseEntity<ApiError> handleGroupHasOutstandingBalances(
      GroupHasOutstandingBalancesException ex, HttpServletRequest request) {
    return conflict(ex, request);
  }

  private ResponseEntity<ApiError> notFound(RuntimeException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError(Instant.now(), 404, "Not Found", ex.getMessage(), request.getRequestURI()));
  }

  private ResponseEntity<ApiError> conflict(RuntimeException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError(Instant.now(), 409, "Conflict", ex.getMessage(), request.getRequestURI()));
  }
}
