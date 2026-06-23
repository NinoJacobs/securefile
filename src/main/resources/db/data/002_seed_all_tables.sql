INSERT INTO roles (name)
VALUES ('ADMIN'),
       ('CUSTOMER');

INSERT INTO users (username, email, password_hash, first_name, last_name, phone, role_id)
VALUES ('admin.user', 'admin@securefile.test', '$2a$10$YGfslg3v2LQnLWpsr0caOuUeHhiyhg2CV5MBg6NN5RE2zlDzeQ9wK', 'Admin', 'User',
        '+27110000000', (SELECT id FROM roles WHERE name = 'ADMIN'));
