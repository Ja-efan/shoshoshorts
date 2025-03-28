import { Link, useNavigate } from "react-router-dom"
import shortLogo from "@/assets/short_logo.png"

export default function TermsPage() {
  const navigate = useNavigate()

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-10 bg-white border-b">
        <div className="container flex h-16 items-center px-4">
          <button 
            onClick={() => navigate(-1)}
            className="mr-4 text-gray-600 hover:text-gray-900"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <Link to="/" className="flex items-center gap-2">
            <img src={shortLogo} alt="쇼쇼숓 로고" className="h-8 w-8" />
            <span className="text-xl font-bold">쇼쇼숓</span>
          </Link>
        </div>
      </header>

      <main className="flex-1 py-8">
        <div className="container mx-auto px-4 max-w-3xl">
          <h1 className="text-3xl font-bold mb-8">이용약관</h1>
          
          <div className="space-y-6 text-gray-700">
            <section>
              <h2 className="text-xl font-semibold mb-4">제1조 (목적)</h2>
              <p>
                본 약관은 쇼쇼숏 운영팀(이하 "운영팀")이 제공하는 AI 기반 유튜브 쇼츠 생성 서비스(이하 "서비스")의 이용조건 및 절차, 운영팀과 이용자의 권리·의무 및 책임사항을 규정하는 것을 목적으로 합니다.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold mb-4">제2조 (정의)</h2>
              <p>
                <strong>"서비스"</strong>란, 이용자가 작성한 스토리(썰)를 AI가 분석하여 자동으로 유튜브 쇼츠 영상을 생성하고, 저장 및 공유할 수 있도록 지원하는 기능을 포함합니다.<br /><br />
                <strong>"이용자"</strong>란, 쇼쇼숏 서비스를 이용하는 모든 사람을 의미합니다.<br /><br />
                <strong>"콘텐츠"</strong>란, 이용자가 서비스 내에서 생성하는 텍스트, 영상, 이미지, 음성 및 기타 데이터를 의미합니다.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold mb-4">제3조 (서비스의 제공 및 변경)</h2>
              <p>
                1. 운영팀은 다음과 같은 서비스를 제공합니다:<br />
                - AI 기반 유튜브 쇼츠 자동 생성<br />
                - 영상 다운로드 및 저장 기능 제공<br />
                - 유튜브 업로드 지원 (이용자의 계정과 연동 가능)<br />
                - 콘텐츠 공유 및 관리 서비스 제공<br /><br />
                2. 운영팀은 서비스의 품질 향상을 위해 필요 시 서비스 내용을 변경할 수 있습니다.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold mb-4">제4조 (이용자의 권리 및 의무)</h2>
              <p>
                1. 이용자는 서비스를 활용하여 자신의 썰을 기반으로 영상을 생성하고, 이를 다운로드하거나 유튜브에 업로드할 수 있습니다.<br /><br />
                2. 이용자는 서비스를 이용함에 있어 다음 행위를 해서는 안 됩니다:<br />
                - 타인의 저작물을 무단 사용하여 콘텐츠를 생성하는 행위<br />
                - 서비스의 정상적인 운영을 방해하는 행위 (예: 자동화된 프로그램으로 무단 요청)<br />
                - 운영팀이 제공하는 서비스를 무단으로 복제, 수정, 배포하는 행위<br /><br />
                3. 이용자는 관련 법령 및 본 약관을 준수해야 합니다.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold mb-4">제5조 (서비스 이용 제한 및 계정 관리)</h2>
              <p>
                1. 운영팀은 이용자가 본 약관을 위반하거나 서비스 운영을 방해하는 경우, 서비스 이용을 제한할 수 있습니다.<br /><br />
                2. 일정 기간 동안 서비스에 접속하지 않은 경우, 데이터(영상, 썰 등)가 삭제될 수 있으며, 이에 대한 책임은 이용자에게 있습니다.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold mb-4">제6조 (개인정보 보호 및 관리)</h2>
              <p>
                1. 운영팀은 이용자의 개인정보 보호를 위해 관련 법령을 준수하며, 개인정보 보호 및 사용에 대한 사항은 개인정보처리방침에 따릅니다.<br /><br />
                2. 이용자는 서비스 이용 시 타인의 개인정보를 침해해서는 안 되며, 자신의 계정 정보를 안전하게 관리해야 합니다.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold mb-4">제7조 (운영팀의 역할)</h2>
              <p>
                1. 운영팀은 서비스가 원활하게 제공될 수 있도록 관리 및 유지보수를 진행합니다.<br /><br />
                2. 운영팀은 이용자의 개인정보를 보호하기 위한 조치를 마련하고, 개인정보 처리방침을 준수합니다.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold mb-4">제8조 (서비스 제공의 중단 및 면책 조항)</h2>
              <p>
                1. 운영팀은 다음과 같은 경우 서비스 제공을 일시적으로 중단할 수 있습니다:<br />
                - 시스템 점검, 유지보수, 업그레이드 등의 경우<br />
                - 예상치 못한 서버 장애, 네트워크 문제 등의 기술적 이유<br />
                - 천재지변, 전기통신 서비스 중단 등 불가항력적 사유 발생 시<br /><br />
                2. 서비스 이용 중 발생한 콘텐츠 삭제, 유튜브 업로드 실패 등의 문제에 대해 운영팀은 고의 또는 중대한 과실이 없는 한 책임을 지지 않습니다.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold mb-4">제9조 (콘텐츠의 저작권 및 이용권한)</h2>
              <p>
                1. 이용자가 서비스 내에서 생성한 모든 콘텐츠(영상, 썰, 음성 등)의 저작권은 이용자에게 귀속됩니다.<br /><br />
                2. 이용자는 서비스 내에서 콘텐츠를 자유롭게 저장하고 공유할 수 있으며, 이를 유튜브 및 기타 플랫폼에 업로드할 권리를 가집니다.<br /><br />
                3. 운영팀은 서비스 운영 및 홍보를 위해 이용자가 공개 설정한 콘텐츠를 일부 활용할 수 있습니다. (단, 사전 동의를 받은 경우에 한함)
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold mb-4">제10조 (준거법 및 관할법원)</h2>
              <p>
                1. 본 약관은 대한민국 법률에 따라 규율되며, 이에 따라 해석됩니다.<br /><br />
                2. 본 서비스는 SSAFY 학습 과정의 일환으로 운영되는 것으로, 법적 분쟁 발생 시 공식적인 법인과 관련된 책임은 지지 않습니다.
              </p>
            </section>

            <section>
              <h2 className="text-xl font-semibold mb-4">부칙</h2>
              <p>
                이 약관은 2025년 1월 1일부터 시행됩니다.
              </p>
            </section>
          </div>
        </div>
      </main>
    </div>
  )
} 