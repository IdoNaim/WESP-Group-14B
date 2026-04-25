package com.ticketpurchasingsystem.project.domain.systemAdmin;
import com.ticketpurchasingsystem.project.domain.Utils.IdGenerator;
public class AdminInfo {
    private final String id;
    private final String username;
    private final String email;

    public AdminInfo(String username, String email) {
        this.username = username;
        this.email = email;
        this.id = "admin-" + IdGenerator.getInstance().nextId();
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
    public String getId() {
        return id;
    }
}
