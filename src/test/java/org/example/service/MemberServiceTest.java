package org.example.service;

import org.example.dao.MemberRepository;
import org.example.domain.Member;
import org.example.domain.MemberAuth;
import org.example.security.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberServiceTest {

    @Test
    void registerNormalizesInputAndStoresPasswordAsHash() {
        PasswordHasher passwordHasher = new PasswordHasher();
        FakeMemberRepository repository = new FakeMemberRepository();
        MemberService service = new MemberService(repository, passwordHasher);

        long memberId = service.registerMember("  김회원  ", false, " Member.User ", "password1234");

        MemberAuth saved = repository.findAuthByLoginId("member.user").orElseThrow();
        assertEquals(memberId, saved.getMember().getId());
        assertEquals("김회원", saved.getMember().getName());
        assertEquals("member.user", saved.getMember().getLoginId());
        assertTrue(passwordHasher.matches("password1234", saved.getPasswordHash()));
        assertFalse(saved.getPasswordHash().contains("password1234"));
    }

    @Test
    void registerRejectsInvalidLoginId() {
        MemberService service = new MemberService(new FakeMemberRepository(), new PasswordHasher());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.registerMember("김회원", false, "bad id", "password1234")
        );
    }

    @Test
    void registerRejectsDuplicateLoginIdRegardlessOfLetterCase() {
        PasswordHasher passwordHasher = new PasswordHasher();
        FakeMemberRepository repository = new FakeMemberRepository();
        MemberService service = new MemberService(repository, passwordHasher);
        service.registerMember("첫 회원", false, "member01", "password1234");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.registerMember("둘째 회원", false, "MEMBER01", "password5678")
        );
    }

    @Test
    void loginReturnsMemberForCorrectPasswordAndRejectsWrongPassword() {
        PasswordHasher passwordHasher = new PasswordHasher();
        FakeMemberRepository repository = new FakeMemberRepository();
        MemberService service = new MemberService(repository, passwordHasher);
        service.registerMember("김회원", true, "member01", "password1234");

        Member member = service.login(" MEMBER01 ", "password1234");

        assertEquals("member01", member.getLoginId());
        assertThrows(
                IllegalArgumentException.class,
                () -> service.login("member01", "wrong-password")
        );
    }

    private static final class FakeMemberRepository implements MemberRepository {
        private final Map<Long, Member> members = new HashMap<>();
        private final Map<Long, String> passwordHashes = new HashMap<>();
        private long nextId = 1;

        @Override
        public List<Member> findAll() {
            return new ArrayList<>(members.values());
        }

        @Override
        public int save(Member member, String passwordHash) {
            long memberId = nextId++;
            Member saved = new Member(memberId, member.getName(), member.isLicenseStatus(), member.getLoginId());
            members.put(memberId, saved);
            passwordHashes.put(memberId, passwordHash);
            return Math.toIntExact(memberId);
        }

        @Override
        public void updatePasswordHash(long memberId, String passwordHash) {
            passwordHashes.put(memberId, passwordHash);
        }

        @Override
        public int updateLicenseStatus(long memberId, boolean licenseStatus) {
            Member member = members.get(memberId);
            if (member == null) {
                return 0;
            }
            members.put(memberId, new Member(memberId, member.getName(), licenseStatus, member.getLoginId()));
            return 1;
        }

        @Override
        public Optional<Member> findById(long memberId) {
            return Optional.ofNullable(members.get(memberId));
        }

        @Override
        public Optional<MemberAuth> findAuthByLoginId(String loginId) {
            return members.values().stream()
                    .filter(member -> member.getLoginId().equals(loginId.toLowerCase(Locale.ROOT)))
                    .findFirst()
                    .map(member -> new MemberAuth(member, passwordHashes.get(member.getId())));
        }
    }
}
