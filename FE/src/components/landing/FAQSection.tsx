import { faqs } from "@/lib/constants/faqs";

export function FAQSection() {
  return (
    <section id="faq" className="py-20">
      <div className="container px-4">
        <div className="mx-auto max-w-3xl text-center">
          <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">자주 묻는 질문</h2>
          <p className="mt-4 text-lg text-gray-600">쇼쇼숓에 대한 일반적인 질문들의 답변을 찾아보세요</p>
        </div>

        <div className="mt-12 grid gap-6 md:grid-cols-2">
          {faqs.map((faq, i) => (
            <div key={i} className="rounded-xl border bg-white p-6 shadow-sm">
              <h3 className="text-lg font-semibold">{faq.question}</h3>
              <p className="mt-2 text-gray-600">{faq.answer}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}