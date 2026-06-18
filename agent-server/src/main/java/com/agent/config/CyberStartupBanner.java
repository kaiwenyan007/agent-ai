package com.agent.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 应用就绪后打印赛博风格启动横幅。
 */
@Component
public class CyberStartupBanner implements ApplicationListener<ApplicationReadyEvent> {

    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[96m";
    private static final String MAGENTA = "\u001B[95m";
    private static final String GREEN = "\u001B[92m";
    private static final String YELLOW = "\u001B[93m";
    private static final String DIM = "\u001B[2m";

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

        String banner = """

                %s╔══════════════════════════════════════════════════════════════╗%s
                %s║%s  ░▒▓ %s█ %s AGENT AI CORE %s █%s ▓▒░     %s[ NEURAL LINK :: ONLINE ]%s  %s║%s
                %s╠══════════════════════════════════════════════════════════════╣%s
                %s║%s  ◈ SYSTEM STATUS .............. %sOPERATIONAL%s                  %s║%s
                %s║%s  ◈ NODE ID ..................... %s%-28s%s  %s║%s
                %s║%s  ◈ PROFILE ..................... %s%-28s%s  %s║%s
                %s║%s  ◈ UPLINK PORT ................. %s%-28s%s  %s║%s
                %s║%s  ◈ HEALTH PROBE ................ %s/api/health%s                 %s║%s
                %s╠══════════════════════════════════════════════════════════════╣%s
                %s║%s  >> %sINIT SEQUENCE COMPLETE — WELCOME TO THE GRID, OPERATOR%s   %s║%s
                %s║%s  >> %s%s%s%s   %s║%s
                %s╚══════════════════════════════════════════════════════════════╝%s
                """.formatted(
                CYAN, RESET,
                CYAN, DIM, MAGENTA, CYAN, MAGENTA, CYAN, GREEN, RESET, CYAN, RESET,
                CYAN, RESET,
                CYAN, DIM, GREEN, RESET, CYAN, RESET,
                CYAN, DIM, YELLOW, app, RESET, CYAN, RESET,
                CYAN, DIM, YELLOW, profile, RESET, CYAN, RESET,
                CYAN, DIM, YELLOW, port, RESET, CYAN, RESET,
                CYAN, DIM, GREEN, RESET, CYAN, RESET,
                CYAN, RESET,
                CYAN, DIM, MAGENTA, RESET, CYAN, RESET,
                CYAN, DIM, GREEN, healthUrl, DIM, RESET, CYAN, RESET,
                CYAN, RESET
        );

        System.out.println(banner);
    }
}
