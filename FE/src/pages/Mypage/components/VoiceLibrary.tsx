import React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useEffect, useState } from "react";
import { apiService } from "@/api/api";
import { useForm, FieldErrors } from "react-hook-form";
import { toast } from "react-hot-toast";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { ISpeakerInfoGet } from "@/types/speakerInfo";

interface IForm {
  title: string;
  description: string;
  voiceSample?: FileList;
}

// 2. File을 base64로 변환하는 헬퍼 함수
async function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = reject;
  });
}

interface VoiceLibraryProps {
  speakerLibrary?: ISpeakerInfoGet[];
}

const VoiceLibrary: React.FC<VoiceLibraryProps> = ({ speakerLibrary }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [speakerArr, setSpeakerArr] = useState<ISpeakerInfoGet[]>();
  const [deleteSpeakerId, setDeleteSpeakerId] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: {},
  } = useForm<IForm>();

  useEffect(() => {
    setSpeakerArr(speakerLibrary);
    debugger;
  }, [speakerLibrary]);

  // 목소리 라이브러리 다시 불러오기 함수
  const refreshSpeakerLibrary = async () => {
    try {
      const response = await apiService.getUserData();
      setSpeakerArr(response.data.speakerLibrary);
    } catch (error) {
      console.error(
        "사용자 정보를 다시 불러오는 중 오류가 발생했습니다:",
        error
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const onValidSubmit = async (data: IForm) => {
    // 이미 제출 중이면 중복 제출 방지
    if (isSubmitting) return;

    setIsSubmitting(true);
    try {
      let base64String = "";

      if (data.voiceSample && data.voiceSample.length > 0) {
        // mp3 파일을 base64로 변환
        const file = data.voiceSample[0];
        base64String = await fileToBase64(file);
      }

      const payload = {
        title: data.title,
        description: data.description,
        audioBase64: base64String, // 서버에서 이 키로 받을지 협의 필요
      };

      await apiService.uploadSpeaker(payload);

      toast.success("목소리가 성공적으로 업로드되었습니다!");
      setIsOpen(false); // 성공 시 모달 닫기

      // 목소리 라이브러리 다시 불러오기
      await refreshSpeakerLibrary();
    } catch (error) {
      console.error("목소리를 등록하는 중 오류가 발생했습니다:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const onInvalidSubmit = (errors: FieldErrors<IForm>) => {
    if (errors.title?.type === "required") {
      // 오류에 맞는 오류 안내창 출력
      toast.error(errors.title.message || "오류가 발생했습니다.");
    } else if (errors.title?.type === "maxLength") {
      // 오류에 맞는 오류 안내창 출력
      toast.error(errors.title.message || "오류가 발생했습니다.");
    } else if (errors.description?.type === "required") {
      // 오류에 맞는 오류 안내창 출력
      toast.error(errors.description.message || "오류가 발생했습니다.");
    } else if (errors.description?.type === "maxLength") {
      // 오류에 맞는 오류 안내창 출력
      toast.error(errors.description.message || "오류가 발생했습니다.");
    } else if (errors.voiceSample?.message) {
      // 파일에 대한 커스텀 validate 에러 메시지 처리
      toast.error(errors.voiceSample.message.toString());
    }
  };

  const handleDelete = async (speakerId: string) => {
    try {
      await apiService.deleteSpeaker(speakerId);
      toast.success("목소리가 성공적으로 삭제되었습니다!");
      await refreshSpeakerLibrary();
    } catch (error) {
      console.error("목소리를 삭제하는 중 오류가 발생했습니다:", error);
    }
  };

  return (
    <>
      <Card className="md:col-span-12">
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle className="text-2xl">캐릭터 목소리 저장소</CardTitle>
            <Dialog open={isOpen} onOpenChange={setIsOpen}>
              <DialogTrigger asChild>
                <Button variant="outline">목소리 등록하기</Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-[425px]">
                <DialogHeader>
                  <DialogTitle>목소리 업로드</DialogTitle>
                </DialogHeader>
                <form
                  className="space-y-4 mt-4"
                  onSubmit={handleSubmit(onValidSubmit, onInvalidSubmit)}
                >
                  <div className="space-y-2">
                    <label htmlFor="title" className="text-sm font-medium">
                      제목
                    </label>
                    <input
                      id="title"
                      {...register("title", {
                        required: "등록할 목소리의 제목을 적어주세요.",
                        maxLength: {
                          value: 50,
                          message: "최대 50자리까지 입력할 수 있습니다",
                        },
                      })}
                      className="bg-transparent text-[#848484] focus:text-black w-full p-2 border rounded-md"
                      placeholder="목소리 제목"
                      type="text"
                      autoComplete="off"
                    />
                  </div>

                  <div className="space-y-2">
                    <label
                      htmlFor="description"
                      className="text-sm font-medium"
                    >
                      설명
                    </label>
                    <input
                      id="description"
                      {...register("description", {
                        required: "등록할 목소리에 대한 설명을 적어주세요.",
                        maxLength: {
                          value: 200,
                          message: "최대 200자리까지 입력할 수 있습니다",
                        },
                      })}
                      className="bg-transparent text-[#848484] focus:text-black w-full p-2 border rounded-md"
                      placeholder="목소리 설명"
                      type="text"
                      autoComplete="off"
                    />
                  </div>

                  <div className="space-y-2">
                    <label
                      htmlFor="voiceSample"
                      className="text-sm font-medium"
                    >
                      MP3 파일
                    </label>
                    <input
                      id="voiceSample"
                      type="file"
                      accept=".mp3,audio/*"
                      className="w-full"
                      {...register("voiceSample", {
                        required: "업로드할 mp3 파일을 선택해 주세요.",
                        validate: {
                          lessThan1MB: (files) => {
                            // 1MB(= 1 * 1024 * 1024) 초과 시 에러
                            if (files && files.length > 0) {
                              return (
                                files[0].size <= 2 * 1024 * 1024 ||
                                "파일 용량은 2MB (약 1분) 이하여야 합니다."
                              );
                            }
                            return true;
                          },
                        },
                      })}
                    />
                  </div>

                  <div className="flex justify-end gap-2 mt-4">
                    <Button
                      variant="outline"
                      type="button"
                      onClick={() => setIsOpen(false)}
                      disabled={isSubmitting}
                    >
                      취소
                    </Button>
                    <Button type="submit" disabled={isSubmitting}>
                      {isSubmitting ? (
                        <div className="flex items-center">
                          <svg
                            className="animate-spin -ml-1 mr-2 h-4 w-4 text-white"
                            xmlns="http://www.w3.org/2000/svg"
                            fill="none"
                            viewBox="0 0 24 24"
                          >
                            <circle
                              className="opacity-25"
                              cx="12"
                              cy="12"
                              r="10"
                              stroke="currentColor"
                              strokeWidth="4"
                            ></circle>
                            <path
                              className="opacity-75"
                              fill="currentColor"
                              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                            ></path>
                          </svg>
                          등록 중...
                        </div>
                      ) : (
                        "등록하기"
                      )}
                    </Button>
                  </div>
                </form>
              </DialogContent>
            </Dialog>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {speakerArr && speakerArr.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {speakerArr.map((speaker) => (
                <div
                  key={speaker.id}
                  className="border rounded-lg p-3 shadow-sm relative"
                >
                  <h3 className="font-medium text-lg mb-2">{speaker.title}</h3>
                  <p className="text-sm text-gray-600 mb-3">
                    {speaker.description}
                  </p>
                  <div className="flex items-center justify-between">
                    <audio controls className="w-[calc(100% - 0.5rem)]">
                      <source src={speaker.voiceSampleUrl} type="audio/mpeg" />
                      브라우저가 오디오 재생을 지원하지 않습니다.
                    </audio>
                    <div className="text-xs text-gray-500 ml-1 whitespace-nowrap w-fit">
                      {new Date(speaker.createdAt).toLocaleDateString()}
                    </div>
                  </div>
                  <button
                    className="absolute top-2 right-2 text-red-500 text-sm p-1 cursor-pointer"
                    onClick={() => setDeleteSpeakerId(String(speaker.id))}
                  >
                    삭제
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500">
              등록된 목소리가 없습니다. 목소리를 등록해보세요!
            </div>
          )}
        </CardContent>
      </Card>

      {/* 삭제 확인 다이얼로그 */}
      {deleteSpeakerId && (
        <Dialog open={true} onOpenChange={() => setDeleteSpeakerId(null)}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>삭제 확인</DialogTitle>
              <DialogDescription>
                정말로 이 목소리를 삭제하시겠습니까?
              </DialogDescription>
            </DialogHeader>
            <div className="flex justify-end gap-2 mt-2">
              <Button
                variant="outline"
                onClick={() => setDeleteSpeakerId(null)}
              >
                취소
              </Button>
              <Button
                onClick={() => {
                  handleDelete(deleteSpeakerId);
                  setDeleteSpeakerId(null);
                }}
              >
                삭제
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      )}
    </>
  );
};

export default VoiceLibrary;
