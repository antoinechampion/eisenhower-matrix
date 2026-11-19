package com.ach.eisenhower;

import com.ach.eisenhower.entities.NoteEntity;
import com.ach.eisenhower.entities.NoteFlag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NoteFlagTest {

    @Test
    public void testIsValidWithNull() {
        assertTrue(NoteFlag.isValid(null));
    }

    @Test
    public void testIsValidWithValidEnumValues() {
        assertTrue(NoteFlag.isValid("QUICK_WIN"));
        assertTrue(NoteFlag.isValid("LONG_TERM"));
    }

    @Test
    public void testIsValidWithInvalidValues() {
        assertFalse(NoteFlag.isValid("INVALID"));
        assertFalse(NoteFlag.isValid(""));
        assertFalse(NoteFlag.isValid("quick_win"));
        assertFalse(NoteFlag.isValid("long_term"));
        assertFalse(NoteFlag.isValid(" QUICK_WIN"));
        assertFalse(NoteFlag.isValid("QUICK_WIN "));
    }

    @Test
    public void testNoteEntitySetFlagValid() {
        NoteEntity note = new NoteEntity();
        note.setFlag("QUICK_WIN");
        assertEquals("QUICK_WIN", note.getFlag());

        note.setFlag("LONG_TERM");
        assertEquals("LONG_TERM", note.getFlag());

        note.setFlag(null);
        assertNull(note.getFlag());
    }

    @Test
    public void testNoteEntitySetFlagInvalid() {
        NoteEntity note = new NoteEntity();
        assertThrows(IllegalArgumentException.class, () -> note.setFlag("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> note.setFlag("quick_win"));
    }
}
