import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. 부하 시나리오 설정
export const options = {
  stages: [
    { duration: '10s', target: 10 }, // 10초 동안 사용자 10명까지 늘림
    { duration: '20s', target: 10 }, // 20초 동안 10명 유지
    { duration: '10s', target: 0 },  // 10초 동안 0명으로 줄임
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% 요청이 0.5초 안에 끝나야 함
  },
};

const BASE_URL = 'http://localhost:8082'; // ⚠️ 포트 8081 확인

export default function () {
  // --- [A] 로그인 ---
  const loginRes = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
    email: 't1@naver.com', // 아까 만든 계정
    password: 'Q!q1qqqq',
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  // 로그인 실패 시 에러 출력하고 중단
  if (loginRes.status !== 200) {
    console.error(`로그인 실패: ${loginRes.status} ${loginRes.body}`);
    return;
  }

  // 토큰 꺼내기
  const accessToken = loginRes.json().data.accessToken;

  // --- [B] 클럽 목록 조회 (QueryDSL 테스트) ---
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`, // 토큰 장착
    },
  };

  const listRes = http.get(`${BASE_URL}/clubs`, params);

  check(listRes, {
    'status is 200': (r) => r.status === 200, // 성공했는지 체크
  });

  sleep(1); // 유저가 1초 정도 본다고 가정
}