package com.g8e.db.models;

public class DBAccount {
    private int account_id;
    private String username;
    private String password;
    private String login_token;
    private String registration_ip;
    private String registration_date;

    public DBAccount(int id, String username, String password, String login_token, String registration_ip,
            String registration_date) {
        this.account_id = id;
        this.username = username;
        this.password = password;
        this.login_token = login_token;
        this.registration_ip = registration_ip;
        this.registration_date = registration_date;
    }

    public int getAccountId() {
        return account_id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getLoginToken() {
        return login_token;
    }

    public String getRegistrationIp() {
        return registration_ip;
    }

    public String getRegistrationDate() {
        return registration_date;
    }

}
