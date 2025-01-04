# 생성형 AI 활용 ASMR 서비스 Hear Here 백엔드 레포지토리

## 🎧 Stacks

### Language & Framework
![Static Badge](https://img.shields.io/badge/java-%235382a1?style=for-the-badge&logoColor=white)
![Static Badge](https://img.shields.io/badge/spring%20boot-%236DB33F?style=for-the-badge&logo=springboot&logoColor=white)

### RDBMS
![Static Badge](https://img.shields.io/badge/mysql-%234479A1?style=for-the-badge&logo=mysql&logoColor=white) 

### Build
![Static Badge](https://img.shields.io/badge/gradle-%2302303A?style=for-the-badge&logo=gradle&logoColor=white)

### Deploy
![Static Badge](https://img.shields.io/badge/amazon%20ec2-%23FF9900?style=for-the-badge&logo=amazonec2&logoColor=white)
![Static Badge](https://img.shields.io/badge/amazon%20rds-%23527FFF?style=for-the-badge&logo=amazonrds&logoColor=white) ![Static Badge](https://img.shields.io/badge/amazon%20s3-%23569A31?style=for-the-badge&logo=amazons3&logoColor=white)
![Static Badge](https://img.shields.io/badge/amazon%20route%2053-%238C4FFF?style=for-the-badge&logo=amazonroute53&logoColor=white)


## 🎧 Folder Structure
```
.
└── src/main/
    ├── java/com/example/hearhere/
    │   ├── common/
    │   │   ├── code # API 응답 통일
    │   │   ├── status # API 응답 통일
    │   │   └── parser 
    │   ├── security/
    │   │   ├── jwt 
    │   │   └── oauth2 # 소셜 로그인 
    │   ├── controller
    │   ├── service
    │   ├── repository
    │   ├── entity
    │   └── dto
    └── resources/
        └── application.properties
```

## 🎹 Commit Convention
``` 
<타입> : <제목>
ex) feat: 로그인 기능 추가

1. feat : 새로운 기능 추가
2. fix : 버그 수정
3. docs : 문서 수정
4. test : 테스트 코드 추가
5. refactor : 코드 리팩토링, 파일 혹은 폴더명을 수정하거나 옮기는 작업만인 경우, 파일을 삭제하는 작업만 수행한 경우
6. chore : 코드 외 빌드 부분 혹은 패키지 매니저 수정사항

타입은 소문자로 고정
```
