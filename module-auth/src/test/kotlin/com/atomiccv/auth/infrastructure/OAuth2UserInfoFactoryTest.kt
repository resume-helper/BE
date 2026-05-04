package com.atomiccv.auth.infrastructure

import com.atomiccv.auth.infrastructure.client.OAuth2UserInfoFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class OAuth2UserInfoFactoryTest {
    @Test
    fun `google registrationId로 GoogleOAuth2UserInfo를 생성한다`() {
        val attrs =
            mapOf(
                "sub" to "g-123",
                "email" to "a@gmail.com",
                "name" to "홍길동",
                "picture" to "https://photo",
            )
        val info = OAuth2UserInfoFactory.of("google", attrs)
        assertEquals("g-123", info.getId())
        assertEquals("a@gmail.com", info.getEmail())
        assertEquals("홍길동", info.getName())
        assertEquals("https://photo", info.getProfileImageUrl())
    }

    @Test
    fun `kakao registrationId로 KakaoOAuth2UserInfo를 생성한다`() {
        val attrs =
            mapOf(
                "id" to 99999L,
                "kakao_account" to
                    mapOf(
                        "email" to "b@kakao.com",
                        "profile" to mapOf("nickname" to "카카오유저", "profile_image_url" to "https://kakao-photo"),
                    ),
            )
        val info = OAuth2UserInfoFactory.of("kakao", attrs)
        assertEquals("99999", info.getId())
        assertEquals("b@kakao.com", info.getEmail())
        assertEquals("카카오유저", info.getName())
    }

    @Test
    fun `naver registrationId로 NaverOAuth2UserInfo를 생성한다`() {
        val attrs =
            mapOf(
                "response" to
                    mapOf(
                        "id" to "n-456",
                        "email" to "c@naver.com",
                        "name" to "네이버유저",
                        "profile_image" to "https://naver-photo",
                    ),
            )
        val info = OAuth2UserInfoFactory.of("naver", attrs)
        assertEquals("n-456", info.getId())
        assertEquals("c@naver.com", info.getEmail())
    }

    @Test
    fun `지원하지 않는 provider는 예외를 발생시킨다`() {
        assertThrows<IllegalArgumentException> {
            OAuth2UserInfoFactory.of("github", emptyMap())
        }
    }
}
