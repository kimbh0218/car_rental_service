package org.example.dao;

import org.example.domain.Member;
import org.example.domain.MemberAuth;

import java.util.List;
import java.util.Optional;

/** 회원 서비스가 필요한 저장소 동작만 정의합니다. */
public interface MemberRepository {
    List<Member> findAll();

    int save(Member member, String passwordHash);

    void updatePasswordHash(long memberId, String passwordHash);

    int updateLicenseStatus(long id, boolean licenseStatus);

    Optional<Member> findById(long id);

    Optional<MemberAuth> findAuthByLoginId(String loginId);
}
