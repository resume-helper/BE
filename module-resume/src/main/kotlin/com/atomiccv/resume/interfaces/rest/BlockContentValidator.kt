package com.atomiccv.resume.interfaces.rest

import com.atomiccv.resume.domain.model.BlockType
import com.atomiccv.shared.common.exception.BusinessException
import com.atomiccv.shared.common.exception.ErrorCode
import com.fasterxml.jackson.databind.JsonNode

@Suppress("TooManyFunctions")
object BlockContentValidator {
    fun validate(
        blockType: BlockType,
        content: JsonNode
    ) {
        when (blockType) {
            BlockType.BASIC_INFO -> validateBasicInfo(content)
            BlockType.SUMMARY -> validateSummary(content)
            BlockType.INTRODUCTION -> validateIntroduction(content)
            BlockType.CAREER -> validateCareer(content)
            BlockType.PROJECT -> validateProject(content)
            BlockType.SKILL -> validateSkill(content)
            BlockType.EDUCATION -> validateEducation(content)
            BlockType.CERTIFICATE -> validateCertificate(content)
            BlockType.ACTIVITY -> validateActivity(content)
            BlockType.CUSTOM -> Unit
        }
    }

    private fun validateBasicInfo(content: JsonNode) {
        requireNotBlank(content, "name")
        requireEmail(content, "email")
        requirePhoneNumber(content, "phoneNumber")
    }

    private fun validateSummary(content: JsonNode) {
        requireNotBlank(content, "title")
        requireNotBlank(content, "content")
    }

    private fun validateIntroduction(content: JsonNode) {
        requireNotBlank(content, "title")
        requireNotBlank(content, "content")
    }

    private fun validateCareer(content: JsonNode) {
        requireNotBlank(content, "companyName")
        requireNotBlank(content, "department")
        requireNotBlank(content, "jobTitle")
        requireNotBlank(content, "position")
        requireEnum(content, "employmentType", setOf("FULL_TIME", "CONTRACT", "INTERN", "FREELANCER"))
        requireDateFormat(content, "startDate", YYYY_MM_REGEX)
        requireDateFormat(content, "endDate", YYYY_MM_REGEX)
        requireNotBlank(content, "achievements")
    }

    private fun validateProject(content: JsonNode) {
        requireNotBlank(content, "projectName")
        requireDateFormat(content, "startDate", YYYY_MM_DD_REGEX)
        requireDateFormat(content, "endDate", YYYY_MM_DD_REGEX)
    }

    private fun validateSkill(content: JsonNode) {
        requireNotBlank(content, "skillName")
        requireNotBlank(content, "usageScope")
        requireOptionalEnum(content, "proficiency", setOf("LOW", "MEDIUM", "HIGH"))
    }

    private fun validateEducation(content: JsonNode) {
        requireEnum(content, "educationLevel", setOf("COLLEGE_OR_ABOVE", "HIGH_SCHOOL", "OTHER"))
        when (content.get("educationLevel")?.asText()) {
            "COLLEGE_OR_ABOVE" -> validateCollegeEducation(content)
            "HIGH_SCHOOL" -> validateHighSchoolEducation(content)
            "OTHER" -> validateOtherEducation(content)
        }
    }

    private fun validateCollegeEducation(content: JsonNode) {
        requireEnum(content, "universityType", setOf("BACHELORS", "ASSOCIATE", "MASTERS", "DOCTORATE"))
        requireNotBlank(content, "schoolName")
        requireNotBlank(content, "major")
        requireEnum(
            content,
            "graduationStatus",
            setOf("GRADUATED", "ENROLLED", "ON_LEAVE", "EXPECTED_GRADUATION", "DROPPED_OUT", "COMPLETED"),
        )
        requireDateFormat(content, "startDate", YYYY_MM_REGEX)
        requireDateFormat(content, "endDate", YYYY_MM_REGEX)
        requireOptionalEnum(content, "majorType", setOf("DOUBLE_MAJOR", "MINOR"))
    }

    private fun validateHighSchoolEducation(content: JsonNode) {
        val isGed = content.get("isGed")?.asBoolean() ?: false
        if (!isGed) {
            requireNotBlank(content, "schoolName")
            requireEnum(
                content,
                "graduationStatus",
                setOf("GRADUATED", "ENROLLED", "EXPECTED_GRADUATION", "DROPPED_OUT"),
            )
            requireOptionalEnum(
                content,
                "majorField",
                setOf("GENERAL", "SPECIALIZED_SCIENCE_FOREIGN", "VOCATIONAL_MEISTER"),
            )
        }
    }

    private fun validateOtherEducation(content: JsonNode) {
        requireNotBlank(content, "educationType")
        requireNotBlank(content, "institutionName")
        requireEnum(content, "completed", setOf("COMPLETED", "IN_PROGRESS", "DISCONTINUED"))
    }

    private fun validateCertificate(content: JsonNode) {
        requireEnum(content, "certificateType", setOf("AWARD_CONTEST", "CERTIFICATE", "LANGUAGE", "OTHER"))
        requireNotBlank(content, "name")
        requireDateFormat(content, "issuedDate", YYYY_MM_REGEX)
        requireNotBlank(content, "issuer")
        if (content.get("certificateType")?.asText() == "OTHER") {
            requireNotBlank(content, "category")
        }
    }

    private fun validateActivity(content: JsonNode) {
        requireEnum(
            content,
            "activityType",
            setOf("SCHOOL_ACTIVITY", "EXTERNAL_ACTIVITY", "INTERN", "EDUCATION_TRAINING", "OTHER"),
        )
        requireNotBlank(content, "activityName")
        requireNotBlank(content, "organizationName")
        requireDateFormat(content, "startDate", YYYY_MM_REGEX)
        requireDateFormat(content, "endDate", YYYY_MM_REGEX)
        requireNotBlank(content, "description")
    }

    private fun requireNotBlank(
        content: JsonNode,
        field: String
    ) {
        val value = content.get(field)
        if (value == null || value.isNull || value.asText().isBlank()) {
            throw BusinessException(ErrorCode.INVALID_BLOCK_CONTENT, "$field 필드가 필요합니다")
        }
    }

    private fun requireEnum(
        content: JsonNode,
        field: String,
        allowed: Set<String>
    ) {
        requireNotBlank(content, field)
        val value = content.get(field)!!.asText()
        if (value !in allowed) {
            throw BusinessException(ErrorCode.INVALID_BLOCK_CONTENT, "$field 필드의 값이 올바르지 않습니다: $value")
        }
    }

    private fun requireOptionalEnum(
        content: JsonNode,
        field: String,
        allowed: Set<String>
    ) {
        val valueNode = content.get(field) ?: return
        val value = valueNode.asText()
        if (!valueNode.isNull && value.isNotBlank() && value !in allowed) {
            throw BusinessException(ErrorCode.INVALID_BLOCK_CONTENT, "$field 필드의 값이 올바르지 않습니다: $value")
        }
    }

    private fun requireDateFormat(
        content: JsonNode,
        field: String,
        regex: Regex
    ) {
        requireNotBlank(content, field)
        val value = content.get(field)!!.asText()
        if (!value.matches(regex)) {
            throw BusinessException(ErrorCode.INVALID_BLOCK_CONTENT, "$field 필드의 날짜 형식이 올바르지 않습니다: $value")
        }
    }

    private fun requireEmail(
        content: JsonNode,
        field: String
    ) {
        requireNotBlank(content, field)
        val value = content.get(field)!!.asText()
        if (!value.contains("@")) {
            throw BusinessException(ErrorCode.INVALID_BLOCK_CONTENT, "$field 필드의 이메일 형식이 올바르지 않습니다")
        }
    }

    private fun requirePhoneNumber(
        content: JsonNode,
        field: String
    ) {
        requireNotBlank(content, field)
        val value = content.get(field)!!.asText()
        if (!value.matches(PHONE_REGEX)) {
            throw BusinessException(ErrorCode.INVALID_BLOCK_CONTENT, "$field 필드의 전화번호 형식이 올바르지 않습니다")
        }
    }

    private val YYYY_MM_REGEX = Regex("^\\d{4}\\.\\d{2}$")
    private val YYYY_MM_DD_REGEX = Regex("^\\d{4}\\.\\d{2}\\.\\d{2}$")
    private val PHONE_REGEX = Regex("^010-\\d{4}-\\d{4}$")
}
