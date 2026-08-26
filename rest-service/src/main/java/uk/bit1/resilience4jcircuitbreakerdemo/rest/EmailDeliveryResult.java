package uk.bit1.resilience4jcircuitbreakerdemo.rest;

record EmailDeliveryResult(String status, String detail) {

    static EmailDeliveryResult sent() {
        return new EmailDeliveryResult("SENT", "email-service accepted the request");
    }

    static EmailDeliveryResult deferred(String reason) {
        return new EmailDeliveryResult("EMAIL_DEFERRED", reason);
    }
}
