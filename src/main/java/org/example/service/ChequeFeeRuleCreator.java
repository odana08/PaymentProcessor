package org.example.service;

public class ChequeFeeRuleCreator extends FeeRuleCreator {

    @Override
    protected FeeRule createFeeRule() {
        return new ChequeFeeRule();
    }
}
