-- Creates the empty database schema for a new Docker MySQL volume.
-- Do not run this file against an existing database: it is a fresh-install schema.
CREATE DATABASE IF NOT EXISTS car_rental_jdbc
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE car_rental_jdbc;
SET FOREIGN_KEY_CHECKS = 0;

--
-- Table structure for table `admin`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `login_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_login_id` (`login_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `parking_lot`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parking_lot` (
  `parking_lot_id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `max_parking_count` int NOT NULL,
  `current_parking_count` int NOT NULL DEFAULT '0',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `rented_car_count` int NOT NULL DEFAULT '0',
  `parking_lot_status` enum('ACTIVE','INACTIVE') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (`parking_lot_id`),
  UNIQUE KEY `uk_parking_lot_name_address` (`name`,`address`),
  CONSTRAINT `chk_parking_capacity` CHECK (((`current_parking_count` >= 0) and (`rented_car_count` >= 0) and ((`current_parking_count` + `rented_car_count`) <= `max_parking_count`))),
  CONSTRAINT `chk_parking_count` CHECK (((`current_parking_count` >= 0) and (`current_parking_count` <= `max_parking_count`))),
  CONSTRAINT `chk_parking_max_count` CHECK ((`max_parking_count` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `car`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `car` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `max_passenger_count` int NOT NULL,
  `price_per_km` int NOT NULL,
  `current_parking_lot_id` int DEFAULT NULL,
  `car_status` enum('AVAILABLE','RENTED','MAINTENANCE','INACTIVE') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AVAILABLE',
  `current_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_car_name_max_passenger_count` (`name`,`max_passenger_count`),
  KEY `fk_car_current_parking_lot` (`current_parking_lot_id`),
  CONSTRAINT `fk_car_current_parking_lot` FOREIGN KEY (`current_parking_lot_id`) REFERENCES `parking_lot` (`parking_lot_id`) ON DELETE SET NULL,
  CONSTRAINT `chk_car_passenger_count` CHECK ((`max_passenger_count` > 0)),
  CONSTRAINT `chk_car_price_per_km` CHECK ((`price_per_km` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `car_image`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `car_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `car_id` int NOT NULL,
  `storage_key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `size_bytes` bigint NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_car_image_storage_key` (`storage_key`),
  UNIQUE KEY `uk_car_image_car_sort_order` (`car_id`,`sort_order`),
  CONSTRAINT `fk_car_image_car` FOREIGN KEY (`car_id`) REFERENCES `car` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_car_image_size` CHECK ((`size_bytes` > 0)),
  CONSTRAINT `chk_car_image_sort_order` CHECK ((`sort_order` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `member`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `license_status` tinyint(1) NOT NULL,
  `login_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_login_id` (`login_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rental`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rental` (
  `id` int NOT NULL AUTO_INCREMENT,
  `start_time` timestamp NOT NULL,
  `return_time` timestamp NOT NULL,
  `passengers_count` int NOT NULL,
  `rental_status` enum('RESERVED','IN_PROGRESS','COMPLETED','CANCELED','NO_SHOW') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RESERVED',
  `car_id` int NOT NULL,
  `pickup_parking_lot_id` int NOT NULL,
  `member_id` int NOT NULL,
  `actual_pickup_time` timestamp NULL DEFAULT NULL,
  `actual_return_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_rental_pickup_parking_lot` (`pickup_parking_lot_id`),
  KEY `fk_rental_member` (`member_id`),
  KEY `idx_rental_car_period` (`car_id`,`start_time`,`return_time`,`rental_status`),
  CONSTRAINT `fk_rental_car` FOREIGN KEY (`car_id`) REFERENCES `car` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_rental_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_rental_pickup_parking_lot` FOREIGN KEY (`pickup_parking_lot_id`) REFERENCES `parking_lot` (`parking_lot_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_rental_passengers` CHECK ((`passengers_count` > 0)),
  CONSTRAINT `chk_rental_time` CHECK ((`return_time` > `start_time`)),
  CONSTRAINT `chk_rental_time_order` CHECK ((`return_time` > `start_time`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

SET FOREIGN_KEY_CHECKS = 1;
