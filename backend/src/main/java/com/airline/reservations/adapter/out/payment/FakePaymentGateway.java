package com.airline.reservations.adapter.out.payment;

import com.airline.reservations.application.port.PaymentGateway;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Pretend payment processor. The card 4000000000000002 is always declined, to exercise the failure path. */
@Component
public class FakePaymentGateway implements PaymentGateway {

    static final String DECLINED_CARD = "4000000000000002";

    @Override
    public boolean charge(String cardNumber, BigDecimal amount) {
        return !DECLINED_CARD.equals(cardNumber);
    }
}
