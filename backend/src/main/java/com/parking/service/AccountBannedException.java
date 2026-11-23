package com.parking.service;

import com.parking.dto.BanDtos.BanInfo;
import lombok.Getter;

/** Login of a banned user: answered with 403 and the ban details, so the app can show them. */
@Getter
public class AccountBannedException extends RuntimeException {

    private final BanInfo banInfo;

    public AccountBannedException(BanInfo banInfo) {
        super("Your account is banned");
        this.banInfo = banInfo;
    }
}
