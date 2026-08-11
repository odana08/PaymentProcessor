package org.example.service;

public class DomesticFeeRuleCreator extends FeeRuleCreator {

    @Override
    protected FeeRule createFeeRule() {
        return new DomesticFeeRule();
    }
}
