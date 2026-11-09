# 프로젝트 작업 규칙

- 사용자 요청: 수정사항이 포함된 새 배포본을 만들 때마다 `app/build.gradle`의 `versionName`을 올리고 `versionCode`를 증가시킨다. 동일 수정본의 검증용 재빌드는 버전을 추가로 올리지 않는다.
- 작은 수정은 패치 버전, 기능 추가는 마이너 버전을 올린다.
- 정식 서명 APK를 `dist/MinuteVoice_v<versionName>.apk`로 내보내고 README의 현재 버전 및 다운로드 경로도 갱신한다.
- 기존 배포용 서명 키를 유지하며 키와 비밀번호는 Git에 추가하거나 출력하지 않는다.
