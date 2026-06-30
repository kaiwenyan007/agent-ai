package com.agent.cli;

import org.springframework.stereotype.Component;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * 控制台输入输出（兼容无 Console 的环境）。
 */
@Component
public class ConsoleIO {

    private final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

    public void println() {
        System.out.println();
    }

    public void println(String line) {
        System.out.println(line);
    }

    public void print(String line) {
        System.out.print(line);
    }

    public String readLine(String prompt) {
        print(prompt);
        return scanner.nextLine().trim();
    }

    public String readLineOrDefault(String prompt, String defaultValue) {
        String line = readLine(prompt);
        return line.isEmpty() ? defaultValue : line;
    }

    public String readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] chars = console.readPassword(prompt);
            return chars == null ? "" : new String(chars).trim();
        }
        return readLine(prompt);
    }

    public int readChoice(String prompt, int min, int max) {
        while (true) {
            String line = readLine(prompt);
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // retry
            }
            println("请输入 " + min + " ~ " + max + " 之间的数字。");
        }
    }
}
