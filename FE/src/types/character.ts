export type Character = {
  id: string;
  name: string;
  gender: "male" | "female" | null;
  voice: string | null;
  description: string | null;
};

export type CurrentlyPlaying = {
  voiceOption: string | null;
  characterId: string | null;
};
