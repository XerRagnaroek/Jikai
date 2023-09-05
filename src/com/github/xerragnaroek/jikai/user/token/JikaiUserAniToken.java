package com.github.xerragnaroek.jikai.user.token;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@JsonIgnoreProperties({"token_type", "expires_in"})
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
public class JikaiUserAniToken {
    private String refresh_token;
    private String access_token;
    private long issued_at;

    public String getAccessToken() {
        return access_token;
    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public void setIssuedAt(long issued) {
        issued_at = issued;
    }

    /**
     * @return epoch seconds of when this token was fetched
     */
    public long getIssuedAt() {
        return issued_at;
    }

    public long getSecondsUntilExpire() {
        return TimeUnit.DAYS.toSeconds(365) - (Instant.now().getEpochSecond() - issued_at);
    }

    public boolean isExpired() {
        return getSecondsUntilExpire() <= 0;
    }

}