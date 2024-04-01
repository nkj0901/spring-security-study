package com.example.demo.controller;

import com.example.demo.User.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class MemberThymeleafController {

    private final MemberRepository memberRepository;

    /*
    * @AuthenticationPrincipal
    * 로그인 세션 정보 가지고 오기
    * Spring의 설정 빈 중에 UserDetailService가 반환한 UserDetails 객체가
    * SecurityContext에 저장되어 spirng에서 제공하는어노테이션을 이용하면 저장된 객체를 조회할 수 있다.
    */

//    @ResponseBody
//    @GetMapping("/user")
//    public ResponseEntity user(@AuthenticationPrincipal UserDetails userDetails) {
//        return new ResponseEntity(userDetails.getUsername(), HttpStatus.OK);
//    }

    @ResponseBody
    @GetMapping("/api1")
    public ResponseEntity api1() {
        return new ResponseEntity("api1 입니다.", HttpStatus.OK);
    }

    @ResponseBody
    @GetMapping("/api2")
    public ResponseEntity api2() {
        return new ResponseEntity<>("api2 입니다.", HttpStatus.OK);
    }

    @RequestMapping("/login")
    public String login(){
        return "login/index";
    }

    @RequestMapping("/user")
    public String user() {
        return "user/index";
    }

    @GetMapping("/join")
    public String join() {
        return "login/join";
    }

    @PostMapping("/join")
    public String postJoin() {
        return "login/index";
    }
}
