# TROUBLE-002-JDBC-테이블-미생성-오류

## 문제 상황
* Spring Boot(`v4.0.1`) 애플리케이션의 데이터 저장소를 인메모리 방식에서 MySQL JDBC 방식으로 변경함.
* `src/main/resources/schema.sql` 경로에 DDL(테이블 생성 쿼리) 파일을 생성함.
* `application.yml`에 `spring.sql.init.mode: always` 설정을 추가하여 서버 시작 시 테이블 자동 생성을 의도함.
* 서버 실행 후 API 호출 시 `java.sql.SQLSyntaxErrorException: Table 'devtalk.session' doesn't exist` 에러가 발생함.
* 서버 시작 로그에 SQL 스크립트 실행을 알리는 `Executing SQL script...` 로그가 출력되지 않음.

## 가설
1. `schema.sql` 파일의 경로가 잘못되었거나 파일명이 틀렸을 것이다.
2. `application.yml`의 들여쓰기 오류로 인해 설정이 적용되지 않았을 것이다.
3. IDE(IntelliJ) 또는 빌드 도구(Gradle)가 리소스 파일을 빌드 결과물 폴더(`out` 또는 `build`)로 복사하지 않았을 것이다.
4. `schema-locations` 설정이 명시적으로 지정되지 않아 파일을 찾지 못했을 것이다.

## 시도
1. `src/main/resources` 폴더 내에 `schema.sql` 파일이 존재하는지 확인함.
2. `application.yml`에서 `sql` 설정이 `datasource`의 하위 속성으로 잘못 작성된 것을 발견하고, 동등한 레벨로 들여쓰기를 수정함.
3. `spring.sql.init.schema-locations: classpath:schema.sql` 설정을 추가하여 경로를 명시함.
4. `Gradle clean` 및 `Rebuild Project`를 수행하여 빌드 캐시를 초기화하고 재빌드함.

## 결과
1. 설정 파일 수정 및 명시적 경로 지정, 클린 빌드 후에도 서버 시작 로그에 `Executing SQL script...`가 출력되지 않음.
2. 여전히 API 호출 시 테이블이 없다는 에러(`Table 'devtalk.session' doesn't exist`)가 지속됨.

## 결론
* 현재 구성된 Spring Boot 4.0.1 환경에서 `application.yml` 설정을 통한 `schema.sql` 자동 실행이 정상적으로 동작하지 않음.
* 원인 파악에 시간을 더 소모하기보다 개발 진행을 우선하기 위해, **MySQL Workbench에서 DDL 쿼리를 직접 실행하여 테이블을 수동으로 생성함.**
* 이후 API 호출 시 정상적으로 데이터가 저장됨을 확인함.
