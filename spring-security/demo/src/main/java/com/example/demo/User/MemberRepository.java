package com.example.demo.User;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class MemberRepository {

    private static final Map<Long, Member> members = new ConcurrentHashMap<>();
    private static long sequene = 0L;

    public Member save(Member member) {
        member.setId(++sequene);
        members.put(member.getId(), member);
        return member;
    }

    public Member findById(Long id) {
        return members.get(id);
    }

    public Optional<Member> findByUsername(String username) {
        for (Member member : members.values()) {
            if (member.getUsername().equals(username)) {
                return Optional.ofNullable(member);
            }
        }
        return Optional.empty();
    }

    @PostConstruct
    public void addMember(){
        Member member = Member.builder()
                .id(1L)
                .username("username")
                .password("password")
                .nickname("nickname")
                .address("address")
                .phone("phone")
                .profileImg("null")
                .build();
        members.put(1L, member);

        members.get(1L).getRoles().add("admin");
        log.info(members.get(1L).getNickname());

        Member member2 = Member.builder()
                .id(2L)
                .username("username2")
                .password("password2")
                .nickname("nickname2")
                .address("address2")
                .phone("phone2")
                .profileImg("null2")
                .build();
        members.put(2L, member2);

        members.get(2L).getRoles().add("user");
        log.info(members.get(2L).getRoles().get(0));
    }
}
