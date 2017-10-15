package com.example.model;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class Agreement {

    private String name;
    public String getName() { return name; }

    private String address;
    public String getAddress() { return address; }

    private String eligibleCollateral;
    public String getEligibleCollateral() { return eligibleCollateral; }

    private String interestCashCollateral;
    public String getInterestCashCollateral() { return interestCashCollateral; }

    private String threshold;
    public String getThreshold() { return threshold; }

    private String mta;
    public String getMta() { return mta; }

    private String initialMarginCollateral;
    public String getInitialMarginCollateral() { return initialMarginCollateral; }

    private String variationMarginCollateral;
    public String getVariationMarginCollateral() { return variationMarginCollateral; }

    private String comments;
    public String getComments() { return comments; }

    public Agreement(
            String _name,
            String _address,
            String _interestCashCollateral,
            String _eligibleCollateral,
            String _threshold,
            String _mta,
            String _initialMarginCollateral,
            String _variationMarginCollateral,
            String _comments) {
        this.name = _name;
        this.address = _address;
        this.interestCashCollateral = _interestCashCollateral;
        this.eligibleCollateral = _eligibleCollateral;
        this.threshold = _threshold;
        this.mta = _mta;
        this.initialMarginCollateral = _initialMarginCollateral;
        this.variationMarginCollateral = _variationMarginCollateral;
        this.comments = _comments;
    }

    // Dummy constructor used by the create-iou API endpoint.
    public Agreement() {}

    @Override public String toString() {
        return String.format("Agreement(" +
                        "name=%d, address=%d, " +
                        "interestCashCollateral=%d, eligibleCollateral=%d, " +
                        "threshold=%d, mta=%d, " +
                        "initialMarginCollateral=%d, variationMarginCollateral=%d, comments=%d)",
                name,address,interestCashCollateral,
                eligibleCollateral,threshold,
                mta,initialMarginCollateral,
                variationMarginCollateral,comments);
    }
}
