-- =============================================
-- Atomic CV — ERD Cloud Import DDL
-- Base: ERD-Cloud export (2026-05-07)
-- DB: MySQL 8.x
-- 변경이력: resume_versions·notifications 제거,
--           blocks 간소화, resumes 구조 변경,
--           resume_blocks.is_visible 제거,
--           view_sessions user_agent·referrer·started_at·ended_at 제거 (2026-05-15)
-- =============================================

-- =====================
-- AUTH CONTEXT
-- =====================

CREATE TABLE users
(
    id                BIGINT                 NOT NULL AUTO_INCREMENT,
    email             VARCHAR(255)           NOT NULL,
    name              VARCHAR(100)           NOT NULL,
    profile_image_url VARCHAR(500)           NULL,
    role              ENUM ('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    is_active         BOOLEAN                NOT NULL DEFAULT TRUE,
    created_at        DATETIME               NOT NULL,
    updated_at        DATETIME               NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    INDEX idx_users_email (email)
);

-- 멀티 소셜 연동 지원 (1:N). users와 합치지 않음.
CREATE TABLE social_accounts
(
    id               BIGINT                            NOT NULL AUTO_INCREMENT,
    user_id          BIGINT                            NOT NULL,
    provider         ENUM ('GOOGLE', 'KAKAO', 'NAVER') NOT NULL,
    provider_user_id VARCHAR(255)                      NOT NULL,
    created_at       DATETIME                          NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_social_accounts_provider_user (provider, provider_user_id),
    INDEX idx_social_accounts_user_id (user_id),
    CONSTRAINT fk_social_accounts_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- =====================
-- BLOCK CONTEXT
-- =====================

-- order_index·is_draft 제거 (MVP 간소화)
CREATE TABLE blocks
(
    id           BIGINT                                                                                 NOT NULL AUTO_INCREMENT,
    user_id      BIGINT                                                                                 NOT NULL,
    type         ENUM ('CAREER', 'SKILL', 'PROJECT', 'EDUCATION', 'CERTIFICATE', 'ACTIVITY', 'CUSTOM') NOT NULL,
    title        VARCHAR(200)                                                                           NOT NULL,
    content_json JSON                                                                                   NOT NULL,
    created_at   DATETIME                                                                               NOT NULL,
    updated_at   DATETIME                                                                               NOT NULL,
    deleted_at   DATETIME                                                                               NULL,
    PRIMARY KEY (id),
    INDEX idx_blocks_user_id (user_id),
    INDEX idx_blocks_user_type (user_id, type),
    CONSTRAINT fk_blocks_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- =====================
-- RESUME CONTEXT
-- =====================

-- type ENUM 값은 팀 확정 필요 (예: PDF / WEB)
-- is_published → is_public 이름 변경
-- pdf_s3_key 추가, current_version_id 제거
CREATE TABLE resumes
(
    id           BIGINT               NOT NULL AUTO_INCREMENT,
    user_id      BIGINT               NOT NULL,
    type         ENUM ('PDF', 'WEB')  NULL,
    title        VARCHAR(200)         NOT NULL,
    slug         VARCHAR(100)         NOT NULL,
    is_public    BOOLEAN              NOT NULL DEFAULT FALSE,
    pdf_s3_key   VARCHAR(500)         NULL,
    created_at   DATETIME             NOT NULL,
    updated_at   DATETIME             NOT NULL,
    deleted_at   DATETIME             NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_resumes_slug (slug),
    INDEX idx_resumes_user_id (user_id),
    INDEX idx_resumes_slug (slug),
    CONSTRAINT fk_resumes_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE resume_blocks
(
    id          BIGINT NOT NULL AUTO_INCREMENT,
    resume_id   BIGINT NOT NULL,
    block_id    BIGINT NOT NULL,
    order_index INT    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_resume_blocks_resume_block (resume_id, block_id),
    INDEX idx_resume_blocks_resume_id (resume_id),
    INDEX idx_resume_blocks_block_id (block_id),
    CONSTRAINT fk_resume_blocks_resume FOREIGN KEY (resume_id) REFERENCES resumes (id),
    CONSTRAINT fk_resume_blocks_block FOREIGN KEY (block_id) REFERENCES blocks (id)
);

-- =====================
-- FEEDBACK CONTEXT
-- =====================

-- version_id·reviewer_name·reviewer_email 제거 (완전 익명화 단순화)
CREATE TABLE feedbacks
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    resume_id   BIGINT      NOT NULL,
    rating      TINYINT     NOT NULL,
    comment     TEXT        NULL,
    reviewer_ip VARCHAR(45) NOT NULL,
    created_at  DATETIME    NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_feedbacks_resume_id (resume_id),
    INDEX idx_feedbacks_reviewer_ip_created (reviewer_ip, created_at),
    CONSTRAINT fk_feedbacks_resume FOREIGN KEY (resume_id) REFERENCES resumes (id)
);

CREATE TABLE feedback_tags
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    feedback_id BIGINT      NOT NULL,
    tag         VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_feedback_tags_feedback_id (feedback_id),
    CONSTRAINT fk_feedback_tags_feedback FOREIGN KEY (feedback_id) REFERENCES feedbacks (id)
);

-- =====================
-- ANALYTICS CONTEXT
-- =====================

-- version_id 제거, user_agent·referrer·started_at·ended_at 제거
CREATE TABLE view_sessions
(
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    resume_id          BIGINT      NOT NULL,
    visitor_ip         VARCHAR(45) NOT NULL,
    total_duration_sec INT         NULL,
    created_at         DATETIME    NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_view_sessions_resume_id (resume_id),
    CONSTRAINT fk_view_sessions_resume FOREIGN KEY (resume_id) REFERENCES resumes (id)
);

CREATE TABLE section_dwells
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    view_session_id BIGINT       NOT NULL,
    block_id        BIGINT       NULL,
    section_name    VARCHAR(100) NOT NULL,
    dwell_seconds   INT          NOT NULL,
    created_at      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_section_dwells_view_session_id (view_session_id),
    CONSTRAINT fk_section_dwells_view_session FOREIGN KEY (view_session_id) REFERENCES view_sessions (id),
    CONSTRAINT fk_section_dwells_block FOREIGN KEY (block_id) REFERENCES blocks (id)
);
