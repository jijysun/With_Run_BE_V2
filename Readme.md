# 같이, 달려갈개 ! : 지역 기반 반려견 산책 메이트 [ BE ]

![1.png](./assets/project/1.png)

## 🚀 프로젝트 소개

![2.png](./assets/project/2.png)

- [같이, 달려갈개!] 는 반려견 산책 친구 및 산책 코스 추천을 통해 반려견과 더 즐거운 시간을 보낼 수 있도록 돕는 어플리케이션입니다.

### 🌟 핵심 기능

![3.png](./assets/project/3.png)

- 국토교통부 자료를 통한 지역 변경 시스템 구현
- 산책 스타일이 비슷한 반려견과 매칭
- Redis Pub/Sub을 활용한 타 사용자와 실시간 채팅 서비스
- DTO Projection 과 페이징을 활용한 조회 로직 최적화

![4.png](./assets/project/4.png)

- 온보딩 시 저장된 산책 스타일을 기반으로 산책 코스 추천
- 사용자 지정 핀을 통한 산책 코스 저장 및 제작 기능
- 사용자 지정 핀과 최적 경로에 대한 효율적인 저장

![5.png](./assets/project/5.png)

- 공공데이터포털의 CSV 데이터를 사용하여 파싱 및 활용

## 🧱 프로젝트 구조 및 스택

### 🛰️ 서버 아키텍쳐

![architecture.png](./assets/project/architecture.png)

### ✨ 사용 기술 스택

![Java](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring Data Redis](https://img.shields.io/badge/Spring%20Data%20Redis-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white)

![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

![Amazon EC2](https://img.shields.io/badge/Amazon%20EC2-FF9900?style=for-the-badge&logo=amazon-ec2&logoColor=white)
![Amazon RDS](https://img.shields.io/badge/Amazon%20RDS-527FFF?style=for-the-badge&logo=amazon-rds&logoColor=white)
![Amazon S3](https://img.shields.io/badge/Amazon%20S3-569A31?style=for-the-badge&logo=amazon-s3&logoColor=white)

![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ%20IDEA-000000?style=for-the-badge&logo=intellij-idea&logoColor=white)
![k6](https://img.shields.io/badge/k6-8C59C3?style=for-the-badge&logo=k6&logoColor=white)
![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)
![Postman](https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white)
![Notion](https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white)

### 🧑‍💻 팀원 소개 (BE Team)

| [김석현 (TL)](https://github.com/jijysun) | [황서진](https://github.com/HwangSeo) | [문지현](https://github.com/dxxrjh) | [장예린](https://github.com/yelin1197) | [김수민](https://github.com/sooominie) |
|:--------------------------------------:|:----------------------------------:|:--------------------------------:|:-----------------------------------:|:-----------------------------------:|
|               **채팅 서비스**               |        **사용자, 온보딩, 마이페이지**         |         **친구, 산책코스, 공통**         |          **지도, 반려견 동반 시설**          |          **지도, 반려견 동반 시설**          |