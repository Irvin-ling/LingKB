/*
Navicat MySQL Data Transfer

Source Server         : 本地
Source Server Version : 50742

Target Server Type    : MYSQL
Target Server Version : 50742
File Encoding         : 65001

Date: 2025-07-25 14:43:39
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for ling_document
-- ----------------------------
DROP TABLE IF EXISTS `ling_document`;
CREATE TABLE `ling_document` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `doc_id` varchar(255) NOT NULL,
  `workspace` varchar(255) NOT NULL,
  `text` text NOT NULL,
  `author` varchar(255) DEFAULT NULL,
  `size` bigint(20) DEFAULT NULL,
  `source_file_name` varchar(255) DEFAULT NULL,
  `creation_date` bigint(20) DEFAULT NULL,
  `page_count` int(11) DEFAULT NULL,
  `char_count` int(11) DEFAULT NULL,
  `word_count` int(11) DEFAULT NULL,
  `sentence_count` int(11) DEFAULT NULL,
  `keywords` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=778 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for ling_document_link
-- ----------------------------
DROP TABLE IF EXISTS `ling_document_link`;
CREATE TABLE `ling_document_link` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `doc_id` varchar(255) NOT NULL,
  `workspace` varchar(255) NOT NULL,
  `type` int(11) DEFAULT NULL COMMENT '0-image|1-code|2-table|3-web',
  `content` longtext,
  `content_assistant` varchar(255) DEFAULT NULL,
  `desc_text` text,
  `desc_vector` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=126 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for ling_vector
-- ----------------------------
DROP TABLE IF EXISTS `ling_vector`;
CREATE TABLE `ling_vector` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `doc_id` varchar(255) NOT NULL,
  `workspace` varchar(255) NOT NULL,
  `node_id` int(11) DEFAULT NULL,
  `txt` text NOT NULL,
  `vector` text NOT NULL,
  `persisted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=492 DEFAULT CHARSET=utf8mb4;
