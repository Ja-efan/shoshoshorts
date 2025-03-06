# 🚀 Project BTA

## 📌 프로젝트 개요

이 프로젝트는 **📜 대본 형식의 텍스트를 입력받아 음성 파일로 출력**하는 프론트엔드 애플리케이션입니다.

## 🛠 기술 스택

| 기술 | 설명 |
|------|------|
| ![React](https://img.shields.io/badge/React-18.3-blue?logo=react) | Vite 기반의 React 18.3 |
| ![TypeScript](https://img.shields.io/badge/TypeScript-✔-blue?logo=typescript) | 정적 타입 언어 |
| ![Redux](https://img.shields.io/badge/Redux-✔-purple?logo=redux) | 상태 관리 |
| ![TailwindCSS](https://img.shields.io/badge/TailwindCSS-✔-teal?logo=tailwindcss) | 스타일링 |
| ![ESLint](https://img.shields.io/badge/ESLint-✔-yellow?logo=eslint) | 코드 품질 관리 |
| ![Node.js](https://img.shields.io/badge/Node.js-22.12.0-green?logo=node.js) | 런타임 환경 |
| ![npm](https://img.shields.io/badge/npm-10.9.0-red?logo=npm) | 패키지 매니저 |

## 📂 프로젝트 구조

```
📦 프로젝트 루트
├── 📂 src
│   ├── 📂 assets         # 🎨 정적 파일 (이미지, 폰트 등)
│   ├── 📂 components     # 🧩 재사용 가능한 UI 컴포넌트
│   ├── 📂 pages          # 📄 주요 페이지 컴포넌트
│   ├── 📂 store          # 🗄 Redux 상태 관리 관련 파일
│   ├── 📂 hooks          # 🔗 커스텀 훅
│   ├── 📂 utils          # 🛠 유틸리티 함수 모음
│   ├── 📂 styles         # 🎨 Tailwind 관련 스타일 파일
│   ├── 📂 api            # 🔗 백엔드 API 요청 관련 함수
│   ├── 📂 router         # 🚦 React Router 관련 파일
│   ├── 📜 main.tsx       # 🚀 애플리케이션 진입점
│   ├── 📜 App.tsx        # 🏠 루트 컴포넌트
├── 📜 index.html         # 📝 기본 HTML 파일
├── 📜 package.json       # 📦 패키지 정보 및 스크립트
├── 📜 tsconfig.json      # ⚙ TypeScript 설정 파일
├── 📜 vite.config.ts     # ⚡ Vite 설정 파일
├── 📜 eslint.config.js   # 🛠 ESLint 설정 파일
└── ... 기타 설정 파일
```

## 🚀 설치 및 실행 방법

### 1️⃣ 프로젝트 클론
```sh
git clone https://lab.ssafy.com/s12-ai-speech-sub1/S12P21B106.git
cd FE
```

### 2️⃣ 패키지 설치
```sh
npm install
```

### 3️⃣ 개발 서버 실행
```sh
npm run dev
```

## 🔧 ESLint 설정 확장

타입 체크를 활성화하려면 `eslint.config.js` 파일을 아래와 같이 설정합니다:

```js
import react from 'eslint-plugin-react';
export default tseslint.config({
  languageOptions: {
    parserOptions: {
      project: ['./tsconfig.node.json', './tsconfig.app.json'],
      tsconfigRootDir: import.meta.dirname,
    },
  },
  settings: { react: { version: '18.3' } },
  plugins: { react },
  rules: {
    ...react.configs.recommended.rules,
    ...react.configs['jsx-runtime'].rules,
  },
});
```

## ✅ TODO
- [ ] 🎤 음성 변환 기능 구현
- [ ] 🔗 백엔드 API 연동
- [ ] 🎨 UI 디자인 및 스타일링

---
이 문서는 프로젝트 진행에 따라 업데이트될 수 있습니다! 🚀
