package org.gik.cloud.storage.auth;

import java.util.Arrays;
import java.util.List;

public class AuthService  {
    private static class Entry {
        String login;
        String password;

        public Entry(String login, String password) {
            this.login = login;
            this.password = password;
        }
    }

    private final List<Entry> entries = Arrays.asList(
            new Entry("user1", ""),
            new Entry("user2", "pass2"),
            new Entry("1", ""));


    public boolean isLegitUser(String login, String pass) {
        for (Entry entry : entries) {
            if(entry.login.equals(login) && entry.password.equals(pass)){
                return true;
            }
        }
        return false;
    }

}
