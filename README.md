## 🏗️ 파일 구조 및 역할 (Project File Tree)

이 프로젝트는 이클립스 기반의 Maven/Servlet 구조로 구성되어 있으며, `src/main/java`가 백엔드를, `src/main/webapp`이 프론트엔드를 담당합니다.

```
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/admin/ ------------------- [ Java Servlet (백엔드 핵심) ]
│   │   │       ├── AuthFilter.java               # [보안] 모든 API 요청의 JWT 유효성 검증
│   │   │       ├── DBUtil.java                   # [DB] MySQL 연결 유틸리티 및 설정값
│   │   │       ├── LoginServlet.java             # [인증] 로그인 처리 및 JWT 토큰 발급
│   │   │       ├── RefreshTokenServlet.java      # [인증] JWT 토큰 갱신 로직
│   │   │       ├── MealTicketPaymentServlet.java # [API] POS 기기 결제 내역 조회 (식권)
│   │   │       └── PointTransactionServlet.java  # [API] QR 식권 발급 내역 조회
│   │   └── webapp/ ------------------------------- [ 웹 리소스 (프론트엔드) ]
│   │       ├── 404.html                          # [시스템] 커스텀 404 에러 페이지 (CSS 내장)
│   │       ├── images/                           # 로고 및 파비콘 이미지 폴더
│   │       ├── admin.html                        # [메인] 관리자 페이지 최상위 레이아웃
│   │       ├── header.html                       # [컴포넌트] 상단 헤더 (토큰 타이머, 로그아웃)
│   │       ├── sidebar.html                      # [컴포넌트] 좌측 메뉴바 (뷰 네비게이션)
│   │       ├── login.html                        # [인증] 관리자 로그인 폼
│   │       ├── login.js                          # [로직] 로그인 및 reCAPTCHA 처리
│   │       ├── admin.js                          # [로직] SPA 컨트롤러 (화면 로딩, 토큰 검사)
│   │       ├── point_history.html                # [뷰] 포인트/QR 발급 내역 테이블 뷰
│   │       ├── point_transactions.js             # [로직] point_history 뷰 데이터 호출 및 가공
│   │       ├── meal_ticket_history.html          # [뷰] 식권 결제 내역 테이블 및 Chart.js 통계
│   │       ├── payment.js                        # [로직] 식권 결제 데이터 호출 및 가공
│   │       ├── style.css                         # 커스텀 CSS (스크롤바, 탭, 페이지네이션 스타일)
│   │       ├── output.css                        # Tailwind CSS 빌드 결과물 (서버 배포용)
│   │       ├── check-security-code.html          # [유틸] 보안 코드 확인 폼
│   │       └── create-admin.html                 # [유틸] 새 관리자 계정 생성 폼
└── pom.xml --------------------------------------- [ Maven 프로젝트 의존성 관리 파일 ]
```

---

## 🛠️ 기술 스택 (Tech Stack)

| 구분 | 기술 스택 (Stack) | 용도 및 상세 설명 |
|------|------------------|------------------|
| 백엔드 핵심 | Java Servlet (Jakarta EE) | API 엔드포인트 구현 및 비즈니스 로직 처리 |
| 인증/보안 | JWT (jjwt 라이브러리) | 무상태(Stateless) 기반의 관리자 인증 토큰 발급/검증 및 권한 관리 |
| 암호화 | BCrypt, SHA-256 | 관리자 비밀번호 저장 및 검증 (하이브리드 지원) |
| 시각화 | Chart.js | 관리자 대시보드의 통계(일별 매출, 비율) 차트 구현 |
| 스타일링 | Tailwind CSS (CLI Build) | CSS 프레임워크를 이용한 반응형 UI 구현 및 성능 최적화 |
| DB/WAS | MySQL, Apache Tomcat (62215) | 데이터 저장 및 Java 애플리케이션 서비스 |

---

## 📁 백엔드 핵심 컴포넌트 분석 (`/src/main/java/com/example/admin`)

모든 백엔드 로직은 `/api/*` 엔드포인트로 구현되어 있으며, JWT 토큰 검증 필터(`AuthFilter`)를 통과해야 접근 가능합니다.

| 파일명 | 엔드포인트 | 주요 기능 및 특징 |
|--------|-----------|------------------|
| LoginServlet.java | `/api/admin/login` | 로그인 및 JWT 발급. 비밀번호를 BCrypt/SHA-256 해시로 검증하며, 성공 시 1시간 유효 토큰을 JSON으로 응답합니다. |
| AuthFilter.java | `/api/*` | API 게이트웨이 역할. 로그인 및 토큰 갱신을 제외한 모든 요청에 대해 `Authorization: Bearer` 헤더의 JWT 유효성을 검사합니다. |
| RefreshTokenServlet.java | `/api/admin/refresh-token` | 세션 연장. 만료가 임박한 토큰을 받아 새로운 JWT 토큰을 재발급하여 세션 연속성을 유지합니다. |
| MealTicketPaymentServlet.java | `/api/meal-ticket-payments` | POS 기기 결제 내역(`pos_payments`)을 조회하여 JSON 리스트로 반환합니다. |
| PointTransactionServlet.java | `/api/point-transactions` | QR 식권 발급 내역(`qr_issued_tokens`)을 사용자 이름과 함께 조회하여 반환합니다. |
| DBUtil.java (유틸리티) | - | MySQL DB 연결 정보를 포함하며, `DB_USER`와 `DB_PASSWORD`가 하드코딩되어 있습니다. |

---

## 🎨 프론트엔드 컴포넌트 분석 (`/src/main/webapp`)

프론트엔드는 SPA(Single Page Application) 방식을 모방하며, `admin.js`가 주요 뷰를 동적으로 로드합니다.

### 1. 메인 레이아웃 및 제어

| 파일명 | 역할 및 특징 |
|--------|-------------|
| admin.html | 최상위 뼈대. Header, Sidebar, Content 영역을 정의하는 메인 레이아웃 파일입니다. |
| admin.js | 대시보드 컨트롤러. (1) `checkTokenValidity`로 토큰 검증 및 자동 갱신, (2) `loadComponent`로 하위 뷰 로드, (3) `MapsTo` 함수를 통해 화면 전환을 처리하는 핵심 로직입니다. |
| header.html | 상단 네비게이션바 UI. '토큰 남은 시간 타이머', '시간 갱신', '로그아웃' 버튼이 포함되어 있습니다. |
| sidebar.html | 좌측 사이드바 UI. `admin.js`의 `MapsTo` 함수와 연동하여 '포인트 충전 내역'과 '식권 결제 내역' 뷰로 이동하는 메뉴를 제공합니다. |
| style.css | Tailwind CSS로 처리하기 힘든 커스텀 CSS 정의. 스크롤바 디자인, 탭/필터/페이지네이션 버튼 스타일, 사이드바 활성화 스타일 등을 정의합니다. |

### 2. 데이터 뷰 및 로직

| 파일명 | 엔드포인트 | 역할 및 특징 |
|--------|-----------|-------------|
| point_transactions.js | `/api/point-transactions` | 포인트 내역 API 호출 및 데이터 가공. 서버에서 받은 데이터를 날짜/시간/상태 텍스트 등으로 변환하여 `point_history.html`에 전달합니다. |
| point_history.html (뷰) | - | 포인트 충전/환불 내역 뷰. 탭 필터(충전/환불), 기간 검색, 이름/사번 검색, 페이지네이션 기능이 구현된 테이블을 렌더링합니다. |
| payment.js | `/api/meal-ticket-payments` | 식권 결제 데이터 가공 모듈. 결제 시간을 분리하고, 메뉴 이름에서 식권 개수를 추출하는 등 UI에 맞게 데이터를 변환합니다. |
| meal_ticket_history.html (복합 뷰) | - | 식권 결제 내역 뷰. 월별/식당별 필터, 결제 수단별 탭을 제공하며, Chart.js를 이용한 통계 차트(비율, 일별 금액)를 동적으로 생성합니다. |

### 3. 기타 페이지

| 파일명 | 역할 및 특징 |
|--------|-------------|
| login.html | reCAPTCHA가 적용된 관리자 로그인 폼 UI. |
| login.js | reCAPTCHA 토큰 발급 및 `/api/admin/login` 요청 로직. |
| totp-setup.html | TOTP (2단계 인증) 등록 폼. 관리자 아이디를 입력받아 QR코드와 시크릿 코드를 화면에 표시합니다. |
| 404.html | 서버 보안을 위해 CSS가 내장된 커스텀 에러 페이지. |
| create-admin.html (선택적) | 새로운 관리자 계정을 생성하는 폼 UI. |

---
