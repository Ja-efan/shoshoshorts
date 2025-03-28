export interface ApiResponse<T> {
  data: T;
  message: string;
  status: number;
}

export type SocialProvider = "google" | "naver" | "kakao"; 
