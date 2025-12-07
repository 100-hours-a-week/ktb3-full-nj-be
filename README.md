# 🕺 C.Groove - Dance Community Platform (BE)

> 대학씬 댄서들을 위한 커뮤니티 및 행사 관리 플랫폼

각 대학 댄스 동아리와 씬에서 활동하는 댄서들이 팀(Crew)을 만들고, 행사(워크샵/배틀) 정보를 쉽게 공유할 수 있는 플랫폼입니다.

[![Coverage](https://img.shields.io/badge/coverage-88%25-brightgreen)](링크)
[![Tests](https://img.shields.io/badge/tests-186%20passed-success)](링크)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-green)](https://spring.io/)

---

## 💡 프로젝트 소개

대학 댄스 동아리 활동을 하면서 가장 불편했던 점은 **흩어져 있는 정보**와 **비효율적인 운영**이었습니다.
행사 신청은 구글 폼으로, 공지는 단톡방으로, 홍보는 인스타그램으로 흩어져 있다 보니 참여율을 관리하기 힘들었고, 크루원 관리는 수기 명부에 의존해야 했습니다.

이러한 비효율을 해결하고, **댄서들이 춤에만 집중할 수 있는 환경**을 만들고자 이 프로젝트를 시작했습니다.

---

## 🛠 Tech Stack

- **Java 21, Spring Boot 3.x**: 최신 LTS 버전을 사용하여 안정적인 백엔드 환경 구축
- **Spring Security + JWT**: 확장성을 고려한 Stateless 인증 방식 채택
- **Spring Data JPA + QueryDSL**: 복잡한 검색 조건과 성능 최적화를 위한 Type-Safe 쿼리 작성
- **MySQL, H2**: 프로덕션 및 테스트 데이터베이스
- **JUnit5, Mockito**: 비즈니스 로직의 신뢰성 확보를 위한 테스트 코드 작성

---

## 📂 ERD (Entity Relationship Diagram)
![dance-community-v2 (1).png](../../../Downloads/dance-community-v2%20%281%29.png)
<img width="2030" height="1412" alt="dance-community-v2 (1)" src="https://github.com/user-attachments/assets/5133e3dc-e4a3-489e-856c-1ab14d4947b9" />

---

## 🌟 주요 기능 구현

### 1. 🔐 안전하고 편리한 인증 (Security)
- **보안 강화**: XSS 공격에 대비해 **Access Token은 JSON Body**로, 탈취 위험이 높은 **Refresh Token은 HttpOnly Cookie**로 분리하여 저장했습니다.
- **명확한 예외 처리**: 인증 실패(`401`)와 권한 부족(`403`)을 명확히 구분하여 프론트엔드에서 적절한 UX(로그인 창 이동 vs 경고 메시지)를 제공하도록 설계했습니다.

### 2. 💃 체계적인 클럽 시스템
- **권한 계층화**: 단순한 멤버 관리를 넘어 `LEADER`(생성자), `MANAGER`, `MEMBER`로 권한을 세분화하여 실제 동아리 운영진 시스템을 반영했습니다.
- **가입 승인제**: 무분별한 가입을 막기 위해 승인/거절 프로세스를 도입했습니다.

### 3. 🎉 실시간 행사 관리
- **동시성 이슈 고려**: 행사 정원(Capacity)을 설정하고, 신청 인원이 꽉 차면 자동으로 마감되도록 구현했습니다.
- **데이터 보존**: 실수로 행사를 삭제하더라도 복구할 수 있도록 `Soft Delete`(@SQLDelete)를 적용했습니다.

---

## ⚡ 기술적 고민과 해결 (Performance & Refactoring)

### JPA N+1 문제 해결 (QueryDSL)

**문제 상황**
게시글 목록을 조회할 때, 각 게시글의 작성자(User)와 소속 클럽(Club) 정보를 가져오기 위해 게시글 개수만큼의 추가 조회 쿼리가 발생하는 **N+1 문제**를 확인했습니다.

**해결**
단순 JPA 메서드 대신 **QueryDSL의 `fetchJoin()`**을 도입했습니다. 연관된 엔티티를 한 번의 쿼리로 함께 조회(`Join`)하도록 변경하여, 불필요한 DB 접근 횟수를 획기적으로 줄였습니다.

순환 참조 문제 해결
문제 상황 ClubService와 EventService가 서로를 참조(Autowired)하면서 순환 참조가 발생하여 애플리케이션이 실행되지 않는 문제가 있었습니다.

해결 공통적으로 사용되는 권한 검증 로직을 ClubAuthService라는 별도의 도메인 서비스로 분리했습니다. 이를 통해 의존성 방향을 단방향으로 정리하고, 코드의 재사용성도 높였습니다.

---

## 🧪 테스트 커버리지

<img width="1111" height="223" alt="스크린샷 2025-12-07 오후 10 07 00" src="https://github.com/user-attachments/assets/738b2af1-abae-4daf-979d-14e532c6985e" />

*전체 라인 커버리지: **88%** / 브랜치 커버리지: **68%***

```bash
# 테스트 실행 + 커버리지 확인
./gradlew test jacocoTestReport
```

---

## 📚 트러블슈팅 하이라이트

### 1. JWT XSS 취약점 개선
**문제**: localStorage에 토큰 저장 → XSS 공격 가능  
**해결**: Refresh Token을 HttpOnly Cookie로 이동 + Access Token은 1시간 짧은 만료

### 2. 순환 참조 문제
**문제**: `ClubService` ↔ `EventService` 양방향 의존  
**해결**: 공통 로직을 `ClubValidationService`로 분리 → 단방향 의존성 확립

---

## 🚀 실행 방법

```
# 1. 프로젝트 클론
git clone [https://github.com/100-hours-a-week/ktb3-full-nj-be](https://github.com/100-hours-a-week/ktb3-full-nj-be)

  # 2. 빌드
./gradlew build

  # 3. 실행
./gradlew bootRun
```


---

## 📡 주요 API

> 전체 API 문서: [Swagger UI](http://localhost:8080/swagger-ui/index.html)

---

## 🎥 시연 영상 시나리오
[![C.Groove 시연 영상](https://img.youtube.com/vi/tm8GrfF0A9Y/0.jpg)](https://www.youtube.com/watch?v=tm8GrfF0A9Y)

---

## 🚧 향후 계획

- [ ] 댓글 기능
- [ ] 실시간 알림 (WebSocket)
- [ ] Redis 캐싱 전략
- [ ] Docker 컨테이너화 + CI/CD
- [ ] AWS 배포 (EC2 + RDS)

---

## 👨‍💻 개발자

**NJ** (남진)  
GitHub: [@JINOJ37](https://github.com/JINOJ37)  
Email: jinoj0423@gmail.com

---

<div align="center">

**Made with ❤️ for University Street Dancers**

</div>
