package com.airline.reservations.adapter.in.web;

import com.airline.reservations.application.exception.FlightNotBookableException;
import com.airline.reservations.application.exception.HoldNotValidException;
import com.airline.reservations.application.exception.NotFoundException;
import com.airline.reservations.application.exception.PaymentDeclinedException;
import com.airline.reservations.application.exception.SeatUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Single place that maps application exceptions to HTTP errors, using RFC 9457 ProblemDetail.
 * Extending ResponseEntityExceptionHandler also gives us a proper 400 for bean-validation failures.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({SeatUnavailableException.class, HoldNotValidException.class,
            FlightNotBookableException.class})
    ProblemDetail conflict(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(PaymentDeclinedException.class)
    ProblemDetail paymentDeclined(PaymentDeclinedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, e.getMessage());
    }
}
