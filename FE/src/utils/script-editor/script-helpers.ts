import { Speaker, ScriptLineType, defaultEmotions, ScriptData, ScriptBlock } from "@/types/script-editor/script-editor";

export function getLineStyles(type: Speaker): string {
  switch (type) {
    case "Narrator":
      return "border-l-4 border-blue-500 bg-blue-50";
    case "Speaker A":
      return "border-l-4 border-green-500 bg-green-50";
    case "Speaker B":
      return "border-l-4 border-purple-500 bg-purple-50";
    case "Situation":
      return "border-l-4 border-amber-500 bg-amber-50";
    default:
      return "bg-gray-50";
  }
}

export function getAvatarColor(type: Speaker): string {
  switch (type) {
    case "Narrator":
      return "bg-blue-500";
    case "Speaker A":
      return "bg-green-500";
    case "Speaker B":
      return "bg-purple-500";
    case "Situation":
      return "bg-amber-500";
    default:
      return "bg-gray-500";
  }
}

export function getAvatarInitial(type: Speaker): string {
  switch (type) {
    case "Narrator":
      return "N";
    case "Speaker A":
      return "A";
    case "Speaker B":
      return "B";
    case "Situation":
      return "S";
    default:
      return (type as string).charAt(0);
  }
}

export function createNewScriptLine(type: Speaker = "Situation", content: string = "새로운 상황"): ScriptLineType {
  return {
    id: `line-${Date.now()}`,
    type,
    content,
    emotions: defaultEmotions,
  };
}

export function exportScript(scriptLines: ScriptLineType[]): void {
  const jsonData = JSON.stringify(scriptLines, null, 2);
  const blob = new Blob([jsonData], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "script-data.json";
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export const dummyScriptData: ScriptData = {
  id: "script-001",
  title: "아반떼 N 전손 썰",
  characters: [
    {
      name: "종훈",
      gender: "male",
      properties: "무언가에 쉽게 열광하고 즉흥적인 성격"
    },
    {
      name: "보험사 직원",
      gender: "male",
      properties: "차분하고 책임감 있는 성격"
    }
  ],
  blocks: [
    {
      type: "narration",
      text: "N 모드를 켠 뒤, 도산대로를 달리기 시작했다.",
      character: null,
      emotion: null,
      audio: null
    },
    {
      type: "dialogue",
      text: "N은 렌트해서 타는 게 아니었다...",
      character: "종훈",
      emotion: "후회",
      audio: {
        voiceType: "male_youth",
        emotion: {
          happiness: 0.1,
          sadness: 0.6,
          disgust: 0,
          fear: 0,
          surprise: 0,
          anger: 0,
          neutral: 0.3,
          speakingRate: 1.0
        },
        speakingRate: 1.0,
        seed: 12345,
        languageCode: "ko"
      }
    }
  ]
};

export function convertScriptLinesToScriptData(scriptLines: ScriptLineType[]): ScriptData {
  return {
    id: "script-" + Date.now(),
    title: "새로운 스크립트",
    characters: scriptLines
      .filter(line => line.type !== "Narrator" && line.type !== "Situation")
      .map(line => ({
        name: line.type,
        gender: "male", // 기본값
        properties: "기본 캐릭터"
      })),
    blocks: scriptLines.map(line => ({
      type: line.type === "Narrator" || line.type === "Situation" ? "narration" : "dialogue",
      text: line.content,
      character: line.type === "Narrator" || line.type === "Situation" ? null : line.type,
      emotion: null,
      audio: {
        voiceType: "male_youth",
        emotion: {
          ...line.emotions,
          speakingRate: line.emotions.speakingRate
        },
        speakingRate: line.emotions.speakingRate,
        seed: Math.floor(Math.random() * 10000),
        languageCode: "ko"
      }
    }))
  };
}

export function convertScriptDataToScriptLines(scriptData: ScriptData): ScriptLineType[] {
  return scriptData.blocks.map((block: ScriptBlock) => ({
    id: `line-${Date.now()}-${Math.random()}`,
    type: block.type === "narration" 
      ? (block.text.includes("상황") ? "Situation" : "Narrator")
      : (block.character as Speaker || "Speaker A"),
    content: block.text,
    emotions: block.audio?.emotion || defaultEmotions
  }));
}