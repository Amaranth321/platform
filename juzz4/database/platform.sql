SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
-- MySQL dump 10.13  Distrib 5.1.63, for debian-linux-gnu (i686)
--
-- Host: localhost    Database: platform
-- ------------------------------------------------------
-- Server version	5.1.63-0ubuntu0.11.10.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Database: `platform`
--

DROP DATABASE IF EXISTS `platform`;
CREATE DATABASE IF NOT EXISTS `platform` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;
USE `platform`;

--
-- Table structure for table `devices`
--

-- DROP TABLE IF EXISTS `devices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `devices` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `bucket_id` bigint(20) NOT NULL,
  `model_id` bigint(20) NOT NULL,
  `ADDRESS` varchar(255) DEFAULT NULL,
  `DEVICEID` varchar(255) DEFAULT NULL,
  `HOST` varchar(255) DEFAULT NULL,
  `DEVICEKEY` varchar(255) DEFAULT NULL,
  `LATITUDE` varchar(255) DEFAULT NULL,
  `LOGIN` varchar(255) DEFAULT NULL,
  `LONGITUDE` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `PASSWORD` varchar(255) DEFAULT NULL,
  `PORT` varchar(255) DEFAULT NULL,
  `CLOUDRECORDINGENABLED` bit(1) DEFAULT NULL,
  UNIQUE KEY `PRIMARY_KEY_A` (`ID`),
  KEY `FK27971388B44AD9E_INDEX_2` (`bucket_id`),
  KEY `FK27971388B44AD9E` (`bucket_id`),
  KEY `FK27971388B44AD9F` (`model_id`),
  CONSTRAINT `FK27971388B44AD9E` FOREIGN KEY (`bucket_id`) REFERENCES `buckets` (`ID`),
  CONSTRAINT `FK27971388B44AD9F` FOREIGN KEY (`model_id`) REFERENCES `device_models` (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `devices`
--

LOCK TABLES `devices` WRITE;
/*!40000 ALTER TABLE `devices` DISABLE KEYS */;
/*!40000 ALTER TABLE `devices` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `buckets`
--

-- DROP TABLE IF EXISTS `buckets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `buckets` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(255) DEFAULT NULL,
  `PATH` varchar(255) DEFAULT NULL,
  `DESCRIPTION` varchar(255) DEFAULT NULL,
  `ACTIVATED` tinyint(1) NOT NULL,
  UNIQUE KEY `PRIMARY_KEY_D` (`ID`)
) ENGINE=InnoDB;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `buckets`
--

LOCK TABLES `buckets` WRITE;
/*!40000 ALTER TABLE `buckets` DISABLE KEYS */;
/*!40000 ALTER TABLE `buckets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `buckets_features`
--
CREATE TABLE `buckets_features` (
  `buckets_id` bigint(20) NOT NULL,
  `features_id` bigint(20) NOT NULL,
  KEY `FKA66E53F3E068C35F` (`buckets_id`),
  KEY `FKA66E53F3BC7E4BAF` (`features_id`),
  CONSTRAINT `FKA66E53F3BC7E4BAF` FOREIGN KEY (`features_id`) REFERENCES `features` (`id`),
  CONSTRAINT `FKA66E53F3E068C35F` FOREIGN KEY (`buckets_id`) REFERENCES `buckets` (`ID`)
) ENGINE=InnoDB;

--
-- Table structure for table `buckets_roles`
--
CREATE TABLE `buckets_roles` (
  `buckets_id` bigint(20) NOT NULL,
  `roles_id` bigint(20) NOT NULL,
  KEY `FK7E882AC7E068C35F` (`buckets_id`),
  KEY `FK7E882AC74001C377` (`roles_id`),
  CONSTRAINT `FK7E882AC74001C377` FOREIGN KEY (`roles_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FK7E882AC7E068C35F` FOREIGN KEY (`buckets_id`) REFERENCES `buckets` (`ID`)
) ENGINE=InnoDB;

--
-- Table structure for table `buckets_services`
--

-- DROP TABLE IF EXISTS `buckets_services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `buckets_services` (
  `buckets_id` bigint(20) NOT NULL,
  `services_id` bigint(20) NOT NULL,
  KEY `FK9F68014E068C35F_INDEX_9` (`buckets_id`),
  KEY `FK9F6801410A6EBAD_INDEX_9` (`services_id`),
  KEY `FK9F68014E068C35F` (`buckets_id`),
  KEY `FK9F6801410A6EBAD` (`services_id`),
  CONSTRAINT `FK9F6801410A6EBAD` FOREIGN KEY (`services_id`) REFERENCES `services` (`ID`),
  CONSTRAINT `FK9F68014E068C35F` FOREIGN KEY (`buckets_id`) REFERENCES `buckets` (`ID`)
) ENGINE=InnoDB;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `buckets_services`
--

LOCK TABLES `buckets_services` WRITE;
/*!40000 ALTER TABLE `buckets_services` DISABLE KEYS */;
/*!40000 ALTER TABLE `buckets_services` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `buckets_users`
--

-- DROP TABLE IF EXISTS `buckets_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `buckets_users` (
  `buckets_id` bigint(20) NOT NULL,
  `users_id` bigint(20) NOT NULL,
  UNIQUE KEY `CONSTRAINT_INDEX_7` (`users_id`),
  KEY `FK7EB42A12E068C35F_INDEX_7` (`buckets_id`),
  KEY `FK7EB42A12E068C35F` (`buckets_id`),
  KEY `FK7EB42A124004E7A1` (`users_id`),
  CONSTRAINT `FK7EB42A124004E7A1` FOREIGN KEY (`users_id`) REFERENCES `users` (`ID`),
  CONSTRAINT `FK7EB42A12E068C35F` FOREIGN KEY (`buckets_id`) REFERENCES `buckets` (`ID`)
) ENGINE=InnoDB;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `buckets_users`
--

LOCK TABLES `buckets_users` WRITE;
/*!40000 ALTER TABLE `buckets_users` DISABLE KEYS */;
/*!40000 ALTER TABLE `buckets_users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_models`
--

-- DROP TABLE IF EXISTS `device_models`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_models` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `MODELID` bigint(20) NOT NULL,
  `CHANNELS` int(11) DEFAULT NULL,
  `LIVEVIEW` longtext,
  `MISC` longtext,
  `NAME` varchar(255) DEFAULT NULL,
  `CAPABILITIES` varchar(255) DEFAULT NULL,
  UNIQUE KEY `PRIMARY_KEY_B` (`ID`)
) ENGINE=InnoDB;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_models`
--

LOCK TABLES `device_models` WRITE;
/*!40000 ALTER TABLE `device_models` DISABLE KEYS */;
/*!40000 ALTER TABLE `device_models` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `services`
--

-- DROP TABLE IF EXISTS `services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `services` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(255) DEFAULT NULL,
  `VERSION` varchar(255) DEFAULT NULL,
  UNIQUE KEY `PRIMARY_KEY_5` (`ID`)
) ENGINE=InnoDB;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `services`
--

LOCK TABLES `services` WRITE;
/*!40000 ALTER TABLE `services` DISABLE KEYS */;

/*!40000 ALTER TABLE `services` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `devices_users`
--

-- DROP TABLE IF EXISTS `devices_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `devices_users` (
  `devices_id` bigint(20) NOT NULL,
  `users_id` bigint(20) NOT NULL,
  KEY `FKB7E8446947140EFF_INDEX_2` (`devices_id`),
  KEY `FKB7E8446947140EFE_INDEX_B` (`users_id`),
  KEY `FKB7E8446947140EFF` (`devices_id`),
  KEY `FKB7E8446947140EFE` (`users_id`),
  CONSTRAINT `FKB7E8446947140EFF` FOREIGN KEY (`devices_id`) REFERENCES `devices` (`ID`),
  CONSTRAINT `FKB7E8446947140EFE` FOREIGN KEY (`users_id`) REFERENCES `users` (`ID`)
) ENGINE=InnoDB;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `devices_users`
--

LOCK TABLES `devices_users` WRITE;
/*!40000 ALTER TABLE `devices_users` DISABLE KEYS */;
/*!40000 ALTER TABLE `devices_users` ENABLE KEYS */;
UNLOCK TABLES;


--
-- Table structure for table `features`
--
CREATE TABLE `features` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;


 --
 -- Table structure for table `features_services`
 --
CREATE TABLE `features_services` (
  `features_id` bigint(20) NOT NULL,
  `services_id` bigint(20) NOT NULL,
  KEY `FKC8ADD220BC7E4BAF` (`features_id`),
  KEY `FKC8ADD22010A6EBAD` (`services_id`),
  CONSTRAINT `FKC8ADD22010A6EBAD` FOREIGN KEY (`services_id`) REFERENCES `services` (`ID`),
  CONSTRAINT `FKC8ADD220BC7E4BAF` FOREIGN KEY (`features_id`) REFERENCES `features` (`id`)
) ENGINE=InnoDB;

--
-- Table structure for table `inventory_items`
--
CREATE TABLE `inventory_items` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `activated` bit(1) NOT NULL,
  `macAddress` varchar(255) DEFAULT NULL,
  `modelNumber` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `registrationNumber` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

--
-- Table structure for table `roles`
--
CREATE TABLE `roles` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

 --
 -- Table structure for table `roles_features`
 --
CREATE TABLE `roles_features` (
  `roles_id` bigint(20) NOT NULL,
  `features_id` bigint(20) NOT NULL,
  KEY `FKADCFCB7F4001C377` (`roles_id`),
  KEY `FKADCFCB7FBC7E4BAF` (`features_id`),
  CONSTRAINT `FKADCFCB7FBC7E4BAF` FOREIGN KEY (`features_id`) REFERENCES `features` (`id`),
  CONSTRAINT `FKADCFCB7F4001C377` FOREIGN KEY (`roles_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB;

--
-- Table structure for table `inventory_items`
--

-- DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `ACTIVATED` tinyint(1) NOT NULL,
  `BUCKETID` bigint(20) DEFAULT NULL,
  `CREATIONTIMESTAMP` varchar(255) DEFAULT NULL,
  `EMAIL` varchar(255) DEFAULT NULL,
  `LOGIN` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `PASSWORD` varchar(255) DEFAULT NULL,
  `SESSION_TIMEOUT` int(11) DEFAULT NULL,
  `TWO_FACTOR_MODE` int(11) DEFAULT NULL,
  UNIQUE KEY `PRIMARY_KEY_6` (`ID`)
) ENGINE=InnoDB;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users_services`
--

-- DROP TABLE IF EXISTS `users_services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users_services` (
  `users_id` bigint(20) NOT NULL,
  `services_id` bigint(20) NOT NULL,
  KEY `FKCABEFCB54004E7A1_INDEX_C` (`users_id`),
  KEY `FKCABEFCB510A6EBAD_INDEX_C` (`services_id`),
  KEY `FKCABEFCB54004E7A1` (`users_id`),
  KEY `FKCABEFCB510A6EBAD` (`services_id`),
  CONSTRAINT `FKCABEFCB510A6EBAD` FOREIGN KEY (`services_id`) REFERENCES `services` (`ID`),
  CONSTRAINT `FKCABEFCB54004E7A1` FOREIGN KEY (`users_id`) REFERENCES `users` (`ID`)
) ENGINE=InnoDB;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users_services`
--

LOCK TABLES `users_services` WRITE;
/*!40000 ALTER TABLE `users_services` DISABLE KEYS */;
/*!40000 ALTER TABLE `users_services` ENABLE KEYS */;
UNLOCK TABLES;


--
-- Table structure for table `users_roles`
--

-- DROP TABLE IF EXISTS `users_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users_roles` (
  `users_id` bigint(20) NOT NULL,
  `roles_id` bigint(20) NOT NULL,
  KEY `FKF6CCD9C64001C377` (`roles_id`),
  KEY `FKF6CCD9C64004E7A1` (`users_id`),
  CONSTRAINT `FKF6CCD9C64001C377` FOREIGN KEY (`roles_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FKF6CCD9C64004E7A1` FOREIGN KEY (`users_id`) REFERENCES `users` (`ID`)
) ENGINE=InnoDB;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userprefs`
--
CREATE TABLE `userprefs` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `APNSDEVICETOKEN` varchar(255) DEFAULT NULL,
  `GCMDEVICETOKEN` varchar(255) DEFAULT NULL,
  `PUSHNOTIFICATIONENABLED` tinyint(1) NOT NULL,
  `EMAILNOTIFICATIONENABLED` tinyint(1) NOT NULL,
  `SMSNOTIFICATIONENABLED` tinyint(1) NOT NULL,
  `slotSettingAssignments` varchar(1023) DEFAULT NULL,
  `numberOfViews` int(11) DEFAULT NULL,
  UNIQUE KEY `PRIMARY_KEY_7` (`ID`),
  KEY `FK27971388B44ADA1` (`user_id`),
  CONSTRAINT `FK27971388B44ADA1` FOREIGN KEY (`user_id`) REFERENCES `users` (`ID`)
) ENGINE=InnoDB;

--
-- Dumping data for table `userprefs`
--

LOCK TABLES `userprefs` WRITE;
/*!40000 ALTER TABLE `userprefs` DISABLE KEYS */;
/*!40000 ALTER TABLE `userprefs` ENABLE KEYS */;
UNLOCK TABLES;


/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2012-10-08 21:05:00
