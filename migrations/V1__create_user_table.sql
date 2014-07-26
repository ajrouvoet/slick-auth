create table `users` (
  `uid` INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(254) NOT NULL,
  `email` VARCHAR(254) NOT NULL,
  `crypt` VARCHAR(254) NOT NULL,
  `created_at` TIMESTAMP NOT NULL
);

create unique index `unique_email` on `users` (`email`);
create unique index `unique_username` on `users` (`username`);
