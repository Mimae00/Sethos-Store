package com.possystem.pos.web;

import com.possystem.pos.config.PosProperties;
import com.possystem.pos.dto.SettingsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only store configuration. The client calls this once on boot to learn the
 * currency symbol, tax default and receipt branding.
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final PosProperties properties;

    public SettingsController(PosProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public SettingsResponse get() {
        return SettingsResponse.from(properties);
    }
}
