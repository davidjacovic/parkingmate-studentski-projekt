CREATE DATABASE IF NOT EXISTS parking_system;
USE parking_system;

-- USER
CREATE TABLE IF NOT EXISTS USER (
    id_USER INT PRIMARY KEY,
    first_name VARCHAR(15),
    atribut VARCHAR(15),
    email VARCHAR(35),
    password_hash VARCHAR(45),
    phone_number VARCHAR(15),
    created DATETIME,
    modified DATETIME,
    user_type ENUM('admin', 'user'),
    hidden TINYINT
);

-- VEHICLE
CREATE TABLE IF NOT EXISTS VEHICLE (
    id_VEHICLE INT PRIMARY KEY,
    model VARCHAR(25),
    registration_number VARCHAR(25),
    vehicle_type VARCHAR(45),
    created DATETIME,
    modified DATETIME,
    TK_USER INT,
    FOREIGN KEY (TK_USER) REFERENCES USER(id_USER)
);

-- TARIFF
CREATE TABLE IF NOT EXISTS TARIFF (
    id_TARIFF INT PRIMARY KEY,
    tariff_type VARCHAR(25),
    tariff_from VARCHAR(25),
    tariff_to VARCHAR(25),
    price_with_tax DECIMAL(10,2),
    price_unit VARCHAR(15),
    created DATETIME,
    modified DATETIME,
    hidden TINYINT,
    TK_PARKING_LOCATION INT,
    FOREIGN KEY (TK_PARKING_LOCATION) REFERENCES PARKING_LOCATION(id_PARKING_LOCATION)
);


-- SUBSCRIBERS
CREATE TABLE IF NOT EXISTS SUBSCRIBERS (
    id_SUBSCRIBERS INT PRIMARY KEY,
    available_spots INT,
    total_spots INT,
    reserved_spots INT,
    waiting_line INT,
    created DATETIME,
    modified DATETIME,
    hidden TINYINT
);

-- PARKING_LOCATION
CREATE TABLE IF NOT EXISTS PARKING_LOCATION (
    id_PARKING_LOCATION INT PRIMARY KEY,
    name VARCHAR(25),
    address VARCHAR(45),
    latitude VARCHAR(45),
    longitude VARCHAR(45),
    total_regular_spots INT,
    total_invalid_spots INT,
    total_electric_spots INT,
    total_bus_spots INT,
    available_regular_spots INT,
    available_invalid_spots INT,
    available_electric_spots INT,
    available_bus_spots INT,
    created DATETIME,
    modified DATETIME,
    description VARCHAR(300),
    working_hours VARCHAR(45),
    hidden TINYINT,
    TK_SUBSCRIBERS INT,
    FOREIGN KEY (TK_SUBSCRIBERS) REFERENCES SUBSCRIBERS(id_SUBSCRIBERS)
);

-- PAYMENT
CREATE TABLE IF NOT EXISTS PAYMENT (
    id_PAYMENT INT PRIMARY KEY,
    date DATETIME,
    amount DECIMAL(10,2),
    method VARCHAR(45),
    payment_status ENUM('pending', 'completed', 'failed'),
    hidden TINYINT,
    created DATETIME,
    modified DATETIME,
    TK_USER INT,
    TK_PARKING_LOCATION INT,
    FOREIGN KEY (TK_USER) REFERENCES USER(id_USER),
    FOREIGN KEY (TK_PARKING_LOCATION) REFERENCES PARKING_LOCATION(id_PARKING_LOCATION)
);

-- REVIEWS
CREATE TABLE IF NOT EXISTS REVIEWS (
    id_REVIEWS INT PRIMARY KEY,
    rating INT,
    review_text VARCHAR(300),
    review_date DATETIME,
    created DATETIME,
    modified DATETIME,
    hidden TINYINT,
    TK_USER INT,
    TK_PARKING_LOCATION INT,
    FOREIGN KEY (TK_USER) REFERENCES USER(id_USER),
    FOREIGN KEY (TK_PARKING_LOCATION) REFERENCES PARKING_LOCATION(id_PARKING_LOCATION)
);

-- CHANGE_LOG
CREATE TABLE IF NOT EXISTS CHANGE_LOG (
    id_CHANGE_LOG INT PRIMARY KEY,
    changed_table_name VARCHAR(100),
    record_id INT,
    type_of_change VARCHAR(45),
    time_of_change DATETIME
);
