package com.example.footprint.data.model;

public final class Policy {
    public String type, version, content, requiredAt;
    public boolean consentRequired, withdrawable;
    public static final class Consent {
        public final String type, version;
        public final boolean accepted;
        public Consent(Policy policy, boolean accepted) { this.type = policy.type; this.version = policy.version; this.accepted = accepted; }
    }
}
