CREATE SEQUENCE card_seq;

CREATE TABLE words (
  id BIGSERIAL PRIMARY KEY,
  language VARCHAR(8) NOT NULL,
  text VARCHAR(255) NOT NULL,
  translation_en VARCHAR(255),
  last_shown_seq BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE sample_sentences (
  id BIGSERIAL PRIMARY KEY,
  word_id BIGINT NOT NULL REFERENCES words(id) ON DELETE CASCADE,
  text TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audio_files (
  id BIGSERIAL PRIMARY KEY,
  sentence_id BIGINT NOT NULL REFERENCES sample_sentences(id) ON DELETE CASCADE,
  file_path VARCHAR(512) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_words_language ON words(language);
CREATE INDEX idx_sample_sentences_word ON sample_sentences(word_id);
CREATE INDEX idx_audio_files_sentence ON audio_files(sentence_id);
