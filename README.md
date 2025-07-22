# spring-template

Boilder Plate Code for Spring

### Project Structure
```
spring-template/
├── build.gradle.kts
├── settings.gradle.kts
└── src/
├── main/
│   ├── java/
│   │   └── ho.seong.cho/
│   │       ├── ServerApplication.java            # @SpringBootApplication
│   │       │
│   │       ├── oauth/                            # OAuth2 클라이언트 관련 코드
│   │       │   ├── data/                         # OAuth2 토큰 응답, 사용자 정보 등 DTO
│   │       │   ├── support/                      # AppleSecret, OIDC 유틸리티 등
│   │       │   ├── impl/                         # Apple, Google, Kakao 등 OAuth2 제공자별 구현체
│   │       │   ├── OAuth2Template.java           # OAuth2 인증 흐름을 추상화한 인터페이스
│   │       │   ├── OAuth2Properties.java         # OAuth2 연동 설정 정보
│   │       │   └── ...                           # Factory, AbstractTemplate...
│   │       │
│   │       ├── infra/                            # 외부 시스템과 통신하기 위한 어댑터 관련 코드
│   │       │   ├── aws/                          # AWS 서비스 연동 관련 패키지
│   │       │   │   ├── config/                   # AWS 설정 @Configuration, @ConfigurationProperties
│   │       │   │   ├── s3/                       # S3 처리 모듈
│   │       │   │   └── ses/                      # SES 처리 모듈
│   │       │   │   └── ...                       # 공통 서비스
│   │       │   ├── redis/                        # Redis Config, @RedisHash repositories
│   │       │   ├── kafka/                        # 
│   │       │   └── client/                       # 외부 클라이언트 모듈
│   │       │       ├── http/                     # OpenFeign 기반 HTTP 클라이언트 인터페이스
│   │       │       ├── sse/                      # Server-Sent-Event(SSE)
│   │       │       └── ...
│   │       │
│   │       ├── security/                         # Spring Security
│   │       │   └── ...
│   │       │
│   │       ├── validation/                       # Validation
│   │       │   ├── annotation
│   │       │   ├── impl                          # 구현체 
│   │       │   └── CustomConstraintValidator     # Validator 추상 클래스
│   │       └ ...
│   │
│   └── resources/
│       └── application.yml            # 기본 환경 설정
│
└── test/
    └── ...
```
