package com.agent.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class CyberStartupBanner implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String app = env.getProperty("spring.application.name", "agent-ai");
        String profile = env.getActiveProfiles().length > 0
                ? String.join(", ", env.getActiveProfiles())
                : "default";
        String port = env.getProperty("local.server.port",
                env.getProperty("server.port", "8080"));
        String healthUrl = "http://localhost:" + port + "/api/health";

        System.out.println();
        System.out.println("+--------------------------------------------------------------+");
        System.out.println("|  AGENT AI CORE          [ NEURAL LINK :: ONLINE ]            |");
        System.out.println("+--------------------------------------------------------------+");
        System.out.printf("|  NODE ID .............. %-34s|%n", app);
        System.out.printf("|  PROFILE .............. %-34s|%n", profile);
        System.out.printf("|  UPLINK PORT .......... %-34s|%n", port);
        System.out.println("|  HEALTH PROBE ......... /api/health                          |");
        System.out.println("+--------------------------------------------------------------+");
        System.out.printf("|  >> INIT COMPLETE - %s%n", healthUrl);
        System.out.println("+--------------------------------------------------------------+");
        System.out.println();
    }
}
