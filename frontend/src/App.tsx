import { useEffect, useRef, useState } from "react";
import { usePrefetchQueue } from "./usePrefetchQueue";

export default function App() {
  const [language, setLanguage] = useState("fr");
  const [revealed, setRevealed] = useState(false);
  const { card, next, error } = usePrefetchQueue(language);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  useEffect(() => {
    setRevealed(false);
  }, [card]);

  const handleNext = () => {
    next();
  };

  const handlePlay = () => {
    audioRef.current?.play();
  };

  return (
    <div style={{ maxWidth: 480, margin: "4rem auto", fontFamily: "sans-serif", textAlign: "center" }}>
      <div style={{ marginBottom: "1rem" }}>
        <label>
          Language:{" "}
          <input
            value={language}
            onChange={(e) => setLanguage(e.target.value.trim().toLowerCase())}
            style={{ width: "4rem" }}
          />
        </label>
      </div>

      {error && <p style={{ color: "crimson" }}>{error}</p>}

      {!card && !error && <p>Loading next card…</p>}

      {card && (
        <div style={{ border: "1px solid #ccc", borderRadius: 8, padding: "2rem" }}>
          <h1>{card.word}</h1>

          <button onClick={() => setRevealed((r) => !r)} style={{ marginBottom: "1rem" }}>
            {revealed ? card.translationEn ?? "(no translation yet)" : "Reveal translation"}
          </button>

          <p style={{ fontStyle: "italic" }}>{card.sentenceText}</p>

          <audio ref={audioRef} src={`${import.meta.env.BASE_URL}${card.audioUrl}`} autoPlay />
          <div style={{ marginTop: "1rem" }}>
            <button onClick={handlePlay}>Play again</button>
            <button onClick={handleNext} style={{ marginLeft: "1rem" }}>
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
