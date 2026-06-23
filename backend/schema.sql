-- EnjoyTrip Database Schema
-- 실행: MySQL Workbench 또는 CLI에서 수동으로 실행하세요.
-- $ mysql -u ssafy -p < schema.sql

CREATE DATABASE IF NOT EXISTS ssafy_trip
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ssafy_trip;

-- ─────────────────────────────────────────
-- 시도 (광역시/도)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sidos (
    sido_code INT          NOT NULL COMMENT '시도 코드',
    sido_name VARCHAR(20)  NOT NULL COMMENT '시도명',
    PRIMARY KEY (sido_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────
-- 구군 (시/군/구)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS guguns (
    gugun_code INT         NOT NULL COMMENT '구군 코드',
    sido_code  INT         NOT NULL COMMENT '시도 코드 (FK)',
    gugun_name VARCHAR(20) NOT NULL COMMENT '구군명',
    PRIMARY KEY (sido_code, gugun_code),
    CONSTRAINT fk_gugun_sido FOREIGN KEY (sido_code) REFERENCES sidos (sido_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────
-- 관광지
-- 초기 데이터: SSAFY 제공 덤프 파일 또는 한국관광공사 공공데이터를 별도 임포트하세요.
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS attractions (
    content_id      INT            NOT NULL COMMENT '콘텐츠 ID (관광공사 고유키)',
    title           VARCHAR(200)   NOT NULL COMMENT '관광지명',
    content_type_id INT            NOT NULL COMMENT '콘텐츠 유형 (12:관광지 14:문화시설 등)',
    area_code       INT            NOT NULL COMMENT '시도 코드 (FK)',
    si_gun_gu_code  INT            NOT NULL COMMENT '구군 코드',
    first_image1    VARCHAR(500)       NULL COMMENT '대표 이미지 URL',
    first_image2    VARCHAR(500)       NULL COMMENT '썸네일 이미지 URL',
    latitude        DOUBLE         NOT NULL COMMENT '위도',
    longitude       DOUBLE         NOT NULL COMMENT '경도',
    addr1           VARCHAR(300)       NULL COMMENT '주소',
    overview        TEXT               NULL COMMENT '개요',
    PRIMARY KEY (content_id),
    CONSTRAINT fk_attraction_sido FOREIGN KEY (area_code) REFERENCES sidos (sido_code),
    INDEX idx_area_code       (area_code),
    INDEX idx_content_type_id (content_type_id),
    INDEX idx_si_gun_gu_code  (si_gun_gu_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────
-- 시도 기초 데이터 (전국 17개 시도)
-- ─────────────────────────────────────────
INSERT IGNORE INTO sidos (sido_code, sido_name) VALUES
(11, '서울'),
(26, '부산'),
(27, '대구'),
(28, '인천'),
(29, '광주'),
(30, '대전'),
(31, '울산'),
(36, '세종'),
(41, '경기'),
(43, '충북'),
(44, '충남'),
(46, '전남'),
(47, '경북'),
(48, '경남'),
(50, '제주'),
(51, '강원'),
(52, '전북');
