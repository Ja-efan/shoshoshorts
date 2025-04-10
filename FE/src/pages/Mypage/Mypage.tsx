import { useEffect, useState } from "react";
import { apiService } from "@/api/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import VoiceLibrary from "./components/VoiceLibrary";
import { IUserData } from "@/types/user";
import { Navbar } from "@/components/common/Navbar";
import { Button } from "../../components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "../../components/ui/dialog";
import { Input } from "../../components/ui/input";

function Mypage() {
  const [userProfile, setUserProfile] = useState<IUserData | null>(null);
  const [loading, setLoading] = useState(false);
  const [nameModalOpen, setNameModalOpen] = useState(false);
  const [newName, setNewName] = useState("");
  const [isUpdating, setIsUpdating] = useState(false);

  const fetchUserProfile = async () => {
    try {
      const response = await apiService.getUserData();
      setUserProfile(response.data);
    } catch (error) {
      console.error("사용자 정보를 가져오는 중 오류가 발생했습니다:", error);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    fetchUserProfile();
  }, []);

  const handleUpdateName = async () => {
    if (!newName.trim()) return;

    setIsUpdating(true);
    try {
      await apiService.updateUserData({ name: newName });
      fetchUserProfile();
      setNameModalOpen(false);
    } catch (error) {
      console.error("이름 업데이트 실패:", error);
    } finally {
      setIsUpdating(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        로딩 중...
      </div>
    );
  }

  return (
    <>
      <Navbar />
      <div className="container max-w-6xl mx-auto py-10 px-4">
        <h1 className="text-5xl font-bold mb-6">내 정보</h1>
        <p className="text-xl mb-10">
          유튜브 계정 정보, 토큰 사용량 및 목소리 저장소를 관리할 수 있습니다
        </p>

        <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
          {/* 기본 정보 카드 */}
          <Card className="md:col-span-5">
            <CardHeader>
              <CardTitle className="text-2xl">계정 정보</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center mb-4">
                {userProfile?.imgUrl ? (
                  <img
                    src={userProfile.imgUrl}
                    alt="프로필 이미지"
                    className="w-20 h-20 rounded-full bg-red-100/50"
                  />
                ) : (
                  <div className="w-20 h-20 rounded-full bg-red-100/50 flex items-center justify-center text-red-600 text-2xl">
                    {userProfile?.name?.[0] || "?"}
                  </div>
                )}
                <div className="ml-6">
                  <div className="space-y-1 mb-2 flex justify-between items-center">
                    <div>
                      <p className="font-semibold">
                        이름{" "}
                        <button
                          className="font-normal text-xs border border-gray-300 rounded-md px-1 py-0.5 hover:bg-gray-100 cursor-pointer"
                          onClick={() => {
                            setNewName(userProfile?.name || "");
                            setNameModalOpen(true);
                          }}
                        >
                          수정
                        </button>
                      </p>
                      <p className="text-muted-foreground">
                        {userProfile?.name || "이름 정보 없음"}
                      </p>
                    </div>
                  </div>
                  <div className="space-y-1">
                    <p className="font-semibold">이메일</p>
                    <p className="text-muted-foreground">
                      {userProfile?.email || "이메일 정보 없음"}
                    </p>
                  </div>
                </div>
              </div>
              <div className="flex justify-center">
                <button
                  onClick={() => (window.location.href = "/dashboard")}
                  className="mt-4 px-4 py-2 bg-red-400 text-white rounded-md shadow-md hover:bg-red-600 transition duration-300 font-medium cursor-pointer"
                >
                  내 동영상 목록 확인하기
                </button>
              </div>
              {/* 대시 보드로 이동하는 버튼*/}
            </CardContent>
          </Card>

          {/* 사용량 카드 */}
          <Card className="md:col-span-7">
            <CardHeader>
              <CardTitle className="text-2xl">토큰 사용량</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-6">
                <div>
                  <div className="flex justify-between items-center mb-2">
                    <span className="font-semibold">
                      [유료] KlingAI 이미지 생성, Elevenlabs 음성 생성
                    </span>
                    <span>미구현 / 500</span>
                  </div>
                  <div className="h-2 w-full bg-gray-200 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-green-500 rounded-full"
                      style={{ width: "100%" }}
                    ></div>
                  </div>
                  <p className="mt-2 text-sm text-gray-600">
                    API를 사용하여 빠른 영상 생성 속도와 퀄리티를 보장합니다.
                  </p>
                  <p className="mt-1 text-sm text-gray-600">
                    최근 한 달간 결제한 m개의 토큰 중, n개를 사용했습니다.
                  </p>
                </div>

                <div>
                  <div className="flex justify-between items-center mb-2">
                    <span className="font-semibold">
                      [무료] Stable Diffusion 모델, Zonos TTS 모델
                    </span>
                    <span>무제한</span>
                  </div>
                  <div className="h-2 w-full bg-gray-200 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-green-500 rounded-full"
                      style={{ width: "0%" }}
                    ></div>
                  </div>
                  <p className="mt-2 text-sm text-gray-600">
                    로컬 모델이기 때문에 무료이지만, 영상 생성 속도가 느립니다.
                  </p>
                </div>
              </div>
            </CardContent>
            {/* <CardFooter className="flex justify-between border-t px-6 py-2">
            <div className="flex items-center">
              <span className="mr-2">⚠️</span>
              <p className="text-sm">무료 생성 토큰은 n일 뒤 초기화됩니다.</p>
            </div>
            <Button variant="link">자세히 알아보기</Button>
          </CardFooter> */}
          </Card>

          <VoiceLibrary speakerLibrary={userProfile?.speakerLibrary} />
        </div>
      </div>

      {/* 이름 수정 모달 */}
      <Dialog open={nameModalOpen} onOpenChange={setNameModalOpen}>
        <DialogContent className="w-[400px]">
          <DialogHeader>
            <DialogTitle>이름 수정</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4">
            <div className="grid grid-cols-3 items-center gap-4">
              <Input
                id="name"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                className="col-span-3"
              />
            </div>
          </div>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setNameModalOpen(false)}
            >
              취소
            </Button>
            <Button
              type="button"
              onClick={handleUpdateName}
              disabled={isUpdating || !newName.trim()}
            >
              {isUpdating ? "저장 중..." : "저장"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}

export default Mypage;
