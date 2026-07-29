export interface Card {
  word: string;
  translationEn: string | null;
  sentenceText: string;
  sentenceTranslatedEn: string | null;
  audioUrl: string;
}

export async function fetchNextCard(language: string): Promise<Card> {
  const response = await fetch(
    `${import.meta.env.BASE_URL}api/cards/next?language=${encodeURIComponent(language)}`,
  );
  if (!response.ok) {
    throw new Error(`Failed to fetch next card: ${response.status}`);
  }
  return response.json();
}
