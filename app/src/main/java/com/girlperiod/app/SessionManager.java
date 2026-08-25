package com.girlperiod.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.girlperiod.app.data.User;

/**
 * Manages user session state using SharedPreferences.
 * Persists the logged-in user's id and username across app launches.
 */
public class SessionManager {

    private static final String PREF_NAME = "GirlPeriodSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
     * Saves the given user as the active session.
     */
    public void saveUser(User user) {
        editor.putInt(KEY_USER_ID, (int) user.getId());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * Returns the currently logged-in user, or null if no active session.
     */
    public User getCurrentUser() {
        if (!isLoggedIn()) {
            return null;
        }
        int id = pref.getInt(KEY_USER_ID, -1);
        String username = pref.getString(KEY_USERNAME, "");
        return new User(id, username);
    }

    /**
     * Clears the current session.
     */
    public void logout() {
        editor.clear();
        editor.apply();
    }

    /**
     * Returns true if a user is currently logged in.
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }
}
