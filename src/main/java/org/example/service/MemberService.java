package org.example.service;

import org.example.dao.MemberDao;
import org.example.dao.DuplicateLoginIdException;
import org.example.domain.Member;
import org.example.domain.MemberAuth;
import org.example.security.PasswordHasher;

import java.util.List;
import java.util.Locale;

public class MemberService {

    private final MemberDao memberDao;
    private final PasswordHasher passwordHasher;

    public MemberService(MemberDao memberDao, PasswordHasher passwordHasher) {
        this.memberDao = memberDao;
        this.passwordHasher = passwordHasher;
    }

    public long registerMember(String name, boolean licenseStatus, String loginId, String rawPassword) {
        String normalizedName = validateAndNormalizeName(name);
        String normalizedLoginId = validateAndNormalizeLoginId(loginId);

        if (memberDao.findAuthByLoginId(normalizedLoginId).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 로그인 ID입니다.");
        }

        Member member = new Member(normalizedName, licenseStatus, normalizedLoginId);
        String passwordHash = passwordHasher.hash(rawPassword);

        try {
            return memberDao.save(member, passwordHash);
        } catch (DuplicateLoginIdException e) {
            throw new IllegalArgumentException("이미 사용 중인 로그인 ID입니다.", e);
        }
    }

    public Member login(String loginId, String rawPassword) {
        String normalizedLoginId = normalizeLoginId(loginId);
        if (!isValidLoginId(normalizedLoginId) || !passwordHasher.isValidRawPassword(rawPassword)) {
            throw invalidCredentials();
        }

        MemberAuth memberAuth = memberDao.findAuthByLoginId(normalizedLoginId)
                .orElseThrow(() -> new IllegalArgumentException("로그인 ID 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordHasher.matches(rawPassword, memberAuth.getPasswordHash())) {
            throw new IllegalArgumentException("로그인 ID 또는 비밀번호가 올바르지 않습니다.");
        }

        if (passwordHasher.needsRehash(memberAuth.getPasswordHash())) {
            memberDao.updatePasswordHash(memberAuth.getMember().getId(), passwordHasher.hash(rawPassword));
        }

        return memberAuth.getMember();
    }

    public List<Member> findAllMembers() {
        return memberDao.findAll();
    }

    public Member findMemberById(long memberId) {
        if (memberId <= 0) {
            throw new IllegalArgumentException("회원 ID는 양수여야 합니다.");
        }

        return memberDao.findById(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 회원입니다.")
                );
    }

    public void registerLicense(long memberId) {
        if (memberId <= 0) {
            throw new IllegalArgumentException("회원 ID는 양수여야 합니다.");
        }

        if (memberDao.updateLicenseStatus(memberId, true) != 1) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다.");
        }
    }

    private String validateAndNormalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("회원명은 필수입니다.");
        }

        String normalizedName = name.strip();
        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException("회원명은 100자 이하여야 합니다.");
        }
        return normalizedName;
    }

    private String validateAndNormalizeLoginId(String loginId) {
        String normalizedLoginId = normalizeLoginId(loginId);
        if (!isValidLoginId(normalizedLoginId)) {
            throw new IllegalArgumentException("로그인 ID는 영문, 숫자, '.', '_', '-' 4~50자여야 합니다.");
        }
        return normalizedLoginId;
    }

    private String normalizeLoginId(String loginId) {
        return loginId == null ? null : loginId.strip().toLowerCase(Locale.ROOT);
    }

    private boolean isValidLoginId(String loginId) {
        return loginId != null && loginId.matches("[a-zA-Z0-9._-]{4,50}");
    }

    private IllegalArgumentException invalidCredentials() {
        return new IllegalArgumentException("로그인 ID 또는 비밀번호가 올바르지 않습니다.");
    }
}
