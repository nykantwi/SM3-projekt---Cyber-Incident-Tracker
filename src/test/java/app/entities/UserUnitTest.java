package app.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserUnitTest {

    @Test
    void correctPasswordReturnsTrue() {
        User user = new User("Testbruger", "test@123.com", "1234");

        assertTrue(user.verifyPassword("1234"));
    }

    @Test
    void wrongPasswordReturnsFalse() {
        User user = new User("Testbruger", "test@123.com", "1234");

        assertFalse(user.verifyPassword("ForkertPassword"));
    }

    @Test
    void passwordIsStoredAsHash() {
        User user = new User("Testbruger", "test@123.com", "1234");

        // Det gemte hash skal være forskelligt fra passwordet.
        assertNotNull(user.getPasswordHash());
        assertNotEquals("1234", user.getPasswordHash());
    }

    @Test
    void userWithoutPasswordCannotLogIn() {
        User user = new User();

        assertFalse(user.verifyPassword("1234"));
    }
}