import { Character } from "./character";
import { NarratorRef } from "@/components/create/NarratorSettings";

export type VoiceFileKey = "male1" | "male2" | "male3" | "male4" | "female1" | "female2" | "female3" | "female4";

export type VoiceModelName = "Zonos" | "ElevenLabs";

export type ModelType = {
  name: string;
  logo: string;
  isSelected: boolean;
};

export type VoiceCode = Record<string, Record<"male" | "female", string[]>>;

export interface VoiceButtonsProps {
  character: Character
  updateCharacter: (id: string, field: keyof Character, value: any) => void
  currentlyPlaying: { voiceOption: string | null, characterId: string | null }
  setCurrentlyPlaying: (value: { voiceOption: string | null, characterId: string | null }) => void
  voiceModel: string 
  narratorRef?: React.RefObject<NarratorRef> | null
} 