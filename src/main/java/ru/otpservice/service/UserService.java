package ru.otpservice.service;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otpservice.dao.UserDao;
import ru.otpservice.model.Role;
import ru.otpservice.model.User;

import java.util.List;
import java.util.Optional;

/** Сервис управления пользователями.
 * Содержит логику регистрации, аутентификации и админских операций.
 * При этом SQL-запросы делегируются UserDao.
 */
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Регистрирует нового пользователя. При этом:
     * - логин и пароль не должны быть пустыми;
     * - админ может быть только один;
     * - пользователь с таким же логином не должен уже существовать;
     * - пароль перед сохранением хешируется через BCrypt.
     * - пробрасываем исключения если логин/пароль пустые, а логин в системе уже есть
     */
    public User register(String username, String password, Role role) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }
        if (role == Role.ADMIN && userDao.adminExists()) {
            throw new IllegalStateException("Admin already exists");
        }
        if (userDao.findByUsername(username).isPresent()) {
            throw new IllegalStateException("User already exists");
        }
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User(null, username, hash, role);
        userDao.save(user);
        log.info("Registered user '{}' with role {}", username, role);
        return user;
    }

    //Проверяет логин/пароль. Возвращает пользователя, если совпадает, иначе пустой Optional
    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = userDao.findByUsername(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    // Возвращает список пользователей кроме админов. Используется в админском API
    public List<User> listNonAdmins() {
        return userDao.findAllNonAdmins();
    }

    // Поиск пользователя по id
    public Optional<User> findById(Long id) {
        return userDao.findById(id);
    }

    // Удаление пользователя (без админа — это блокируется в DAO)
    public boolean delete(Long id) {
        return userDao.delete(id);
    }
}
