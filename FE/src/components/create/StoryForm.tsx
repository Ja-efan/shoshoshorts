import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"

interface StoryFormProps {
  title: string
  story: string
  onTitleChange: (value: string) => void
  onStoryChange: (value: string) => void
}

export function StoryForm({ title, story, onTitleChange, onStoryChange }: StoryFormProps) {
  return (
    <>
      <div>
        <h2 className="text-xl font-semibold">제목</h2>
        <Input
          value={title}
          onChange={(e) => onTitleChange(e.target.value)}
          placeholder="동영상 제목을 입력하세요..."
          className="mt-2"
        />
      </div>

      <div>
        <h2 className="text-xl font-semibold">스토리</h2>
        <Textarea
          value={story}
          onChange={(e) => onStoryChange(e.target.value)}
          placeholder="스토리를 입력하세요..."
          className="mt-2 min-h-[200px]"
        />
      </div>
    </>
  )
} 