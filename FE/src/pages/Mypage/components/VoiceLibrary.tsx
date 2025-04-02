import React, { useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const VoiceLibrary: React.FC = () => {
  return (
    <>
      <Card className="md:col-span-12">
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle className="text-2xl">보이스 라이브러리</CardTitle>
          </div>
        </CardHeader>
        <CardContent className="space-y-4"></CardContent>
      </Card>
    </>
  );
};

export default VoiceLibrary;
