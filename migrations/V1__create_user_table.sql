create table `users` (
  `uid` INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(254) NOT NULL,
  `email` VARCHAR(254) NOT NULL,
  `password` VARCHAR(254) NOT NULL,
  `salt` VARCHAR(254) NOT NULL,
  `created_at` TIMESTAMP NOT NULL
);

create unique index `unique_email` on `users` (`email`);
