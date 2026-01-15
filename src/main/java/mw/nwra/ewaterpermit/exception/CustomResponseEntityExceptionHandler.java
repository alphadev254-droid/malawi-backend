package mw.nwra.ewaterpermit.exception;

import java.util.Date;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolationException;
import mw.nwra.ewaterpermit.constant.CustomError;

@ControllerAdvice
@RestController
public class CustomResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(Exception.class)
	protected ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {

		ExceptionResponse exceptionResponse = new ExceptionResponse("500", CustomError.INTERNAL_SERVER_ERROR.toString(),
				request.getDescription(false), new Date());
		return new ResponseEntity<Object>(exceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(EntityNotFoundException.class)
	protected ResponseEntity<Object> handleUserNotFoundException(EntityNotFoundException ex, WebRequest request) {

		ExceptionResponse exceptionResponse = new ExceptionResponse("404", ex.getMessage(),
				request.getDescription(false), new Date());
		return new ResponseEntity<Object>(exceptionResponse, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(DataFormatException.class)
	protected ResponseEntity<Object> handleUnauthorizedException(DataFormatException ex, WebRequest request) {

		ExceptionResponse exceptionResponse = new ExceptionResponse("400", ex.getMessage(),
				request.getDescription(false), new Date());
		return new ResponseEntity<Object>(exceptionResponse, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(UnauthorizedException.class)
	protected ResponseEntity<Object> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {

		ExceptionResponse exceptionResponse = new ExceptionResponse("401", ex.getMessage(),
				request.getDescription(false), new Date());
		return new ResponseEntity<Object>(exceptionResponse, HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(ForbiddenException.class)
	protected ResponseEntity<Object> handleForbiddenException(ForbiddenException ex, WebRequest request) {

		ExceptionResponse exceptionResponse = new ExceptionResponse("403", ex.getMessage(),
				request.getDescription(false), new Date());
		return new ResponseEntity<Object>(exceptionResponse, HttpStatus.FORBIDDEN);
	}

	@ExceptionHandler({ DataIntegrityViolationException.class, ConstraintViolationException.class })
	protected ResponseEntity<Object> handleUnauthorizedException(Exception ex, WebRequest request) {

		ExceptionResponse exceptionResponse = new ExceptionResponse("400", CustomError.RECORD_ALREADY_EXISTS.toString(),
				request.getDescription(false), new Date());
		return new ResponseEntity<Object>(exceptionResponse, HttpStatus.BAD_REQUEST);
	}

	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		ExceptionResponse exceptionResponse = new ExceptionResponse("400", CustomError.FAILED_VALIDATION.toString(),
				ex.getBindingResult().getAllErrors().toString(), new Date());
		return new ResponseEntity<Object>(exceptionResponse, HttpStatus.BAD_REQUEST);
	}

}
