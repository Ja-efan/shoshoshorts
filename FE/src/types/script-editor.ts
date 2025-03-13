export type EmotionSettings = {
    happiness: number
    sadness: number
    disgust: number
    fear: number
    surprise: number
    anger: number
    neutral: number
    speakingRate: number
  }
  
  export type Speaker = "Narrator" | "Speaker A" | "Speaker B" | "Situation"
  
  export type ScriptLineType = {
    id: string
    type: Speaker
    content: string
    emotions: EmotionSettings
  }
  
  export const defaultEmotions: EmotionSettings = {
    happiness: 0,
    sadness: 0,
    disgust: 0,
    fear: 0,
    surprise: 0,
    anger: 0,
    neutral: 1,
    speakingRate: 5,
  }