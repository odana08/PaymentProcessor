package org.example.service;

import org.example.model.Currency;
import org.example.model.NotificationChannel;
import org.example.model.Payment;
import org.example.model.PaymentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FeeRuleCreatorTest {

    @Test
    void domesticCreatorUsesDomesticRuleForItsCalculation() {
        TestableDomesticCreator creator = new TestableDomesticCreator();
        Payment payment = payment("DOMESTIC_FEE", "1000");

        assertInstanceOf(DomesticFeeRule.class, creator.rule());
        assertEquals(new BigDecimal("150"), creator.calculateFee(payment));
    }

    @Test
    void internationalCreatorUsesInternationalRuleForItsCalculation() {
        FeeRuleCreator creator = new InternationalFeeRuleCreator();

        assertEquals(new BigDecimal("20.00"),
                creator.calculateFee(payment("INTERNATIONAL_FEE", "1000")));
    }

    @Test
    void chequeCreatorUsesChequeRuleForItsCalculation() {
        FeeRuleCreator creator = new ChequeFeeRuleCreator();

        assertEquals(new BigDecimal("200"),
                creator.calculateFee(payment("CHEQUE_FEE", "1000")));
    }

    private Payment payment(String type, String amount) {
        return new Payment(new PaymentType(type), new BigDecimal(amount), Currency.JOD,
                new NotificationChannel("EMAIL"));
    }

    private static class TestableDomesticCreator extends DomesticFeeRuleCreator {
        private FeeRule rule() {
            return createFeeRule();
        }
    }
}
