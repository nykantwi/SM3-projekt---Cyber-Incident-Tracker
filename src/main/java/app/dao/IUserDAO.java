package app.dao;

import app.entities.User;

public interface IUserDAO {

    User create(User user);

    User findByEmail(String email);


}
