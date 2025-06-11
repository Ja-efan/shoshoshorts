import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatDate(dateString: string | null) {
  if (!dateString) return ""
  
  // GMT 시간을 로컬 시간으로 변환
  const date = new Date(dateString)
  const timezoneOffset = date.getTimezoneOffset() * 60000 // 분을 밀리초로 변환
  const localDate = new Date(date.getTime() - timezoneOffset)
  
  return localDate.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}