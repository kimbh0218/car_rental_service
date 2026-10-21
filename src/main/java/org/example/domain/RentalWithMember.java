package org.example.domain;

public class RentalWithMember {

    private final Rental rental;
    private final String memberName;

    public RentalWithMember(Rental rental, String memberName) {
        this.rental = rental;
        this.memberName = memberName;
    }

    public Rental getRental() {
        return rental;
    }

    public String getMemberName() {
        return memberName;
    }

    @Override
    public String toString() {
        return "RentalWithMember{" +
                "rental=" + rental +
                ", memberName='" + memberName + '\'' +
                '}';
    }
}