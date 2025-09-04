CREATE DATABASE netflix_clone;
USE netflix_clone;

CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(100) UNIQUE NOT NULL,
  otp VARCHAR(6),
  otp_expiry DATETIME
);
