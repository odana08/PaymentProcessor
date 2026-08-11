package org.example.service;

public class InternationalFeeRuleCreator extends FeeRuleCreator {

    @Override
    protected FeeRule createFeeRule() {
        return new InternationalFeeRule();
    }
}
