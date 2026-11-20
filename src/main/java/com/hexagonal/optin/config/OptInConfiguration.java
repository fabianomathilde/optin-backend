package com.hexagonal.optin.config;

import com.hexagonal.optin.application.port.out.MerchantList;
import com.hexagonal.optin.application.port.out.OptInRepository;
import com.hexagonal.optin.application.service.OptInService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Spring wiring for the core. Application services are plain classes created here (CLAUDE.md). */
@Configuration
class OptInConfiguration {

    /** Business dates (start, validUntil, daily check) are in Brazilian time. */
    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }

    @Bean
    OptInService optInService(OptInRepository repository, MerchantList merchants, Clock clock) {
        return new OptInService(repository, merchants, clock);
    }
}
