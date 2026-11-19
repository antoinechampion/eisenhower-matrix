package com.ach.eisenhower.entities;

public enum NoteFlag {
    QUICK_WIN,
    LONG_TERM;

    public static boolean isValid(String flag) {
        if (flag == null) {
            return true;
        }
        for (NoteFlag value : values()) {
            if (value.name().equals(flag)) {
                return true;
            }
        }
        return false;
    }
}
