package ru.mirea.emulator;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Имя пользователя и имя хоста реальной ОС для заголовка и приглашения. */
public record HostIdentity(String user, String host) {
    /** Прочитать сведения об ОС, не обращаясь к виртуальной файловой системе. */
    public static HostIdentity current() {
        String user = System.getProperty("user.name", "user");
        try {
            return new HostIdentity(user, InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException exception) {
            String host = System.getenv().getOrDefault("HOSTNAME", "localhost");
            return new HostIdentity(user, host);
        }
    }

    /** Заголовок окна по формату задания. */
    public String title() {
        return "Эмулятор - [" + user + "@" + host + "]";
    }
}
