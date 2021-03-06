/*
 Navicat Premium Data Transfer

 Source Server         : 10.100.20.243
 Source Server Type    : MySQL
 Source Server Version : 80021
 Source Host           : 10.100.20.243:3306
 Source Schema         : eto_crm_sys

 Target Server Type    : MySQL
 Target Server Version : 80021
 File Encoding         : 65001

 Date: 10/10/2020 16:14:32
*/

CREATE database if NOT EXISTS eto_crm_sys default character set utf8mb4 collate utf8mb4_unicode_ci;

use eto_crm_sys;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for BATCH_JOB_EXECUTION
-- ----------------------------
DROP TABLE IF EXISTS `BATCH_JOB_EXECUTION`;
CREATE TABLE `BATCH_JOB_EXECUTION`  (
  `JOB_EXECUTION_ID` bigint(0) NOT NULL,
  `VERSION` bigint(0) NULL DEFAULT NULL,
  `JOB_INSTANCE_ID` bigint(0) NOT NULL,
  `CREATE_TIME` datetime(0) NOT NULL,
  `START_TIME` datetime(0) NULL DEFAULT NULL,
  `END_TIME` datetime(0) NULL DEFAULT NULL,
  `STATUS` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `EXIT_CODE` varchar(2500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `EXIT_MESSAGE` varchar(2500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `LAST_UPDATED` datetime(0) NULL DEFAULT NULL,
  `JOB_CONFIGURATION_LOCATION` varchar(2500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`JOB_EXECUTION_ID`) USING BTREE,
  INDEX `JOB_INST_EXEC_FK`(`JOB_INSTANCE_ID`) USING BTREE,
  CONSTRAINT `JOB_INST_EXEC_FK` FOREIGN KEY (`JOB_INSTANCE_ID`) REFERENCES `BATCH_JOB_INSTANCE` (`JOB_INSTANCE_ID`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for BATCH_JOB_EXECUTION_CONTEXT
-- ----------------------------
DROP TABLE IF EXISTS `BATCH_JOB_EXECUTION_CONTEXT`;
CREATE TABLE `BATCH_JOB_EXECUTION_CONTEXT`  (
  `JOB_EXECUTION_ID` bigint(0) NOT NULL,
  `SHORT_CONTEXT` varchar(2500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `SERIALIZED_CONTEXT` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  PRIMARY KEY (`JOB_EXECUTION_ID`) USING BTREE,
  CONSTRAINT `JOB_EXEC_CTX_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for BATCH_JOB_EXECUTION_PARAMS
-- ----------------------------
DROP TABLE IF EXISTS `BATCH_JOB_EXECUTION_PARAMS`;
CREATE TABLE `BATCH_JOB_EXECUTION_PARAMS`  (
  `JOB_EXECUTION_ID` bigint(0) NOT NULL,
  `TYPE_CD` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `KEY_NAME` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `STRING_VAL` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `DATE_VAL` datetime(0) NULL DEFAULT NULL,
  `LONG_VAL` bigint(0) NULL DEFAULT NULL,
  `DOUBLE_VAL` double NULL DEFAULT NULL,
  `IDENTIFYING` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  INDEX `JOB_EXEC_PARAMS_FK`(`JOB_EXECUTION_ID`) USING BTREE,
  CONSTRAINT `JOB_EXEC_PARAMS_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for BATCH_JOB_EXECUTION_SEQ
-- ----------------------------
DROP TABLE IF EXISTS `BATCH_JOB_EXECUTION_SEQ`;
CREATE TABLE `BATCH_JOB_EXECUTION_SEQ`  (
  `ID` bigint(0) NOT NULL,
  `UNIQUE_KEY` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  UNIQUE INDEX `UNIQUE_KEY_UN`(`UNIQUE_KEY`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for BATCH_JOB_INSTANCE
-- ----------------------------
DROP TABLE IF EXISTS `BATCH_JOB_INSTANCE`;
CREATE TABLE `BATCH_JOB_INSTANCE`  (
  `JOB_INSTANCE_ID` bigint(0) NOT NULL,
  `VERSION` bigint(0) NULL DEFAULT NULL,
  `JOB_NAME` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `JOB_KEY` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`JOB_INSTANCE_ID`) USING BTREE,
  UNIQUE INDEX `JOB_INST_UN`(`JOB_NAME`, `JOB_KEY`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for BATCH_JOB_SEQ
-- ----------------------------
DROP TABLE IF EXISTS `BATCH_JOB_SEQ`;
CREATE TABLE `BATCH_JOB_SEQ`  (
  `ID` bigint(0) NOT NULL,
  `UNIQUE_KEY` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  UNIQUE INDEX `UNIQUE_KEY_UN`(`UNIQUE_KEY`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for BATCH_STEP_EXECUTION
-- ----------------------------
DROP TABLE IF EXISTS `BATCH_STEP_EXECUTION`;
CREATE TABLE `BATCH_STEP_EXECUTION`  (
  `STEP_EXECUTION_ID` bigint(0) NOT NULL,
  `VERSION` bigint(0) NOT NULL,
  `STEP_NAME` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `JOB_EXECUTION_ID` bigint(0) NOT NULL,
  `START_TIME` datetime(0) NOT NULL,
  `END_TIME` datetime(0) NULL DEFAULT NULL,
  `STATUS` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `COMMIT_COUNT` bigint(0) NULL DEFAULT NULL,
  `READ_COUNT` bigint(0) NULL DEFAULT NULL,
  `FILTER_COUNT` bigint(0) NULL DEFAULT NULL,
  `WRITE_COUNT` bigint(0) NULL DEFAULT NULL,
  `READ_SKIP_COUNT` bigint(0) NULL DEFAULT NULL,
  `WRITE_SKIP_COUNT` bigint(0) NULL DEFAULT NULL,
  `PROCESS_SKIP_COUNT` bigint(0) NULL DEFAULT NULL,
  `ROLLBACK_COUNT` bigint(0) NULL DEFAULT NULL,
  `EXIT_CODE` varchar(2500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `EXIT_MESSAGE` varchar(2500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `LAST_UPDATED` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`STEP_EXECUTION_ID`) USING BTREE,
  INDEX `JOB_EXEC_STEP_FK`(`JOB_EXECUTION_ID`) USING BTREE,
  CONSTRAINT `JOB_EXEC_STEP_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for BATCH_STEP_EXECUTION_CONTEXT
-- ----------------------------
DROP TABLE IF EXISTS `BATCH_STEP_EXECUTION_CONTEXT`;
CREATE TABLE `BATCH_STEP_EXECUTION_CONTEXT`  (
  `STEP_EXECUTION_ID` bigint(0) NOT NULL,
  `SHORT_CONTEXT` varchar(2500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `SERIALIZED_CONTEXT` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  PRIMARY KEY (`STEP_EXECUTION_ID`) USING BTREE,
  CONSTRAINT `STEP_EXEC_CTX_FK` FOREIGN KEY (`STEP_EXECUTION_ID`) REFERENCES `BATCH_STEP_EXECUTION` (`STEP_EXECUTION_ID`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for BATCH_STEP_EXECUTION_SEQ
-- ----------------------------
DROP TABLE IF EXISTS `BATCH_STEP_EXECUTION_SEQ`;
CREATE TABLE `BATCH_STEP_EXECUTION_SEQ`  (
  `ID` bigint(0) NOT NULL,
  `UNIQUE_KEY` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  UNIQUE INDEX `UNIQUE_KEY_UN`(`UNIQUE_KEY`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_audit_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_audit_log`;
CREATE TABLE `sys_audit_log`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `user_id` int(0) NULL DEFAULT NULL COMMENT '??????id',
  `thread_id` int(0) NULL DEFAULT NULL COMMENT '??????ID',
  `request_id` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `request_ip` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `request_pkg` varchar(256) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `request_method` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `proces_time` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `request_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '????????????',
  `response_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '????????????',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` varchar(1) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????????????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_brands
-- ----------------------------
DROP TABLE IF EXISTS `sys_brands`;
CREATE TABLE `sys_brands`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `org_id` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `destination_database_id` int(0) NULL DEFAULT NULL COMMENT '???????????????id',
  `brands_code` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `brands_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `brands_name_en` varchar(526) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `brands_full_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `brands_memo` varchar(1024) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `brands_logo_url` varchar(526) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `brands_status` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????????????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_brands_org
-- ----------------------------
DROP TABLE IF EXISTS `sys_brands_org`;
CREATE TABLE `sys_brands_org`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `org_code` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `org_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `org_name_en` varchar(526) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `org_full_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `org_status` varchar(1) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????????????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_data_source
-- ----------------------------
DROP TABLE IF EXISTS `sys_data_source`;
CREATE TABLE `sys_data_source`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `data_type` int(0) NULL DEFAULT NULL COMMENT '??????????????? ????????????',
  `brands_id` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `data_status` int(0) NULL DEFAULT NULL COMMENT '??????????????? 0??????  1?????????',
  `data_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '???????????????',
  `data_memo` varchar(1024) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '???????????????',
  `data_corn` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '?????????????????????',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) NULL DEFAULT NULL COMMENT '???????????? 0????????? 1??????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `data_flag` int(0) NULL DEFAULT NULL COMMENT '0???????????????1???????????????',
  `order_number` int(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '?????????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_db_source
-- ----------------------------
DROP TABLE IF EXISTS `sys_db_source`;
CREATE TABLE `sys_db_source`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `data_source_id` int(0) NULL DEFAULT NULL COMMENT '?????????ID',
  `db_type_id` int(0) NULL DEFAULT NULL COMMENT '??????????????? ????????????',
  `db_driver_class_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `db_url` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `db_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `db_username` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `db_password` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `filters` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `initial_size` int(0) NULL DEFAULT NULL COMMENT '?????????????????????',
  `initialization_mode` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `max_active` int(0) NULL DEFAULT NULL COMMENT '?????????????????????',
  `min_idle` int(0) NULL DEFAULT NULL COMMENT '?????????????????????',
  `max_wait` int(0) NULL DEFAULT NULL COMMENT '??????????????????',
  `min_evictable_idle_time_millis` int(0) NULL DEFAULT NULL COMMENT '??????????????????',
  `pool_prepared_statements` int(0) NULL DEFAULT NULL COMMENT '????????????pscache',
  `max_pool_prepared_statement_per_connection_size` int(0) NULL DEFAULT NULL COMMENT 'pscache??????????????????',
  `test_on_borrow` int(0) NULL DEFAULT NULL COMMENT '?????????????????????????????????',
  `test_on_return` int(0) NULL DEFAULT NULL COMMENT '?????????????????????????????????',
  `test_while_idle` int(0) NULL DEFAULT NULL COMMENT '?????????????????????????????????????????????????????????',
  `time_between_eviction_runs_millis` int(0) NULL DEFAULT NULL COMMENT '??????????????????',
  `validation_query` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `db_port` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `url_params` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '?????????????????????',
  `db_host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '?????????ip??????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????????????????????????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `dict_code` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `dict_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `dict_value` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `dict_status` int(0) NULL DEFAULT NULL COMMENT '???????????????0????????????1??????',
  `dict_memo` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `is_leaf` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `dict_parent_id` int(0) NULL DEFAULT NULL COMMENT '????????????ID',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) NULL DEFAULT 0 COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `order_number` int(0) NULL DEFAULT 1 COMMENT '???????????????1????????????2?????????',
  `value_flag` int(0) NULL DEFAULT NULL,
  `dict_parent_code` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '???????????????',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `??????code`(`dict_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '??????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_industry
-- ----------------------------
DROP TABLE IF EXISTS `sys_industry`;
CREATE TABLE `sys_industry`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `industry_code` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `industry_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `industry_full_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `industry_memo` varchar(1024) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `industry_status` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) NULL DEFAULT 0 COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????????????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_location
-- ----------------------------
DROP TABLE IF EXISTS `sys_location`;
CREATE TABLE `sys_location`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `local_code` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `local_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `local_full_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `parent_local` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `province_id` int(0) NULL DEFAULT NULL COMMENT '???ID',
  `lng` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `lat` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `local_scope` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) NULL DEFAULT 0 COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????????????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `menu_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '????????????',
  `brands_id` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `menu_lev` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `menu_parent_id` int(0) NULL DEFAULT NULL COMMENT '????????????id',
  `menu_route` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '????????????',
  `menu_status` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `menu_order` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `menu_memo` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '????????????',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) NULL DEFAULT 0 COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_button` int(0) NULL DEFAULT 0 COMMENT '???????????????',
  `button_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '????????????',
  `icon` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT 'icon??????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '??????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_model_table
-- ----------------------------
DROP TABLE IF EXISTS `sys_model_table`;
CREATE TABLE `sys_model_table`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `model_table` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '??????',
  `model_table_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '?????????',
  `relation_rule` json NULL COMMENT '????????????',
  `model_status` int(0) NULL DEFAULT 1 COMMENT '????????????',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) NULL DEFAULT 0 COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '???????????????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_model_table_column
-- ----------------------------
DROP TABLE IF EXISTS `sys_model_table_column`;
CREATE TABLE `sys_model_table_column`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `model_table_id` int(0) NULL DEFAULT NULL COMMENT '?????????ID',
  `column_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `display_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `not_null` int(0) NULL DEFAULT NULL COMMENT '???????????????',
  `data_type` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `length` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `value_type` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_muti_select` int(0) NULL DEFAULT NULL COMMENT '???????????????',
  `relation_table_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `relation_table_id` int(0) NULL DEFAULT NULL COMMENT '?????????ID',
  `display_column_id` int(0) NULL DEFAULT NULL COMMENT '???????????????id',
  `relation_pk` int(0) NULL DEFAULT NULL COMMENT '?????????????????????ID',
  `del_flag` int(0) NULL DEFAULT 0 COMMENT '???????????????0?????? 1????????????',
  `data_range` json NULL COMMENT '??????',
  `logical_operations` json NULL COMMENT '??????????????????',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) NULL DEFAULT 0 COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '?????????????????????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `user_id` int(0) NULL DEFAULT NULL COMMENT '??????ID',
  `role_id` int(0) NULL DEFAULT NULL COMMENT '??????ID',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) UNSIGNED NULL DEFAULT 0 COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????????????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `role_code` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '????????????',
  `role_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '????????????',
  `role_memo` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '????????????',
  `role_status` int(0) NULL DEFAULT 1 COMMENT '????????????',
  `brands_id` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) NULL DEFAULT 0 COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '??????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `menu_id` int(0) NULL DEFAULT NULL COMMENT '??????ID',
  `role_id` int(0) NULL DEFAULT NULL COMMENT '??????ID',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) NULL DEFAULT 0 COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????????????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_synchronization_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_synchronization_config`;
CREATE TABLE `sys_synchronization_config`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `origin_database_id` int(0) NULL DEFAULT NULL COMMENT '????????????ID',
  `destination_database_id` int(0) NULL DEFAULT NULL COMMENT '???????????????ID',
  `origin_table_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `destination_table_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `process_status` int(0) NULL DEFAULT 0 COMMENT '0?????????1?????????2???????????????',
  `primary_key` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `column_data` json NULL COMMENT '??????????????????',
  `sync_memo` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `sync_status` int(0) NULL DEFAULT 1 COMMENT '???????????? 0 ????????? 1??????',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) NULL DEFAULT 0 COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '??????????????????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_tag
-- ----------------------------
DROP TABLE IF EXISTS `sys_tag`;
CREATE TABLE `sys_tag`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `master_tag_id` int(0) NULL DEFAULT 0 COMMENT '??????tagId,0-??????',
  `tag_classes_id` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `tag_code` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `tag_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `tag_memo` varchar(1000) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `tag_update_type` int(0) NULL DEFAULT NULL COMMENT '?????????????????????0-????????????,1-??????',
  `tag_update_cron` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `tag_status` int(0) NULL DEFAULT NULL COMMENT '???????????? 0?????????  1??????',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) NULL DEFAULT 0 COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '??????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_tag_classes
-- ----------------------------
DROP TABLE IF EXISTS `sys_tag_classes`;
CREATE TABLE `sys_tag_classes`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `tag_classes_code` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `tag_classes_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `tag_classes_status` tinyint(0) NULL DEFAULT NULL COMMENT '????????????',
  `tag_classes_memo` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` tinyint(0) NULL DEFAULT 0 COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????????????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_tag_industry
-- ----------------------------
DROP TABLE IF EXISTS `sys_tag_industry`;
CREATE TABLE `sys_tag_industry`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `tag_id` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `industry_id` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) NULL DEFAULT 0 COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '??????????????????????????????' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_tag_property
-- ----------------------------
DROP TABLE IF EXISTS `sys_tag_property`;
CREATE TABLE `sys_tag_property`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `tag_id` int(0) NULL DEFAULT NULL COMMENT '??????ID',
  `property_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `property_memo` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `rule_relationship_id` int(0) NULL DEFAULT NULL COMMENT '????????????id',
  `process_status` int(0) NULL DEFAULT NULL COMMENT '0?????????1?????????2???????????????',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` tinyint(0) NULL DEFAULT 0 COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????????????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_tag_property_rule
-- ----------------------------
DROP TABLE IF EXISTS `sys_tag_property_rule`;
CREATE TABLE `sys_tag_property_rule`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `property_id` int(0) NULL DEFAULT NULL COMMENT '??????ID',
  `model_table_id` int(0) NULL DEFAULT NULL COMMENT '???????????????ID',
  `model_table_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `model_table` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `column_relationship_id` int(0) NULL DEFAULT NULL COMMENT '??????????????????ID',
  `columns` json NULL COMMENT '????????????',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` tinyint(0) NULL DEFAULT 0 COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '??????????????????????????? ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '??????',
  `user_code` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '????????????',
  `user_account` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '????????????',
  `password` varchar(99) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '????????????',
  `user_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '????????????',
  `user_name_en` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '???????????????',
  `brands_id` int(0) NULL DEFAULT NULL COMMENT '????????????',
  `brithday` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '??????',
  `phone` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '?????????',
  `email` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '??????',
  `sex` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL COMMENT '??????',
  `last_login_time` datetime(0) NULL DEFAULT NULL COMMENT '??????????????????',
  `status` int(0) NULL DEFAULT 1 COMMENT '???????????? ????????????',
  `revision` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `created_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `updated_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `updated_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `is_delete` int(0) NULL DEFAULT 0 COMMENT '????????????',
  `delete_by` int(0) NULL DEFAULT NULL COMMENT '?????????',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '????????????',
  `sso_id` int(0) NULL DEFAULT NULL COMMENT '????????????id',
  `organization` int(0) NULL DEFAULT NULL COMMENT '????????????',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '????????????????????? ' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
