package org.example.service;

import org.example.model.Payment;
import org.example.model.PaymentType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FeeCalculator {

    private final List<FeeRuleCreator> feeRuleCreators;

    public FeeCalculator(List<FeeRule> feeRules) {
        List<FeeRuleCreator> creators = new ArrayList<>();
        for (FeeRule feeRule : feeRules) {
            creators.add(new RegisteredFeeRuleCreator(feeRule));
        }
        this.feeRuleCreators = List.copyOf(creators);
    }

    public FeeCalculator(FeeRuleCreator... feeRuleCreators) {
        this.feeRuleCreators = List.of(feeRuleCreators);
    }

    public BigDecimal calculateFee(Payment payment) {
        for (FeeRuleCreator creator : feeRuleCreators) {
            if (creator.supportedType().getName().equals(payment.getType().getName())) {
                return creator.calculateFee(payment);
            }
        }

        throw new IllegalArgumentException("Unsupported payment type: " + payment.getType().getName());
    }

    public List<PaymentType> getAvailableTypes() {
        List<PaymentType> paymentTypes = new ArrayList<>();
        for (FeeRuleCreator creator : feeRuleCreators) {
            paymentTypes.add(creator.supportedType());
        }
        return List.copyOf(paymentTypes);
    }

    private static final class RegisteredFeeRuleCreator extends FeeRuleCreator {

        private final FeeRule feeRule;

        private RegisteredFeeRuleCreator(FeeRule feeRule) {
            this.feeRule = feeRule;
        }

        @Override
        protected FeeRule createFeeRule() {
            return feeRule;
        }
    }
}
