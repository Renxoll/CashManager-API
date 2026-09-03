package pe.smartcash.cash.workspaces.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.smartcash.cash.shared.interfaces.rest.ApiError;
import pe.smartcash.cash.workspaces.domain.exception.DefaultWorkspaceProtectedException;
import pe.smartcash.cash.workspaces.domain.exception.DuplicateWorkspaceCategoryException;
import pe.smartcash.cash.workspaces.domain.exception.LastActiveWorkspaceCategoryException;
import pe.smartcash.cash.workspaces.domain.exception.WorkspaceCategoryNotFoundException;
import pe.smartcash.cash.workspaces.domain.exception.WorkspaceNotFoundException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class WorkspaceExceptionHandler {

  @ExceptionHandler({WorkspaceNotFoundException.class, WorkspaceCategoryNotFoundException.class})
  ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError(Instant.now(), 404, "Not Found", ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler({
    DefaultWorkspaceProtectedException.class,
    DuplicateWorkspaceCategoryException.class,
    LastActiveWorkspaceCategoryException.class
  })
  ResponseEntity<ApiError> handleConflict(RuntimeException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError(Instant.now(), 409, "Conflict", ex.getMessage(), request.getRequestURI()));
  }
}
