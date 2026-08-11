package org.example.service;

import org.example.model.Payment;

import java.math.BigDecimal;
import java.util.List;

public class FeeCalculator {

    private final List<FeeRuleCreator> feeRuleCreators;

    public FeeCalculator(List<FeeRule> feeRules) {
        this.feeRuleCreators = feeRules.stream()
                .map(RegisteredFeeRuleCreator::new)
                .map(creator -> (FeeRuleCreator) creator)
                .toList();
    }

    public FeeCalculator(FeeRuleCreator... feeRuleCreators) {
        this.feeRuleCreators = List.of(feeRuleCreators);
    }

    public BigDecimal calculateFee(Payment payment) {
        return feeRuleCreators.stream()
                .filter(creator -> creator.supportedType().getName().equals(payment.getType().getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported payment type: " + payment.getType().getName()))
                .calculateFee(payment);
    }

    public List<org.example.model.PaymentType> getAvailableTypes() {
        return feeRuleCreators.stream().map(FeeRuleCreator::supportedType).toList();
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
