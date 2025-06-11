package constants;

public enum VerificationStatus {

    unapproved("01"),
    approved("00"),
    canceled("02");

    final String verificationStatus;
    VerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
    public String getVerificationStatus() {
        return this.verificationStatus;
    }
}
