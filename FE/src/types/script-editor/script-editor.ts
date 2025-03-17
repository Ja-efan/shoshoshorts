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

export type Character = {
  name: string;
  gender: "male" | "female";
  properties: string;
}

export type AudioSettings = {
  voiceType: string;
  emotion: EmotionSettings;
  speakingRate: number;
  seed: number;
  languageCode: string;
}

export type ScriptBlock = {
  type: "narration" | "dialogue";
  text: string;
  character: string | null;
  emotion: string | null;
  audio: AudioSettings | null;
}

export type ScriptData = {
  id: string;
  title: string;
  characters: Character[];
  blocks: ScriptBlock[];
}