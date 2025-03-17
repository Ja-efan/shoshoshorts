import ScriptEditor from "@/components/script-editor/scriptEditor"

export default function ScriptEditorPage() {
  return (
    <div className="container mx-auto py-10">
      <h1 className="text-2xl font-bold mb-6">Interactive Script Editor</h1>
      <ScriptEditor />
    </div>
  )
}